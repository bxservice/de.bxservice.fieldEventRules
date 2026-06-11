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
package de.bxservice.fieldEventRules.callout;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.CLogger;

import de.bxservice.fieldEventRules.engine.EvaluationContext;
import de.bxservice.fieldEventRules.engine.FieldEventResult;
import de.bxservice.fieldEventRules.engine.FieldEventResult.ValidationMessage;
import de.bxservice.fieldEventRules.engine.FieldEventRuleEngine;

public class FieldEventRuleCallout implements IColumnCallout {

	private static final CLogger log = CLogger.getCLogger(FieldEventRuleCallout.class);

	@Override
	public String start(Properties ctx, int WindowNo,
			GridTab mTab, GridField mField, Object value, Object oldValue) {

		int adFieldId = mField.getAD_Field_ID();
		if (adFieldId <= 0) return "";

		EvaluationContext evalCtx = buildContext(ctx, WindowNo, mTab, mField, value);

		FieldEventRuleEngine engine = new FieldEventRuleEngine();
		FieldEventResult result;
		try {
			result = engine.evaluateUITrigger(adFieldId, evalCtx);
		} catch (Exception e) {
			log.warning("FieldEventRule evaluation failed for AD_Field_ID="
					+ adFieldId + ": " + e.getMessage());
			return "";
		}

		applyResult(result, mTab);

		result.getMessages().stream()
				.filter(m -> "W".equals(m.getLevel()))
				.map(ValidationMessage::getMessage)
				.findFirst()
				.ifPresent(msg -> mTab.fireDataStatusEEvent(msg, "", false));

		return result.getMessages().stream()
				.filter(m -> "E".equals(m.getLevel()))
				.map(ValidationMessage::getMessage)
				.findFirst()
				.orElse("");
	}

	static EvaluationContext buildContext(
			Properties ctx, int windowNo,
			GridTab mTab, GridField mField, Object value) {

		Map<String, Object> currentValues = new LinkedHashMap<>();
		for (GridField f : mTab.getFields())
			currentValues.put(f.getColumnName(), f.getValue());
		currentValues.put(mField.getColumnName(), value);

		return EvaluationContext.builder()
				.ctx(ctx)
				.po(null)
				.gridTab(mTab)
				.currentValues(currentValues)
				.resolvedParams(Collections.emptyMap())
				.build();
	}

	static void applyResult(FieldEventResult result, GridTab mTab) {
		for (FieldEventResult.FieldAssignment assignment : result.getAssignments()) {
			String colName = assignment.getColumnName();
			if (mTab.getField(colName) == null) {
				log.warning("FieldEventRule: column " + colName
						+ " not found on tab " + mTab.getName() + " — skipping");
				continue;
			}
			mTab.setValue(colName, assignment.getValue());
		}
	}
}
