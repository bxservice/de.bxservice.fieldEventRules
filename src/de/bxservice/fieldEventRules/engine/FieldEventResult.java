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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result produced by {@link FieldEventRuleEngine}.
 * Contains field assignments to apply and validation messages to surface.
 */
public final class FieldEventResult {

	private final List<FieldAssignment> assignments;
	private final List<ValidationMessage> messages;

	private FieldEventResult(List<FieldAssignment> assignments, List<ValidationMessage> messages) {
		this.assignments = Collections.unmodifiableList(new ArrayList<>(assignments));
		this.messages    = Collections.unmodifiableList(new ArrayList<>(messages));
	}

	public static FieldEventResult empty() {
		return new FieldEventResult(Collections.emptyList(), Collections.emptyList());
	}

	public List<FieldAssignment> getAssignments() {
		return assignments;
	}

	public List<ValidationMessage> getMessages() {
		return messages;
	}

	public boolean isEmpty() {
		return assignments.isEmpty() && messages.isEmpty();
	}

	public boolean hasBlockingError() {
		return messages.stream().anyMatch(m -> "E".equals(m.getLevel()));
	}

	public static final class FieldAssignment {

		private final String columnName;
		private final Object value;

		public FieldAssignment(String columnName, Object value) {
			this.columnName = columnName;
			this.value = value;
		}

		public String getColumnName() { return columnName; }
		public Object getValue()      { return value; }
	}

	public static final class ValidationMessage {

		private final String message;
		/** "E" = error (block save), "W" = warning (allow continue). */
		private final String level;
		private final String columnName;

		public ValidationMessage(String message, String level, String columnName) {
			this.message    = message;
			this.level      = level;
			this.columnName = columnName;
		}

		public String getMessage()    { return message; }
		public String getLevel()      { return level; }
		public String getColumnName() { return columnName; }
	}

	public static final class Builder {

		private final List<FieldAssignment>  assignments = new ArrayList<>();
		private final List<ValidationMessage> messages   = new ArrayList<>();

		public Builder addAssignment(String columnName, Object value) {
			assignments.add(new FieldAssignment(columnName, value));
			return this;
		}

		public Builder addMessage(String message, String level, String columnName) {
			messages.add(new ValidationMessage(message, level, columnName));
			return this;
		}

		public FieldEventResult build() {
			return new FieldEventResult(assignments, messages);
		}
	}
}
