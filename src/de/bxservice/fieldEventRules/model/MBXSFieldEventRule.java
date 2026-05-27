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
package de.bxservice.fieldEventRules.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MField;
import org.compiere.model.MTab;
import org.compiere.util.DB;
import org.compiere.util.Msg;

import de.bxservice.fieldEventRules.callout.FieldEventRuleCalloutRegister;
import de.bxservice.fieldEventRules.engine.ConditionClauseValidator;
import de.bxservice.fieldEventRules.engine.FieldEventRuleCache;
import de.bxservice.fieldEventRules.validator.FieldEventRuleEventHandler;

public class MBXSFieldEventRule extends X_BXS_FieldEventRule {

	private static final long serialVersionUID = 1000040L;

	public MBXSFieldEventRule(Properties ctx, int BXS_FieldEventRule_ID, String trxName) {
		super(ctx, BXS_FieldEventRule_ID, trxName);
	}

	public MBXSFieldEventRule(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getSeqNo() == 0) {
			final String sql = "SELECT COALESCE(MAX(SeqNo),0) + 10 FROM "+ Table_Name
								+" WHERE AD_Table_ID=? AND AD_Column_ID=?";
			int seqNo = DB.getSQLValueEx(get_TrxName(), sql, getAD_Table_ID(), getAD_Column_ID());
			setSeqNo(seqNo);
		}
		
		if (getAD_Tab_ID() > 0) {
			MTab tab = MTab.get(getAD_Tab_ID());
			if (tab != null) {
				if (getAD_Table_ID() == 0)
					setAD_Table_ID(tab.getAD_Table_ID());
				else if (tab.getAD_Table_ID() != getAD_Table_ID())
					throw new AdempiereException(
							Msg.getElement(getCtx(), COLUMNNAME_AD_Tab_ID) + " / "
							+ Msg.getElement(getCtx(), COLUMNNAME_AD_Table_ID)
							+ " mismatch");
			}
		}

		if (getAD_Field_ID() > 0) {
			MField field = MField.get(getAD_Field_ID());
			if (field != null) {
				setAD_Column_ID(field.getAD_Column_ID());
				
				if (field.getAD_Column_ID() != getAD_Column_ID())
					throw new AdempiereException(
							Msg.getElement(getCtx(), COLUMNNAME_AD_Field_ID) + " / "
							+ Msg.getElement(getCtx(), COLUMNNAME_AD_Column_ID)
							+ " mismatch");
			}
		}

		// Either AD_Window_ID or AD_Table_ID must be filled
		if (getAD_Window_ID() == 0 && getAD_Table_ID() == 0) {
			log.saveError("FillMandatory",
					Msg.getElement(getCtx(), COLUMNNAME_AD_Window_ID) + " / "
					+ Msg.getElement(getCtx(), COLUMNNAME_AD_Table_ID));
			return false;
		}

		if (newRecord || is_ValueChanged(COLUMNNAME_BXS_Condition)) {
			try {
				ConditionClauseValidator.validate(getBXS_Condition());
			} catch (AdempiereException e) {
				log.saveError("ConditionClause", e.getMessage());
				return false;
			}
		}

		return super.beforeSave(newRecord);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (success) {
			FieldEventRuleCache.get().invalidate();
			FieldEventRuleCalloutRegister register = FieldEventRuleCalloutRegister.getInstance();
			if (register != null) register.syncRegistrations();
			FieldEventRuleEventHandler handler = FieldEventRuleEventHandler.getInstance();
			if (handler != null) handler.syncRegistrations();
		}
		return super.afterSave(newRecord, success);
	}

	@Override
	protected boolean afterDelete(boolean success) {
		if (success) {
			FieldEventRuleCache.get().invalidate();
			FieldEventRuleCalloutRegister register = FieldEventRuleCalloutRegister.getInstance();
			if (register != null) register.syncRegistrations();
			FieldEventRuleEventHandler handler = FieldEventRuleEventHandler.getInstance();
			if (handler != null) handler.syncRegistrations();
		}
		return super.afterDelete(success);
	}
}
