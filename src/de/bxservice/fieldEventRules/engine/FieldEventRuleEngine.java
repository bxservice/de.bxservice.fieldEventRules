/**********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Diego Ruiz - BX Service GmbH                                      *
 **********************************************************************/
package de.bxservice.fieldEventRules.engine;

import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MMessage;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Evaluatee;
import org.compiere.util.Msg;

import de.bxservice.fieldEventRules.engine.ConditionClauseValidator.Format;
import de.bxservice.fieldEventRules.model.MBXSFieldEventAction;
import de.bxservice.fieldEventRules.model.MBXSFieldEventRule;
import de.bxservice.fieldEventRules.model.X_BXS_FieldEventAction;
import de.bxservice.fieldEventRules.model.X_BXS_FieldEventRule;

/**
 * Stateless engine that evaluates {@link MBXSFieldEventRule} records and
 * produces a {@link FieldEventResult}. Safe to instantiate per-call or as a
 * singleton.
 *
 * <p>
 * Two entry points cover the two caller contexts:
 * <ul>
 * <li>{@link #evaluateUITrigger} — called from a ZK callout (AD_Field_ID scope)
 * <li>{@link #evaluateSaveTrigger} — called from a ModelValidator (AD_Column_ID
 * scope)
 * </ul>
 */
public class FieldEventRuleEngine {

	private static final CLogger log = CLogger.getCLogger(FieldEventRuleEngine.class);

	private static final String SQL_PREFIX = "@SQL=";

	private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

	public FieldEventResult evaluateUITrigger(int adFieldId, EvaluationContext ctx) {
		return evaluate(X_BXS_FieldEventRule.COLUMNNAME_AD_Field_ID, adFieldId, X_BXS_FieldEventRule.TRIGGEREVENT_UI,
				ctx);
	}

	public FieldEventResult evaluateSaveTrigger(int adColumnId, EvaluationContext ctx) {
		return evaluate(X_BXS_FieldEventRule.COLUMNNAME_AD_Column_ID, adColumnId,
				X_BXS_FieldEventRule.TRIGGEREVENT_OnSaveModel, ctx);
	}

	private FieldEventResult evaluate(String idColumn, int idValue, String triggerPath, EvaluationContext ctx) {

		List<MBXSFieldEventRule> rules = X_BXS_FieldEventRule.COLUMNNAME_AD_Field_ID.equals(idColumn)
				? FieldEventRuleCache.get().getRulesByFieldId(idValue)
				: FieldEventRuleCache.get().getRulesByColumnId(idValue);

		FieldEventResult.Builder result = new FieldEventResult.Builder();

		for (MBXSFieldEventRule rule : rules) {

			if (!matchesTrigger(rule.getTriggerEvent(), triggerPath))
				continue;

			if (!conditionPasses(rule, ctx))
				continue;

			if (X_BXS_FieldEventRule.BXS_RULETYPE_VALIDATE.equals(rule.getBXS_RuleType())) {
				applyValidation(rule, result);
			} else {
				List<MBXSFieldEventAction> actions = FieldEventRuleCache.get().getActionsByRuleId(rule.get_ID());
				for (MBXSFieldEventAction action : actions)
					applyAction(action, rule, ctx, result);
			}
		}

		return result.build();
	}

	private static boolean matchesTrigger(String ruleEvent, String triggerPath) {
		return triggerPath.equals(ruleEvent) || X_BXS_FieldEventRule.TRIGGEREVENT_Both.equals(ruleEvent);
	}

	private boolean conditionPasses(MBXSFieldEventRule rule, EvaluationContext ctx) {
		String condition = rule.getBXS_Condition();
		if (condition == null || condition.isBlank())
			return true;

		try {
			java.util.Optional<Format> format = ConditionClauseValidator.validate(condition);

			if (format.isEmpty())
				return true;

			if (format.get() == Format.CONTEXT_EXPRESSION)
				return evaluateContextCondition(condition, ctx);

			// SQL_WHERE
			return evaluateSQLCondition(condition, ctx);

		} catch (Exception e) {
			log.warning("Rule '" + rule.getName() + "': condition evaluation failed — " + e.getMessage());
			return false;
		}
	}

	private static boolean evaluateContextCondition(String condition, EvaluationContext ctx) {
		return org.idempiere.expression.logic.LogicEvaluator.evaluateLogic(evaluateeFor(ctx), condition);
	}

	private boolean evaluateSQLCondition(String condition, EvaluationContext ctx) throws AdempiereException {

		String fragment = condition.substring(SQL_PREFIX.length());

		PO po = ctx.getPo();
		if (po != null) {
			String parsedFragment = Env.parseVariable(fragment, po, null, false);
			String sql = "SELECT 1 FROM dual "
					+ " WHERE (" + parsedFragment + ")";
			return DB.getSQLValueEx(po.get_TrxName(), sql) > 0;
		}

		// UI callout path: no persisted record, fall back to inline token substitution.
		// Only @Token@ references work here; bare column names are not supported.
		String wrapped = SQL_PREFIX + "SELECT CASE WHEN (" + fragment + ") THEN 'Y' ELSE 'N' END"
				+ " FROM (SELECT 1) AS T";
		Object result = evaluator.evaluate(wrapped, ctx);
		return "Y".equals(String.valueOf(result));
	}

	private void applyAction(MBXSFieldEventAction action, MBXSFieldEventRule rule, EvaluationContext ctx,
			FieldEventResult.Builder result) {

		String colName = MColumn.getColumnName(ctx.getCtx(), action.getAD_Column_ID());

		try {
			String actionType = action.getBXS_ActionType();

			if (X_BXS_FieldEventAction.BXS_ACTIONTYPE_CLEAR.equals(actionType)) {
				result.addAssignment(colName, null);

			} else if (X_BXS_FieldEventAction.BXS_ACTIONTYPE_SET.equals(actionType)) {
				Object value = evaluator.evaluate(action.getBXS_ValueExpression(), ctx);
				result.addAssignment(colName, value);

			} else if (X_BXS_FieldEventAction.BXS_ACTIONTYPE_SETIFBLANK.equals(actionType)) {
				Object current = ctx.getCurrentValues().get(colName);
				if (current == null || "".equals(current)) {
					Object value = evaluator.evaluate(action.getBXS_ValueExpression(), ctx);
					result.addAssignment(colName, value);
				}
			}

		} catch (Exception e) {
			log.warning("Rule '" + rule.getName() + "' action " + action.getSeqNo() + " on column " + colName + ": "
					+ e.getMessage());
			result.addMessage(
					"Error in rule '" + rule.getName() + "' action " + action.getSeqNo() + ": " + e.getMessage(), "W",
					colName);
		}
	}

	private static void applyValidation(MBXSFieldEventRule rule, FieldEventResult.Builder result) {
		String level = rule.getBXS_ErrorLevel();
		if (level == null || level.isBlank())
			level = X_BXS_FieldEventRule.BXS_ERRORLEVEL_ErrorBlockSave;

		String msg = null;
		MMessage message = MMessage.get(Env.getCtx(), rule.getAD_Message_ID());
		if (message != null) {
			String msgValue = message.getValue();
			msg = Msg.getMsg(Env.getCtx(), msgValue);
		}
		if (msg == null || msg.isBlank())
			msg = rule.getName();

		result.addMessage(msg, level, null);
	}

	private static Evaluatee evaluateeFor(EvaluationContext ctx) {
		if (ctx.getPo() != null)
			return ctx.getPo();
		return variableName -> {
			Object v = ctx.getCurrentValues().get(variableName);
			return v != null ? v.toString() : "";
		};
	}
}
