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
import java.sql.Savepoint;
import java.util.LinkedHashMap;
import java.util.Map;
import org.compiere.model.MTable;
import org.compiere.util.Trx;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MMessage;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Evaluatee;
import org.compiere.util.Msg;
import org.idempiere.expression.logic.LogicEvaluator;

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

	public FieldEventResult evaluateUITrigger(int adColumnId, EvaluationContext ctx) {
		return evaluate(adColumnId, X_BXS_FieldEventRule.TRIGGEREVENT_UI, ctx);
	}

	public FieldEventResult evaluateSaveTrigger(int adColumnId, EvaluationContext ctx) {
		return evaluate(adColumnId, X_BXS_FieldEventRule.TRIGGEREVENT_OnSaveModel, ctx);
	}

	private FieldEventResult evaluate(int adColumnId, String triggerPath, EvaluationContext ctx) {

		List<MBXSFieldEventRule> rules = FieldEventRuleCache.get().getRulesByColumnId(adColumnId);

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
				for (MBXSFieldEventAction action : actions) {
					if (action.getAD_Target_Table_ID() == 0)
						applyAction(action, rule, ctx, result);
				}
			}
		}

		return result.build();
	}

	/**
	 * Dedicated path for PO_AFTER_NEW: collects and applies cross-table actions
	 * across all registered columns without re-running validations or
	 * source-record assignments (which already ran on PO_BEFORE_NEW).
	 */
	public FieldEventResult evaluateAfterNewTrigger(List<Integer> columnIds, EvaluationContext ctx) {
		FieldEventResult.Builder result = new FieldEventResult.Builder();
		Map<Integer, Map<String, Object>> crossTable = new LinkedHashMap<>();

		for (int adColumnId : columnIds) {
			List<MBXSFieldEventRule> rules = FieldEventRuleCache.get().getRulesByColumnId(adColumnId);
			for (MBXSFieldEventRule rule : rules) {
				if (!matchesTrigger(rule.getTriggerEvent(), X_BXS_FieldEventRule.TRIGGEREVENT_OnSaveModel))
					continue;
				if (X_BXS_FieldEventRule.BXS_RULETYPE_VALIDATE.equals(rule.getBXS_RuleType()))
					continue;
				if (!conditionPasses(rule, ctx))
					continue;
				List<MBXSFieldEventAction> actions = FieldEventRuleCache.get().getActionsByRuleId(rule.get_ID());
				for (MBXSFieldEventAction action : actions) {
					if (action.getAD_Target_Table_ID() > 0)
						collectCrossTableAction(action, rule, ctx, crossTable, result);
				}
			}
		}

		if (ctx.getPo() != null && !crossTable.isEmpty())
			applyCrossTableRecords(crossTable, ctx, result);

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
		return LogicEvaluator.evaluateLogic(evaluateeFor(ctx), condition);
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
				if (!ctx.isAfterNew())
					result.addAssignment(colName, null);

			} else if (X_BXS_FieldEventAction.BXS_ACTIONTYPE_SET.equals(actionType)) {
				if (!ctx.isAfterNew()) {
					Object value = evaluator.evaluate(action.getBXS_ValueExpression(), ctx);
					result.addAssignment(colName, value);
				}
			} else if (X_BXS_FieldEventAction.BXS_ACTIONTYPE_SETIFBLANK.equals(actionType)) {
				if (!ctx.isAfterNew()) {
					Object current = ctx.getCurrentValues().get(colName);
					if (current == null || "".equals(current)) {
						Object value = evaluator.evaluate(action.getBXS_ValueExpression(), ctx);
						result.addAssignment(colName, value);
					}
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
	
	private void collectCrossTableAction(MBXSFieldEventAction action, MBXSFieldEventRule rule, EvaluationContext ctx,
			Map<Integer, Map<String, Object>> crossTable, FieldEventResult.Builder result) {
		if (action.getAD_Column_ID() == 0) {
			result.addMessage("Rule '" + rule.getName() + "': cross-table action has no Target Column, skipped.", "W", null);
			return;
		}
		try {
			String colName = MColumn.getColumnName(ctx.getCtx(), action.getAD_Column_ID());
			if (colName == null) {
				result.addMessage("Rule '" + rule.getName() + "': Target Column ID=" + action.getAD_Column_ID() + " not found, skipped.", "W", null);
				return;
			}
			Object value = evaluator.evaluate(action.getBXS_ValueExpression(), ctx);
			crossTable.computeIfAbsent(action.getAD_Target_Table_ID(), k -> new LinkedHashMap<>()).put(colName, value);
		} catch (Exception e) {
			String msg = "Rule '" + rule.getName() + "' cross-table collect failed: " + e.getMessage();
			log.warning(msg);
			result.addMessage(msg, "W", null);
		}
	}
    
	private void applyCrossTableRecords(Map<Integer, Map<String, Object>> crossTable, EvaluationContext ctx,
			FieldEventResult.Builder result) {
		String srcTrxName = ctx.getPo().get_TrxName();
		Trx trx = srcTrxName != null ? Trx.get(srcTrxName, false) : null;

		for (Map.Entry<Integer, Map<String, Object>> entry : crossTable.entrySet()) {
			int targetTableId = entry.getKey();
			Map<String, Object> colValues = entry.getValue();

			MTable targetTable = MTable.get(ctx.getCtx(), targetTableId);
			if (targetTable == null || targetTable.getAD_Table_ID() == 0) {
				String msg = "Cross-table action: target table ID=" + targetTableId + " not found.";
				log.warning(msg);
				result.addMessage(msg, "W", null);
				continue;
			}

			PO relatedPo = targetTable.getPO(0, srcTrxName);
			for (Map.Entry<String, Object> cv : colValues.entrySet()) {
				if (relatedPo.get_ColumnIndex(cv.getKey()) < 0) {
					log.warning("Cross-table: column '" + cv.getKey() + "' not found in " + targetTable.getTableName());
					continue;
				}
				relatedPo.set_Value(cv.getKey(), cv.getValue());
			}

			Savepoint sp = null;
			try {
				if (trx != null) sp = trx.setSavepoint(null);
				relatedPo.saveEx();
			} catch (Exception e) {
				if (trx != null && sp != null) {
					try { trx.rollback(sp); } catch (Exception rollbackEx) { log.warning("Savepoint rollback failed: " + rollbackEx.getMessage()); }
				}
				String msg = "Cross-table saveEx failed for " + targetTable.getTableName() + ": " + e.getMessage();
				log.warning(msg);
				result.addMessage(msg, "W", null);
			}
		}
	}

	private static Evaluatee evaluateeFor(EvaluationContext ctx) {
		return variableName -> toLogicValue(resolveRawValue(variableName, ctx));
	}

	/**
	 * Resolves the raw (untyped) value of a column from the PO or, on the UI
	 * callout path (no PO available), from the current values map.
	 */
	private static Object resolveRawValue(String variableName, EvaluationContext ctx) {
		PO po = ctx.getPo();
		if (po != null) {
			int idx = po.get_ColumnIndex(variableName);
			return idx >= 0 ? po.get_Value(idx) : null;
		}
		return ctx.getCurrentValues().get(variableName);
	}

	/**
	 * Normalizes a raw column value to the string form expected by
	 * {@link LogicEvaluator} comparisons. Boolean (YesNo) columns are stored as
	 * {@link Boolean} by both {@code PO} and {@code GridField}; without this
	 * conversion {@code value.toString()} yields "true"/"false" instead of
	 * "Y"/"N", silently breaking conditions like {@code @IsPurchased@='Y'}.
	 */
	private static String toLogicValue(Object value) {
		if (value == null)
			return "";
		if (value instanceof Boolean)
			return ((Boolean) value) ? "Y" : "N";
		return value.toString();
	}
}
