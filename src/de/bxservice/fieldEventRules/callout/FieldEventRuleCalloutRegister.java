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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import org.adempiere.base.IMappedColumnCalloutFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component(immediate = true)
public class FieldEventRuleCalloutRegister {

	private static final CLogger log = CLogger.getCLogger(FieldEventRuleCalloutRegister.class);

	private static volatile FieldEventRuleCalloutRegister instance;

	public static FieldEventRuleCalloutRegister getInstance() {
		return instance;
	}

	private static final String SQL =
			"SELECT DISTINCT t.TableName, c.ColumnName"
			+ " FROM BXS_FieldEventRule r"
			+ " JOIN AD_Column c  ON c.AD_Column_ID = r.AD_Column_ID"
			+ " JOIN AD_Table  t  ON t.AD_Table_ID  = r.AD_Table_ID"
			+ " WHERE r.IsActive    = 'Y'"
			+ "   AND r.TriggerEvent IN ('U', 'B')";

	@Reference(service = IMappedColumnCalloutFactory.class, cardinality = ReferenceCardinality.MANDATORY)
	private IMappedColumnCalloutFactory mappedCalloutFactory;

	@Activate
	public void activate(BundleContext context) {
		instance = this;
		registerAll();
	}

	@Deactivate
	public void deactivate() {
		instance = null;
	}

	public void registerAll() {
		registerAll(null);
	}

	public void registerAll(String trxName) {
		int columnCount = 0;
		Set<String> tables = new HashSet<>();

		try (PreparedStatement pstmt = DB.prepareStatement(SQL, trxName)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String tableName  = rs.getString(1);
				String columnName = rs.getString(2);
				mappedCalloutFactory.addMapping(tableName, columnName,
						() -> new FieldEventRuleCallout());
				tables.add(tableName);
				columnCount++;
			}
		} catch (Exception e) {
			log.warning("FieldEventRuleCalloutRegistrar: failed to register callouts — " + e.getMessage());
		}

		log.info("FieldEventRuleCalloutRegistrar: registered for "
				+ columnCount + " columns across " + tables.size() + " tables.");
	}

	public void syncRegistrations(String trxName) {
		registerAll(trxName);
	}
}
