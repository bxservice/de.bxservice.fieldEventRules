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
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Evaluator;

/**
 * Resolves a {@code ValueExpression} string against an {@link EvaluationContext}.
 *
 * <p>Evaluation strategy by expression form:
 * <ol>
 *   <li>{@code @SQL=<query>} — token-substituted and executed as a scalar SQL query.
 *   <li>{@code 'literal'} — single-quoted string literal; quotes are stripped.
 *   <li>{@code @Token@} — single token resolved from the PO / current values, supporting
 *       core's reference operator {@code @C_BPartner_ID.Name@} and default-value operator
 *       {@code @C_Location_ID:0@} (see {@link #resolveToken}).
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
		if (trimmed.startsWith(SQL_PREFIX))
			return evaluateSqlQuery(trimmed.substring(SQL_PREFIX.length()), ctx);

		// String literal — strip the outer single quotes
		if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2)
			return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");

		// Single token — resolve from PO/context
		Matcher m = SINGLE_TOKEN_PATTERN.matcher(trimmed);
		if (m.matches())
			return resolveToken(m.group(1), ctx);

		// Arithmetic / multi-token — substitute as raw strings and parse in Java
		return evaluateArithmeticExpression(trimmed, ctx);
	}

	// -------------------------------------------------------------------------
	// SQL path
	// -------------------------------------------------------------------------

	/**
	 * Substitutes the {@code @Token@} references in {@code rawSql} as SQL literals
	 * and executes the result as a scalar query.
	 *
	 * @param rawSql SQL without the {@code @SQL=} prefix
	 * @return the single value returned by the query, or {@code null} if no row
	 */
	Object evaluateSqlQuery(String rawSql, EvaluationContext ctx) throws AdempiereException {
		String sql = substituteTokensForSQL(rawSql, ctx);
		assertSafeSql(sql);
		// Run inside the source record's transaction so the query can see
		// uncommitted data (e.g. the row just inserted on PO_AFTER_NEW).
		String trxName = ctx.getPo() != null ? ctx.getPo().get_TrxName() : null;
		try {
			List<List<Object>> rows = DB.getSQLArrayObjectsEx(trxName, sql);
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

	private static String substituteTokensForSQL(String expression, EvaluationContext ctx) {
		Matcher m = TOKEN_PATTERN.matcher(expression);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String token = m.group(1);
			// A token the author already wrapped in quotes ('@Name@') keeps those quotes;
			// only the value is escaped, otherwise it would end up quoted twice.
			boolean quoted = isQuotedToken(expression, m.start(), m.end());
			Object value = resolveToken(token, ctx);
			String rep;
			if (value == null) {
				log.warning("Token @" + token + "@ could not be resolved; substituting NULL");
				rep = quoted ? "" : "NULL";
			} else {
				rep = quoted ? escapeSQL(toSQLString(value)) : formatForSQL(value);
			}
			m.appendReplacement(sb, Matcher.quoteReplacement(rep));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/** True when the matched token is immediately surrounded by single quotes. */
	private static boolean isQuotedToken(String expression, int start, int end) {
		return start > 0 && end < expression.length()
				&& expression.charAt(start - 1) == '\'' && expression.charAt(end) == '\'';
	}

	private static String formatForSQL(Object value) {
		if (value instanceof Number)
			return value.toString();
		if (value instanceof Timestamp)
			return DB.TO_DATE((Timestamp) value, false);
		return "'" + escapeSQL(toSQLString(value)) + "'";
	}

	/** String form of a value as SQL expects it, without quotes. */
	private static String toSQLString(Object value) {
		// PO/GridField store YesNo columns as Boolean; SQL expects the 'Y'/'N' literal.
		if (value instanceof Boolean)
			return ((Boolean) value) ? "Y" : "N";
		return value.toString();
	}

	private static String escapeSQL(String value) {
		return value.replace("'", "''");
	}

	// -------------------------------------------------------------------------
	// Token resolution — the single resolver behind every path
	// -------------------------------------------------------------------------

	/**
	 * Resolves {@code token} to its typed value, honouring core's operators with core's
	 * semantics (see {@code DefaultEvaluatee.get_ValueAsString}):
	 * <ul>
	 *   <li>{@code @C_BPartner_ID.Name@} — reference operator: loads the referenced record
	 *       and returns the value of its {@code Name} column.
	 *   <li>{@code @C_Location_ID:0@} — default-value operator (IDEMPIERE-194): falls back to
	 *       the literal after the colon when the value is empty.
	 * </ul>
	 * Core resolves the reference operator before the default, so a default sits on the base
	 * ({@code @C_BPartner_ID:0.Name@}); the order here is the same.
	 *
	 * <p>Unlike core's evaluatee this returns the <b>typed</b> value rather than a String,
	 * which is what lets the SQL path quote by type and lets assignments reach typed columns.
	 */
	static Object resolveToken(String token, EvaluationContext ctx) {

		String foreignColumn = null;
		int f = token.indexOf(Evaluator.VARIABLE_REFERENCE_OPERATOR);
		if (f > 0 && token.substring(0, f).matches(".*[_]ID([:].+)?")) {
			foreignColumn = token.substring(f + 1);
			token = token.substring(0, f);
		} else if (f > 0 && !Env.isGlobalVariable(token)) {
			// Not <FK>_ID.Column: either the old @C_BPartner.Name@ form or a typo. It would
			// resolve to NULL and silently disable the rule, so say so instead. Global
			// variables (@$sysconfig.Foo@) legitimately carry a dot and are left alone.
			throw new AdempiereException("Token @" + token + "@: the reference operator needs an _ID"
					+ " column, e.g. @C_BPartner_ID.Name@");
		}

		String defaultValue = null;
		int idx = token.indexOf(Evaluator.VARIABLE_DEFAULT_VALUE_OPERATOR);
		if (idx > 0) {
			defaultValue = token.substring(idx + 1);
			token = token.substring(0, idx);
		}

		Object value = lookupToken(token, ctx);
		if (isEmpty(value) && defaultValue != null)
			value = defaultValueOf(defaultValue);
		if (foreignColumn != null && !isEmpty(value))
			value = resolveViaForeignKey(token, value, foreignColumn, ctx);
		return value;
	}

	/**
	 * Resolves a plain token from the lookup chain:
	 * resolvedParams → currentValues → PO column → Env context.
	 * The Env fallback covers core's {@code #}, {@code $} and {@code +} global prefixes.
	 */
	private static Object lookupToken(String token, EvaluationContext ctx) {
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

	/**
	 * Loads the record referenced by {@code fkColumn} = {@code idValue} and returns the typed
	 * value of {@code foreignColumn} on it, or null when the reference is not set.
	 */
	private static Object resolveViaForeignKey(
			String fkColumn, Object idValue, String foreignColumn, EvaluationContext ctx) {

		int id = intValueOf(idValue);
		if (id <= 0)
			return null;

		String foreignTable = foreignTableName(fkColumn, ctx);
		if (foreignTable == null)
			throw new AdempiereException("Token @" + fkColumn + "." + foreignColumn
					+ "@: " + fkColumn + " does not reference a table.");

		MTable table = MTable.get(ctx.getCtx(), foreignTable);
		PO foreignPo = table != null
				? table.getPO(id, ctx.getPo() != null ? ctx.getPo().get_TrxName() : null) : null;
		if (foreignPo == null)
			return null;
		if (foreignPo.get_ColumnIndex(foreignColumn) < 0)
			throw new AdempiereException("Token @" + fkColumn + "." + foreignColumn
					+ "@: column " + foreignColumn + " not found in " + foreignTable + ".");

		return foreignPo.get_Value(foreignColumn);
	}

	/** The table {@code fkColumn} points at, from the dictionary — same fallback as core. */
	private static String foreignTableName(String fkColumn, EvaluationContext ctx) {
		String tableName = ctx.getPo() != null ? ctx.getPo().get_TableName()
				: ctx.getGridTab() != null ? ctx.getGridTab().getTableName() : null;

		if (tableName != null) {
			MColumn column = MColumn.get(ctx.getCtx(), tableName, fkColumn);
			if (column != null && column.getReferenceTableName() != null)
				return column.getReferenceTableName();
		}

		// Same last resort as DefaultEvaluatee.getForeignTableName: strip _ID off the column name.
		String candidate = fkColumn.substring(0,
				fkColumn.length() - Evaluator.ID_COLUMN_SUFFIX.length());
		return MTable.get(ctx.getCtx(), candidate) != null ? candidate : null;
	}

	private static int intValueOf(Object value) {
		if (value instanceof Number)
			return ((Number) value).intValue();
		try {
			return Integer.parseInt(value.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Mirrors core's Util.isEmpty(value) check on the string form: null or "" takes the default. */
	private static boolean isEmpty(Object value) {
		return value == null || value.toString().isEmpty();
	}

	/**
	 * The literal after the colon in {@code @Name:default@}. A number-like default is returned as
	 * a BigDecimal so it substitutes unquoted ({@code @C_Location_ID:0@} → {@code 0}, not
	 * {@code '0'}); anything else stays a string and is quoted like any other text value.
	 */
	private static Object defaultValueOf(String defaultValue) {
		if (defaultValue.isEmpty())
			return null;
		try {
			return new BigDecimal(defaultValue);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
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
			Object value = resolveToken(token, ctx);
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
