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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.exceptions.AdempiereException;
import org.idempiere.expression.logic.LogicEvaluator;

/**
 * Validates and identifies the format of a BXS_FieldEventRule ConditionClause.
 */
public class ConditionClauseValidator {

	enum Format { CONTEXT_EXPRESSION, SQL_WHERE }

	private static final String SQL_PREFIX = "@SQL=";

	private static final Pattern TOKEN_PATTERN =
			Pattern.compile("@[^@\\s]+@");

	private static final Pattern FORBIDDEN_KEYWORD_PATTERN =
			Pattern.compile(
				"\\b(DROP|DELETE|UPDATE|INSERT|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE)\\b",
				Pattern.CASE_INSENSITIVE);

	/**
	 * Returns the detected format if the clause is valid.
	 * Throws ConditionClauseException with a user-readable message if invalid.
	 */
	public static Optional<Format> validate(String clause) throws AdempiereException {
		if (clause == null || clause.isBlank())	
			return Optional.empty();

		if (clause.startsWith(SQL_PREFIX))
			return validateSqlFormat(clause);

		return validateContextExpression(clause);
	}

	/** Package-accessible check reused by {@link ExpressionEvaluator#assertSafeSql}. */
	static boolean hasForbiddenKeyword(String sql) {
		return FORBIDDEN_KEYWORD_PATTERN.matcher(sql).find();
	}

	private static Optional<Format> validateSqlFormat(String clause) throws AdempiereException {
		String fragment = clause.substring(SQL_PREFIX.length());
		if (fragment.isBlank())
			throw new AdempiereException(
				"ConditionClause SQL format (@SQL=...) must have a non-empty WHERE fragment after the prefix.");

		String normalized = TOKEN_PATTERN.matcher(fragment).replaceAll("NULL");

		Matcher m = FORBIDDEN_KEYWORD_PATTERN.matcher(normalized);
		if (m.find())
			throw new AdempiereException(
				"ConditionClause SQL contains forbidden keyword: " + m.group().toUpperCase() + ".");

		return Optional.of(Format.SQL_WHERE);
	}

	private static Optional<Format> validateContextExpression(String clause) throws AdempiereException {
		if (!TOKEN_PATTERN.matcher(clause).find())
			throw new AdempiereException(
				"ConditionClause is not a valid context expression: missing @Token@ reference.");

		try {
			LogicEvaluator.validate(clause);
		} catch (AdempiereException e) {
			throw new AdempiereException(
				"ConditionClause context expression could not be parsed. Check operators (&, |) and @Token@ syntax.");
		}

		return Optional.of(Format.CONTEXT_EXPRESSION);
	}
}
