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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Resolves a {@code ValueExpression} string against an {@link EvaluationContext}.
 *
 * <p>Evaluation strategy by expression form:
 * <ol>
 *   <li>{@code @SQL=<query>} — token-substituted and executed as a scalar SQL query.
 *   <li>{@code 'literal'} — single-quoted string literal; quotes are stripped.
 *   <li>{@code @Token@} — single token resolved directly from the PO / current values.
 *       Dotted form {@code @Table.Column@} navigates the foreign-key chain (like Kanban).
 *   <li>Arithmetic expression — tokens are resolved and substituted as numeric strings,
 *       then the result is evaluated by a pure-Java arithmetic parser (no SQL round-trip).
 * </ol>
 */
public class ExpressionEvaluator {

	private static final CLogger log = CLogger.getCLogger(ExpressionEvaluator.class);

	private static final String SQL_PREFIX = "@SQL=";

	private static final Pattern TOKEN_PATTERN = Pattern.compile("@([^@\\s]+)@");

	/** Matches an expression whose entire content is a single @Token@ (optional whitespace). */
	private static final Pattern SINGLE_TOKEN_PATTERN =
			Pattern.compile("^\\s*@([^@\\s]+)@\\s*$");

	/**
	 * Evaluates {@code expression} against {@code ctx}.
	 *
	 * @return the computed value, or {@code null} if the expression is blank
	 */
	public Object evaluate(String expression, EvaluationContext ctx)
			throws AdempiereException {

		if (expression == null || expression.isBlank())
			return null;

		String trimmed = expression.trim();

		// SQL expression — tokens substituted as SQL literals, then query executed
		if (trimmed.startsWith(SQL_PREFIX)) {
			String substituted = substituteTokensForSQL(trimmed, ctx);
			String sql = substituted.substring(SQL_PREFIX.length());
			assertSafeSql(sql);
			try {
				List<List<Object>> rows = DB.getSQLArrayObjectsEx(null, sql);
				if (rows == null || rows.isEmpty())
					return null;
				if (rows.size() > 1)
					throw new AdempiereException(
							"SQL expression for assignment returned " + rows.size()
							+ " rows; must return exactly one: " + sql);
				List<Object> firstRow = rows.get(0);
				return (firstRow != null && !firstRow.isEmpty()) ? firstRow.get(0) : null;
			} catch (AdempiereException e) {
				throw e;
			} catch (Exception e) {
				throw new AdempiereException(
						"SQL expression execution failed [" + sql + "]", e);
			}
		}

		// String literal — strip the outer single quotes
		if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2)
			return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");

		// Single token — resolve directly from PO/context (supports dotted notation)
		Matcher m = SINGLE_TOKEN_PATTERN.matcher(trimmed);
		if (m.matches())
			return resolveTokenDirect(m.group(1), ctx);

