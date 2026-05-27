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

/** Generated Interface for BXS_FieldEventRule
 *  @author iDempiere (generated) 
 *  @version Release 14
 */
@SuppressWarnings("all")
public interface I_BXS_FieldEventRule 
{

    /** TableName=BXS_FieldEventRule */
    public static final String Table_Name = "BXS_FieldEventRule";

    /** AD_Table_ID=1000040 */
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

	/** Set Column.
	  * Column in the table
	  */
	public void setAD_Column_ID (int AD_Column_ID);

	/** Get Column.
	  * Column in the table
	  */
	public int getAD_Column_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException;

    /** Column name AD_Field_ID */
    public static final String COLUMNNAME_AD_Field_ID = "AD_Field_ID";

	/** Set Field.
	  * Field on a database table
	  */
	public void setAD_Field_ID (int AD_Field_ID);

	/** Get Field.
	  * Field on a database table
	  */
	public int getAD_Field_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Field getAD_Field() throws RuntimeException;

    /** Column name AD_Message_ID */
    public static final String COLUMNNAME_AD_Message_ID = "AD_Message_ID";

	/** Set Message.
	  * System Message
	  */
	public void setAD_Message_ID (int AD_Message_ID);

	/** Get Message.
	  * System Message
	  */
	public int getAD_Message_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Message getAD_Message() throws RuntimeException;

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

    /** Column name AD_Tab_ID */
    public static final String COLUMNNAME_AD_Tab_ID = "AD_Tab_ID";

	/** Set Tab.
	  * Tab within a Window
	  */
	public void setAD_Tab_ID (int AD_Tab_ID);

	/** Get Tab.
	  * Tab within a Window
	  */
	public int getAD_Tab_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Tab getAD_Tab() throws RuntimeException;

    /** Column name AD_Table_ID */
    public static final String COLUMNNAME_AD_Table_ID = "AD_Table_ID";

	/** Set Table.
	  * Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID);

	/** Get Table.
	  * Database Table information
	  */
	public int getAD_Table_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException;

    /** Column name AD_Window_ID */
    public static final String COLUMNNAME_AD_Window_ID = "AD_Window_ID";

	/** Set Window.
	  * Data entry or display window
	  */
	public void setAD_Window_ID (int AD_Window_ID);

	/** Get Window.
	  * Data entry or display window
	  */
	public int getAD_Window_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Window getAD_Window() throws RuntimeException;

    /** Column name BXS_Condition */
    public static final String COLUMNNAME_BXS_Condition = "BXS_Condition";

	/** Set Condition.
	  * Condition rule that fires only if this evaluates to &#039;
Y&#039;
 or any row is returned
	  */
	public void setBXS_Condition (String BXS_Condition);

	/** Get Condition.
	  * Condition rule that fires only if this evaluates to &#039;
Y&#039;
 or any row is returned
	  */
	public String getBXS_Condition();

    /** Column name BXS_ErrorLevel */
    public static final String COLUMNNAME_BXS_ErrorLevel = "BXS_ErrorLevel";

	/** Set Error Level	  */
	public void setBXS_ErrorLevel (String BXS_ErrorLevel);

	/** Get Error Level	  */
	public String getBXS_ErrorLevel();

    /** Column name BXS_FieldEventRule_ID */
    public static final String COLUMNNAME_BXS_FieldEventRule_ID = "BXS_FieldEventRule_ID";

	/** Set Field Event Rule	  */
	public void setBXS_FieldEventRule_ID (int BXS_FieldEventRule_ID);

	/** Get Field Event Rule	  */
	public int getBXS_FieldEventRule_ID();

    /** Column name BXS_FieldEventRule_UU */
    public static final String COLUMNNAME_BXS_FieldEventRule_UU = "BXS_FieldEventRule_UU";

	/** Set BXS_FieldEventRule_UU	  */
	public void setBXS_FieldEventRule_UU (String BXS_FieldEventRule_UU);

	/** Get BXS_FieldEventRule_UU	  */
	public String getBXS_FieldEventRule_UU();

    /** Column name BXS_RuleType */
    public static final String COLUMNNAME_BXS_RuleType = "BXS_RuleType";

	/** Set Rule Type.
	  * SET (data consequence), VALIDATE
	  */
	public void setBXS_RuleType (String BXS_RuleType);

	/** Get Rule Type.
	  * SET (data consequence), VALIDATE
	  */
	public String getBXS_RuleType();

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

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

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

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

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

    /** Column name TriggerEvent */
    public static final String COLUMNNAME_TriggerEvent = "TriggerEvent";

	/** Set Trigger Event.
	  * On field change (UI), On save (model), Both
	  */
	public void setTriggerEvent (String TriggerEvent);

	/** Get Trigger Event.
	  * On field change (UI), On save (model), Both
	  */
	public String getTriggerEvent();

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

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();
}
