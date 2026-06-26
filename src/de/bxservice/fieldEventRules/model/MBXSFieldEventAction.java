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
import org.compiere.util.DB;

import de.bxservice.fieldEventRules.engine.ConditionClauseValidator;
import de.bxservice.fieldEventRules.engine.FieldEventRuleCache;

public class MBXSFieldEventAction extends X_BXS_FieldEventAction {

	private static final long serialVersionUID = 1000041L;

	public MBXSFieldEventAction(Properties ctx, int BXS_FieldEventAction_ID, String trxName) {
		super(ctx, BXS_FieldEventAction_ID, trxName);
	}

	public MBXSFieldEventAction(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getSeqNo() == 0) {
			final String sql = "SELECT COALESCE(MAX(SeqNo),0) + 10 FROM "+ Table_Name
								+" WHERE " + COLUMNNAME_BXS_FieldEventRule_ID + "=?";
			int seqNo = DB.getSQLValueEx(get_TrxName(), sql, getBXS_FieldEventRule_ID());
			setSeqNo(seqNo);
		}

		if (newRecord || is_ValueChanged(COLUMNNAME_BXS_ValueExpression)) {
			try {
				ConditionClauseValidator.validateSqlExpression(getBXS_ValueExpression());
			} catch (AdempiereException e) {
				log.saveError("SQLExpressionValidation", e.getMessage());
				return false;
			}
			try {
				ConditionClauseValidator.dryRunSqlExpression(getBXS_ValueExpression());
			} catch (AdempiereException e) {
				log.saveError("SQLExpressionDryRun", e.getMessage());
				return false;
			}
		}

		return true;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (success)
			FieldEventRuleCache.get().invalidate();
		return super.afterSave(newRecord, success);
	}

	@Override
	protected boolean afterDelete(boolean success) {
		if (success)
			FieldEventRuleCache.get().invalidate();
		return super.afterDelete(success);
	}
}
