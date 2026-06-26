/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package de.bxservice.fieldEventRules.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for BXS_FieldEventAction
 *  @author iDempiere (generated) 
 *  @version Release 14
 */
@SuppressWarnings("all")
public interface I_BXS_FieldEventAction 
{

    /** TableName=BXS_FieldEventAction */
    public static final String Table_Name = "BXS_FieldEventAction";

    /** AD_Table_ID=1000041 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 6 - System - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(6);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Column_ID */
    public static final String COLUMNNAME_AD_Column_ID = "AD_Column_ID";

	/** Set Target Column.
	  * Column name to write into (e.g. C_BPartner_ID)
	  */
	public void setAD_Column_ID (int AD_Column_ID);

	/** Get Target Column.
	  * Column name to write into (e.g. C_BPartner_ID)
	  */
	public int getAD_Column_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException;

    /** Column name AD_Target_Table_ID */
    public static final String COLUMNNAME_AD_Target_Table_ID = "AD_Target_Table_ID";

	/** Set Target Table.
	  * Table to create a new related record in (cross-table actions)
	  */
	public void setAD_Target_Table_ID (int AD_Target_Table_ID);

	/** Get Target Table.
	  * Table to create a new related record in (cross-table actions)
	  */
	public int getAD_Target_Table_ID();
	



    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name BXS_ActionType */
    public static final String COLUMNNAME_BXS_ActionType = "BXS_ActionType";

	/** Set Action Type	  */
	public void setBXS_ActionType (String BXS_ActionType);

	/** Get Action Type	  */
	public String getBXS_ActionType();

    /** Column name BXS_FieldEventAction_ID */
    public static final String COLUMNNAME_BXS_FieldEventAction_ID = "BXS_FieldEventAction_ID";

	/** Set Field Event Action	  */
	public void setBXS_FieldEventAction_ID (int BXS_FieldEventAction_ID);

	/** Get Field Event Action	  */
	public int getBXS_FieldEventAction_ID();

    /** Column name BXS_FieldEventAction_UU */
    public static final String COLUMNNAME_BXS_FieldEventAction_UU = "BXS_FieldEventAction_UU";

	/** Set BXS_FieldEventAction_UU	  */
	public void setBXS_FieldEventAction_UU (String BXS_FieldEventAction_UU);

	/** Get BXS_FieldEventAction_UU	  */
	public String getBXS_FieldEventAction_UU();

    /** Column name BXS_FieldEventRule_ID */
    public static final String COLUMNNAME_BXS_FieldEventRule_ID = "BXS_FieldEventRule_ID";

	/** Set Field Event Rule	  */
	public void setBXS_FieldEventRule_ID (int BXS_FieldEventRule_ID);

	/** Get Field Event Rule	  */
	public int getBXS_FieldEventRule_ID();

	@Deprecated(since="13") // use better methods with cache
	public I_BXS_FieldEventRule getBXS_FieldEventRule() throws RuntimeException;

    /** Column name BXS_ValueExpression */
    public static final String COLUMNNAME_BXS_ValueExpression = "BXS_ValueExpression";

	/** Set Value Expression.
	  * SQL scalar subquery or @variable@ expression that produces the new value
	  */
	public void setBXS_ValueExpression (String BXS_ValueExpression);

	/** Get Value Expression.
	  * SQL scalar subquery or @variable@ expression that produces the new value
	  */
	public String getBXS_ValueExpression();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name SeqNo */
    public static final String COLUMNNAME_SeqNo = "SeqNo";

	/** Set Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public void setSeqNo (int SeqNo);

	/** Get Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public int getSeqNo();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();
}
