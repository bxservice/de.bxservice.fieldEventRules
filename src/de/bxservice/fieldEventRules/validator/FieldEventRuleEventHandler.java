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
package de.bxservice.fieldEventRules.validator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.Adempiere;
import org.compiere.model.MColumn;
import org.compiere.model.PO;
import org.compiere.model.ServerStateChangeEvent;
import org.compiere.model.ServerStateChangeListener;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;

import de.bxservice.fieldEventRules.engine.EvaluationContext;
import de.bxservice.fieldEventRules.engine.FieldEventResult;
import de.bxservice.fieldEventRules.engine.FieldEventResult.FieldAssignment;
import de.bxservice.fieldEventRules.engine.FieldEventResult.ValidationMessage;
import de.bxservice.fieldEventRules.engine.FieldEventRuleCache;
import de.bxservice.fieldEventRules.engine.FieldEventRuleEngine;

@Component(
		reference = @Reference(
				name = "IEventManager",
				bind = "bindEventManager",
				unbind = "unbindEventManager",
				policy = ReferencePolicy.STATIC,
				cardinality = ReferenceCardinality.MANDATORY,
				service = IEventManager.class)
		)
public class FieldEventRuleEventHandler extends AbstractEventHandler {

	private static final CLogger log = CLogger.getCLogger(FieldEventRuleEventHandler.class);

	private static volatile FieldEventRuleEventHandler instance;

	public static FieldEventRuleEventHandler getInstance() { return instance; }

	private final Set<String> registeredTables = Collections.synchronizedSet(new HashSet<>());

	private static final String SQL_TABLE_NAMES =
			"SELECT DISTINCT t.TableName"
			+ " FROM BXS_FieldEventRule r"
			+ " JOIN AD_Table t ON t.AD_Table_ID = r.AD_Table_ID"
			+ " WHERE r.IsActive     = 'Y'"
			+ "   AND r.AD_Column_ID IS NOT NULL"
			+ "   AND r.TriggerEvent IN ('S', 'B')";

	@Override
	protected void initialize() {
		if (!Adempiere.isStarted()) {
			Adempiere.addServerStateChangeListener(new ServerStateChangeListener() {
				@Override
				public void stateChange(ServerStateChangeEvent event) {
					if (event.getEventType() == ServerStateChangeEvent.SERVER_START && Adempiere.isStarted())
							initialize();
				}
			});
			return;
		}

		instance = this;
		int count = 0;
		try (PreparedStatement pstmt = DB.prepareStatement(SQL_TABLE_NAMES, null);
				ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				String tableName = rs.getString(1);
				if (registeredTables.add(tableName)) {
					registerTableEvent(IEventTopics.PO_BEFORE_NEW, tableName);
					registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, tableName);
					count++;
				}
			}
		} catch (Exception e) {
			log.warning("FieldEventRuleEventHandler: failed to register tables — "
					+ e.getMessage());
		}
		log.info("FieldEventRuleEventHandler: registered for " + count + " tables.");
	}

	public synchronized void syncRegistrations(String trxName) {
		if (!Adempiere.isStarted()) return;
		try (PreparedStatement pstmt = DB.prepareStatement(SQL_TABLE_NAMES, trxName);
				ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				String tableName = rs.getString(1);
				if (registeredTables.add(tableName)) {
					registerTableEvent(IEventTopics.PO_BEFORE_NEW, tableName);
					registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, tableName);
					log.info("FieldEventRuleEventHandler: registered new table " + tableName);
				}
			}
		} catch (Exception e) {
			log.warning("FieldEventRuleEventHandler.syncRegistrations failed: " + e.getMessage());
		}
	}

	@Override
	protected void doHandleEvent(Event event) {
		PO po = getPO(event);
		if (po == null) return;

		EvaluationContext evalCtx = buildContext(po);
		List<Integer> columnIds = resolveActiveColumnIds(po.get_TableName());

		FieldEventResult combined = FieldEventResult.empty();
		FieldEventRuleEngine engine = new FieldEventRuleEngine();
		boolean isNewRecord = IEventTopics.PO_BEFORE_NEW.equals(event.getTopic());

		for (int adColumnId : columnIds) {
			if (!isNewRecord) {
				String columnName = MColumn.getColumnName(po.getCtx(), adColumnId);
				if (!po.is_ValueChanged(columnName))
					continue;
			}
			FieldEventResult result;
			try {
				result = engine.evaluateSaveTrigger(adColumnId, evalCtx);
			} catch (Exception e) {
				log.warning("FieldEventRule save evaluation failed for AD_Column_ID="
						+ adColumnId + " table=" + po.get_TableName()
						+ ": " + e.getMessage());
				continue;
			}
			applyAssignments(result, po, evalCtx);
			combined = merge(combined, result);
			if (combined.hasBlockingError()) break;
		}

		combined.getMessages().stream()
				.filter(m -> "W".equals(m.getLevel()))
				.map(ValidationMessage::getMessage)
				.findFirst()
				.ifPresent(msg -> log.saveWarning(msg, ""));

		combined.getMessages().stream()
				.filter(m -> "E".equals(m.getLevel()))
				.map(ValidationMessage::getMessage)
				.findFirst()
				.ifPresent(msg -> { throw new AdempiereException(msg); });
	}

	static EvaluationContext buildContext(PO po) {
		return EvaluationContext.builder()
				.ctx(po.getCtx())
				.po(po)
				.gridTab(null)
				.currentValues(extractCurrentValues(po))
				.resolvedParams(Collections.emptyMap())
				.build();
	}

	static Map<String, Object> extractCurrentValues(PO po) {
		int count = po.get_ColumnCount();
		Map<String, Object> values = new LinkedHashMap<>(count * 2);
		for (int i = 0; i < count; i++) {
			String name = po.get_ColumnName(i);
			Object value = po.get_Value(i);
			if (name != null)
				values.put(name, value);
		}
		return values;
	}

	static void applyAssignments(FieldEventResult result, PO po,
			EvaluationContext evalCtx) {
		for (FieldAssignment a : result.getAssignments()) {
			if (po.get_ColumnIndex(a.getColumnName()) < 0) {
				log.warning("FieldEventRule: column " + a.getColumnName()
						+ " not found on " + po.get_TableName() + ", skipping.");
				continue;
			}
			// Double-fire guard: skip if callout already applied this value
			Object current = po.get_Value(a.getColumnName());
			if (Objects.equals(current, a.getValue())) continue;

			po.set_ValueOfColumn(a.getColumnName(), a.getValue());
			// Keep evalCtx in sync so later rules in this pass see the updated value
			evalCtx.getCurrentValues().put(a.getColumnName(), a.getValue());
		}
	}

	static List<Integer> resolveActiveColumnIds(String tableName) {
		return FieldEventRuleCache.get().getColumnIdsByTableName(tableName);
	}

	static FieldEventResult merge(FieldEventResult a, FieldEventResult b) {
		FieldEventResult.Builder builder = new FieldEventResult.Builder();
		for (FieldAssignment fa : a.getAssignments())
			builder.addAssignment(fa.getColumnName(), fa.getValue());
		for (FieldAssignment fb : b.getAssignments())
			builder.addAssignment(fb.getColumnName(), fb.getValue());
		for (ValidationMessage ma : a.getMessages())
			builder.addMessage(ma.getMessage(), ma.getLevel(), ma.getColumnName());
		for (ValidationMessage mb : b.getMessages())
			builder.addMessage(mb.getMessage(), mb.getLevel(), mb.getColumnName());
		return builder.build();
	}
}
