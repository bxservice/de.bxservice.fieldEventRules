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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MMessage;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
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
 * <li>{@link #evaluateAfterNewTrigger} — called from a ModelValidator on
 * PO_AFTER_NEW for cross-table actions only
 * </ul>
 */
public class FieldEventRuleEngine {

	private static final CLogger log = CLogger.getCLogger(FieldEventRuleEngine.class);

	private static final String SQL_PREFIX = "@SQL=";

	private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

	public FieldEventResult evaluateUITrigger(int adColumnId, EvaluationContext ctx) {
		return evaluate(adColumnId, X_BXS_FieldEventRule.TRIGGEREVENT_UICallout, ctx);
	}

	public FieldEventResult evaluateSaveTrigger(int adColumnId, EvaluationContext ctx) {
		return evaluate(adColumnId, X_BXS_FieldEventRule.TRIGGEREVENT_OnSaveModel, ctx);
	}

	/**
	 * Save-trigger path for rules that have no watch column (AD_Column_ID is
	 * NULL). Unlike {@link #evaluateSaveTrigger(int, EvaluationContext)}, the
	 * caller does not gate these on a column change — they always run on save.
	 */
	public FieldEventResult evaluateColumnlessSaveTrigger(List<MBXSFieldEventRule> rules, EvaluationContext ctx) {
		return evaluateRules(rules, X_BXS_FieldEventRule.TRIGGEREVENT_OnSaveModel, ctx);
	}

	private FieldEventResult evaluate(int adColumnId, String triggerPath, EvaluationContext ctx) {
		return evaluateRules(FieldEventRuleCache.get().getRulesByColumnId(adColumnId), triggerPath, ctx);
	}

	private FieldEventResult evaluateRules(List<MBXSFieldEventRule> rules, String triggerPath, EvaluationContext ctx) {

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
					if (action.getBXS_Target_Table_ID() == 0)
						applySourceRecordAction(action, rule, ctx, result);
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
	public void evaluateAfterNewTrigger(List<Integer> columnIds, List<MBXSFieldEventRule> columnlessRules,
			EvaluationContext ctx) {
		Map<Integer, Map<String, Object>> crossTable = new LinkedHashMap<>();

		for (int adColumnId : columnIds)
			collectAfterNewCrossTable(FieldEventRuleCache.get().getRulesByColumnId(adColumnId), ctx, crossTable);

		collectAfterNewCrossTable(columnlessRules, ctx, crossTable);

		if (ctx.getPo() != null && !crossTable.isEmpty())
			applyCrossTableRecords(crossTable, ctx);
	}

	private void collectAfterNewCrossTable(List<MBXSFieldEventRule> rules, EvaluationContext ctx,
			Map<Integer, Map<String, Object>> crossTable) {
		for (MBXSFieldEventRule rule : rules) {
			if (!X_BXS_FieldEventRule.TRIGGEREVENT_AfterNew.equals(rule.getTriggerEvent()))
				continue;
			if (X_BXS_FieldEventRule.BXS_RULETYPE_VALIDATE.equals(rule.getBXS_RuleType()))
				continue;
			List<MBXSFieldEventAction> actions = FieldEventRuleCache.get().getActionsByRuleId(rule.get_ID());
			// Check for cross-table actions before conditionPasses — the condition can
			// run SQL and would otherwise execute on every insert for nothing.
			if (actions.stream().noneMatch(a -> a.getBXS_Target_Table_ID() > 0))
				continue;
			if (!conditionPasses(rule, ctx))
				continue;
			for (MBXSFieldEventAction action : actions) {
				if (action.getBXS_Target_Table_ID() > 0)
					collectCrossTableAction(action, rule, ctx, crossTable);
			}
		}
	}

	private static boolean matchesTrigger(String ruleEvent, String triggerPath) {
		return triggerPath.equals(ruleEvent) || X_BXS_FieldEventRule.TRIGGEREVENT_UISave.equals(ruleEvent);
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
			// Do not swallow: a rule whose condition cannot be evaluated is misconfigured, and
			// silently skipping it would let a VALIDATE rule never fire. The caller (model
			// validator or callout) surfaces the message; core rolls the transaction back.
			throw new AdempiereException(
					"Rule '" + rule.getName() + "': condition evaluation failed — " + e.getMessage(), e);
		}
	}

	private static boolean evaluateContextCondition(String condition, EvaluationContext ctx) {
		return LogicEvaluator.evaluateLogic(evaluateeFor(ctx), condition);
	}

	private boolean evaluateSQLCondition(String condition, EvaluationContext ctx) throws AdempiereException {

		String fragment = condition.substring(SQL_PREFIX.length());

		Object result = evaluator.evaluateSqlQuery(
				"SELECT CASE WHEN (" + fragment + ") THEN 'Y' ELSE 'N' END FROM dual", ctx);
		return "Y".equals(String.valueOf(result));
	}

	private void applySourceRecordAction(MBXSFieldEventAction action, MBXSFieldEventRule rule, EvaluationContext ctx,
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
			level = X_BXS_FieldEventRule.BXS_ERRORLEVEL_Error;

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
			Map<Integer, Map<String, Object>> crossTable) {
		if (action.getAD_Column_ID() == 0)
			throw new AdempiereException("Rule '" + rule.getName() + "': cross-table action has no Target Column configured.");

		String colName = MColumn.getColumnName(ctx.getCtx(), action.getAD_Column_ID());
		if (colName == null)
			throw new AdempiereException("Rule '" + rule.getName() + "': Target Column ID=" + action.getAD_Column_ID() + " not found.");

		Object value = evaluator.evaluate(action.getBXS_ValueExpression(), ctx);
		crossTable.computeIfAbsent(action.getBXS_Target_Table_ID(), k -> new LinkedHashMap<>()).put(colName, value);
	}

	private void applyCrossTableRecords(Map<Integer, Map<String, Object>> crossTable, EvaluationContext ctx) {
		String srcTrxName = ctx.getPo().get_TrxName();
		if (srcTrxName == null)
			throw new AdempiereException("Cross-table actions require a transaction; source record has no active transaction.");

		for (Map.Entry<Integer, Map<String, Object>> entry : crossTable.entrySet()) {
			int targetTableId = entry.getKey();
			Map<String, Object> colValues = entry.getValue();

			MTable targetTable = MTable.get(ctx.getCtx(), targetTableId);
			if (targetTable == null || targetTable.getAD_Table_ID() == 0)
				throw new AdempiereException("Cross-table action: target table ID=" + targetTableId + " not found.");

			PO relatedPo = targetTable.getPO(0, srcTrxName);
			if (relatedPo == null)
				throw new AdempiereException("Cross-table action: could not create PO for " + targetTable.getTableName() + ".");

			for (Map.Entry<String, Object> cv : colValues.entrySet()) {
				if (relatedPo.get_ColumnIndex(cv.getKey()) < 0)
					throw new AdempiereException("Cross-table action: column '" + cv.getKey()
							+ "' not found in " + targetTable.getTableName() + ". Check the Target Column configuration.");
				if (!relatedPo.set_ValueOfColumnReturningBoolean(cv.getKey(), cv.getValue()))
					throw new AdempiereException("Cross-table action: could not set column '" + cv.getKey()
							+ "' on " + targetTable.getTableName() + " (not updateable or invalid value).");
			}
			
			// Inherit AD_Org_ID from source PO if admin did not configure it explicitly
			if (!colValues.containsKey("AD_Org_ID") && relatedPo.get_ColumnIndex("AD_Org_ID") >= 0)
				relatedPo.set_ValueOfColumn("AD_Org_ID", ctx.getPo().get_Value("AD_Org_ID"));

			relatedPo.saveEx(); // exception propagates — iDempiere core rolls back the transaction
		}
	}

	private static Evaluatee evaluateeFor(EvaluationContext ctx) {
		return variableName -> toLogicValue(ExpressionEvaluator.resolveToken(variableName, ctx));
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