		// Arithmetic / multi-token — substitute as raw strings and parse in Java
		return evaluateArithmeticExpression(trimmed, ctx);
	}

	// -------------------------------------------------------------------------
	// SQL path
	// -------------------------------------------------------------------------

	private static String substituteTokensForSQL(String expression, EvaluationContext ctx) {
		Matcher m = TOKEN_PATTERN.matcher(expression);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String token = m.group(1);
			Object value = resolveTokenRaw(token, ctx);
			String rep;
			if (value == null) {
				log.warning("Token @" + token + "@ could not be resolved; substituting NULL");
				rep = "NULL";
			} else {
				rep = formatForSQL(value);
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(rep));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String formatForSQL(Object value) {
		if (value instanceof Number)
			return value.toString();
		if (value instanceof Timestamp)
			return DB.TO_DATE((Timestamp) value, false);
		// PO/GridField store YesNo columns as Boolean; SQL expects the 'Y'/'N' literal.
		if (value instanceof Boolean)
			return ((Boolean) value) ? "'Y'" : "'N'";
		String s = value.toString();
		return "'" + s.replace("'", "''") + "'";
	}

	// -------------------------------------------------------------------------
	// Non-SQL token resolution (like Kanban Board's parseVariable)
	// -------------------------------------------------------------------------

	/**
	 * Resolves {@code token} directly from the PO or context values without SQL.
	 * Supports dotted notation: {@code C_BPartner.Name} navigates via the FK column
	 * {@code C_BPartner_ID}, loads the referenced PO, and returns the sub-column value.
	 */
	private static Object resolveTokenDirect(String token, EvaluationContext ctx) {
		if (token.startsWith("$")) {
			String raw = Env.getContext(ctx.getCtx(), token);
			return raw.isEmpty() ? null : raw;
		}

		int dotIdx = token.indexOf('.');
		if (dotIdx > 0) {
			String fkBase = token.substring(0, dotIdx);
			String subCol = token.substring(dotIdx + 1);
			return resolveViaForeignKey(fkBase + "_ID", fkBase, subCol, ctx);
		}

		return resolveTokenRaw(token, ctx);
	}

	/**
	 * Loads the PO referenced by {@code fkCol} and returns the value of {@code subCol} on it.
	 * Works for both the PO context (model validator) and the UI context (callout).
	 */
	private static Object resolveViaForeignKey(
			String fkCol, String tableName, String subCol, EvaluationContext ctx) {

		PO po = ctx.getPo();
		if (po != null && po.get_ColumnIndex(fkCol) >= 0) {
			Integer id = (Integer) po.get_Value(fkCol);
			if (id != null && id > 0) {
				PO subPo = MTable.get(ctx.getCtx(), tableName).getPO(id, po.get_TrxName());
				if (subPo != null)
					return subPo.get_Value(subCol);
			}
			return null;
		}

		// UI callout path: FK id lives in currentValues
		Object idObj = ctx.getCurrentValues().get(fkCol);
		if (idObj instanceof Integer && (Integer) idObj > 0) {
			PO subPo = MTable.get(ctx.getCtx(), tableName).getPO((Integer) idObj, null);
			if (subPo != null)
				return subPo.get_Value(subCol);
		}
		return null;
	}

	/**
	 * Resolves a plain (non-dotted, non-{@code $}) token from the lookup chain:
	 * resolvedParams → currentValues → PO column → Env context.
	 */
	private static Object resolveTokenRaw(String token, EvaluationContext ctx) {
		Map<String, Object> params = ctx.getResolvedParams();
		if (params.containsKey(token)) return params.get(token);

		Map<String, Object> current = ctx.getCurrentValues();
		if (current.containsKey(token)) return current.get(token);

		PO po = ctx.getPo();
		if (po != null) {
			int idx = po.get_ColumnIndex(token);
			if (idx >= 0) return po.get_Value(idx);
		}

		String raw = Env.getContext(ctx.getCtx(), token);
		return raw.isEmpty() ? null : raw;
	}

	// -------------------------------------------------------------------------
	// Arithmetic path
	// -------------------------------------------------------------------------

	/**
	 * Substitutes each {@code @Token@} with its string value, then evaluates the
	 * resulting arithmetic expression using a pure-Java parser.
	 */
	private Object evaluateArithmeticExpression(String expression, EvaluationContext ctx) {
		Matcher m = TOKEN_PATTERN.matcher(expression);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String token = m.group(1);
			Object value = resolveTokenDirect(token, ctx);
			if (value == null) {
				log.warning("Token @" + token + "@ could not be resolved; substituting 0");
				value = BigDecimal.ZERO;
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
		}
		m.appendTail(sb);

		String substituted = sb.toString().trim();
		try {
			return ArithmeticParser.evaluate(substituted);
		} catch (Exception e) {
			throw new AdempiereException(
					"Cannot evaluate expression [" + expression + "]: " + e.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// Pure-Java arithmetic parser  (grammar: expr = term ((+|-) term)*)
	// -------------------------------------------------------------------------

	private static final class ArithmeticParser {

		private final String input;
		private int pos;

		private ArithmeticParser(String input) {
			this.input = input;
			this.pos   = 0;
		}

		static BigDecimal evaluate(String expr) {
			ArithmeticParser p = new ArithmeticParser(expr.trim());
			BigDecimal result = p.parseExpr();
			p.skipWS();
			if (p.pos < p.input.length())
				throw new AdempiereException(
						"Unexpected character '" + p.input.charAt(p.pos)
						+ "' at position " + p.pos + " in: " + expr);
			return result;
		}

		// expr = term (('+' | '-') term)*
		private BigDecimal parseExpr() {
			BigDecimal left = parseTerm();
			skipWS();
			while (pos < input.length()
					&& (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
				char op = input.charAt(pos++);
				BigDecimal right = parseTerm();
				left = op == '+' ? left.add(right) : left.subtract(right);
				skipWS();
			}
			return left;
		}

		// term = factor (('*' | '/') factor)*
		private BigDecimal parseTerm() {
			BigDecimal left = parseFactor();
			skipWS();
			while (pos < input.length()
					&& (input.charAt(pos) == '*' || input.charAt(pos) == '/')) {
				char op = input.charAt(pos++);
				BigDecimal right = parseFactor();
				left = op == '*'
						? left.multiply(right)
						: left.divide(right, 10, RoundingMode.HALF_UP);
				skipWS();
			}
			return left;
		}

		// factor = '-' factor | '(' expr ')' | number
		private BigDecimal parseFactor() {
			skipWS();
			if (pos >= input.length())
				throw new AdempiereException("Unexpected end of expression: " + input);
			if (input.charAt(pos) == '-') {
				pos++;
				return parseFactor().negate();
			}
			if (input.charAt(pos) == '(') {
				pos++;
				BigDecimal val = parseExpr();
				skipWS();
				if (pos >= input.length() || input.charAt(pos) != ')')
					throw new AdempiereException("Expected ')' in expression: " + input);
				pos++;
				return val;
			}
			return parseNumber();
		}

		private BigDecimal parseNumber() {
			int start = pos;
			while (pos < input.length()
					&& (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.'))
				pos++;
			if (start == pos)
				throw new AdempiereException(
						"Expected number at position " + pos + " in: " + input);
			return new BigDecimal(input.substring(start, pos));
		}

		private void skipWS() {
			while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
				pos++;
		}
	}

	// -------------------------------------------------------------------------
	// SQL safety check
	// -------------------------------------------------------------------------

	private static void assertSafeSql(String sql) throws AdempiereException {
		if (ConditionClauseValidator.hasForbiddenKeyword(sql))
			throw new AdempiereException(
					"SQL expression contains a forbidden keyword: " + sql);
	}
}
