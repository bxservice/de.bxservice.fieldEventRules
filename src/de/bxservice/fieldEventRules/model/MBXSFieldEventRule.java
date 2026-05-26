/******************************************************************************
 * Copyright (C) 2025 BX Service GmbH. All Rights Reserved.                  *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 *****************************************************************************/
package de.bxservice.fieldEventRules.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MField;
import org.compiere.model.MTab;
import org.compiere.util.Msg;

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

		return super.beforeSave(newRecord);
	}
}
