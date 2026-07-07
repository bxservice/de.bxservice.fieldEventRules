/**********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.             *
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import de.bxservice.fieldEventRules.model.MBXSFieldEventAction;
import de.bxservice.fieldEventRules.model.MBXSFieldEventRule;
import de.bxservice.fieldEventRules.model.X_BXS_FieldEventAction;
import de.bxservice.fieldEventRules.model.X_BXS_FieldEventRule;

public class FieldEventRuleCache {

	private static final CLogger log = CLogger.getCLogger(FieldEventRuleCache.class);

	private static final FieldEventRuleCache INSTANCE = new FieldEventRuleCache();

	private static final String SQL_COLUMN_IDS =
			"SELECT r.AD_Column_ID"
			+ " FROM BXS_FieldEventRule r"
			+ " JOIN AD_Table t ON t.AD_Table_ID = r.AD_Table_ID"
			+ " WHERE t.TableName    = ?"
			+ "   AND r.IsActive     = 'Y'"
			+ "   AND r.AD_Column_ID IS NOT NULL"
			+ "   AND r.TriggerEvent IN ('S', 'B', 'A')"
			+ " ORDER BY r.SeqNo";

	private static final CCache<Integer, List<MBXSFieldEventRule>> rulesByColumnId =
			new CCache<>(MBXSFieldEventRule.Table_Name, "BXS_FieldEventRuleByColumn", 30, 0, false, 500);
	private static final CCache<Integer, List<MBXSFieldEventRule>> columnlessRulesByTable =
			new CCache<>(MBXSFieldEventRule.Table_Name, "BXS_FieldEventRuleColumnlessByTable", 30, 0, false, 500);
	private static final CCache<String, List<Integer>> columnIdsByTable =
			new CCache<>(MBXSFieldEventRule.Table_Name, "BXS_FieldEventRuleColumnsByTable", 30, 0, false, 500);
	private static final CCache<Integer, List<MBXSFieldEventAction>> actionsByRuleId =
			new CCache<>(MBXSFieldEventAction.Table_Name, "BXS_FieldEventActionByRule", 30, 0, false, 500);

	private FieldEventRuleCache() {}

	public static FieldEventRuleCache get() {
		return INSTANCE;
	}

	public List<MBXSFieldEventRule> getRulesByColumnId(int adColumnId) {
		List<MBXSFieldEventRule> list = rulesByColumnId.get(adColumnId);
		if (list != null)
			return list;
		list = loadRulesByColumnId(adColumnId);
		rulesByColumnId.put(adColumnId, list);
		return list;
	}

	public List<MBXSFieldEventRule> getColumnlessRulesByTableId(int adTableId) {
		List<MBXSFieldEventRule> list = columnlessRulesByTable.get(adTableId);
		if (list != null)
			return list;
		list = loadColumnlessRulesByTableId(adTableId);
		columnlessRulesByTable.put(adTableId, list);
		return list;
	}

	public List<Integer> getColumnIdsByTableName(String tableName) {
		List<Integer> ids = columnIdsByTable.get(tableName);
		if (ids != null)
			return ids;
		ids = loadColumnIdsByTable(tableName);
		columnIdsByTable.put(tableName, ids);
		return ids;
	}

	public List<MBXSFieldEventAction> getActionsByRuleId(int ruleId) {
		List<MBXSFieldEventAction> list = actionsByRuleId.get(ruleId);
		if (list != null)
			return list;
		list = loadActionsByRuleId(ruleId);
		actionsByRuleId.put(ruleId, list);
		return list;
	}

	public void invalidate() {
		rulesByColumnId.reset();
		columnlessRulesByTable.reset();
		columnIdsByTable.reset();
		actionsByRuleId.reset();
		log.fine("FieldEventRuleCache invalidated");
	}

	private static List<MBXSFieldEventRule> loadRulesByColumnId(int adColumnId) {
		try {
			List<MBXSFieldEventRule> list = new Query(Env.getCtx(),
					MBXSFieldEventRule.Table_Name,
					X_BXS_FieldEventRule.COLUMNNAME_AD_Column_ID + " = ? AND AD_Client_ID IN (0,?)", null)
					.setOnlyActiveRecords(true)
					.setParameters(adColumnId, Env.getAD_Client_ID(Env.getCtx()))
					.setOrderBy(X_BXS_FieldEventRule.COLUMNNAME_SeqNo)
					.list();
			return Collections.unmodifiableList(list);
		} catch (Exception e) {
			log.warning("FieldEventRuleCache: failed to load rules for AD_Column_ID=" + adColumnId + ": " + e.getMessage());
			return Collections.emptyList();
		}
	}

	private static List<MBXSFieldEventRule> loadColumnlessRulesByTableId(int adTableId) {
		try {
			List<MBXSFieldEventRule> list = new Query(Env.getCtx(),
					MBXSFieldEventRule.Table_Name,
					X_BXS_FieldEventRule.COLUMNNAME_AD_Table_ID + " = ?"
					+ " AND " + X_BXS_FieldEventRule.COLUMNNAME_AD_Column_ID + " IS NULL"
					+ " AND " + X_BXS_FieldEventRule.COLUMNNAME_TriggerEvent + " IN (?,?)"
					+ " AND AD_Client_ID IN (0,?)", null)
					.setOnlyActiveRecords(true)
					.setParameters(adTableId,
							X_BXS_FieldEventRule.TRIGGEREVENT_OnSaveModel,
							X_BXS_FieldEventRule.TRIGGEREVENT_AfterNew,
							Env.getAD_Client_ID(Env.getCtx()))
					.setOrderBy(X_BXS_FieldEventRule.COLUMNNAME_SeqNo)
					.list();
			return Collections.unmodifiableList(list);
		} catch (Exception e) {
			log.warning("FieldEventRuleCache: failed to load column-less rules for AD_Table_ID=" + adTableId + ": " + e.getMessage());
			return Collections.emptyList();
		}
	}

	private static List<Integer> loadColumnIdsByTable(String tableName) {
		List<Integer> ids = new ArrayList<>();
		try (PreparedStatement pstmt = DB.prepareStatement(SQL_COLUMN_IDS, null)) {
			pstmt.setString(1, tableName);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next())
					ids.add(rs.getInt(1));
			}
		} catch (Exception e) {
			log.warning("FieldEventRuleCache: failed to load column IDs for table " + tableName + ": " + e.getMessage());
		}
		return Collections.unmodifiableList(ids);
	}

	private static List<MBXSFieldEventAction> loadActionsByRuleId(int ruleId) {
		try {
			List<MBXSFieldEventAction> list = new Query(Env.getCtx(),
					MBXSFieldEventAction.Table_Name,
					X_BXS_FieldEventAction.COLUMNNAME_BXS_FieldEventRule_ID + " = ?", null)
					.setParameters(ruleId)
					.setOrderBy(X_BXS_FieldEventAction.COLUMNNAME_SeqNo)
					.list();
			return Collections.unmodifiableList(list);
		} catch (Exception e) {
			log.warning("FieldEventRuleCache: failed to load actions for BXS_FieldEventRule_ID=" + ruleId + ": " + e.getMessage());
			return Collections.emptyList();
		}
	}
}
