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
/** Generated Model - DO NOT CHANGE */
package de.bxservice.fieldEventRules.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for BXS_FieldEventRule
 *  @author iDempiere (generated)
 *  @version Release 14 - $Id$ */
@org.adempiere.base.Model(table="BXS_FieldEventRule")
public class X_BXS_FieldEventRule extends PO implements I_BXS_FieldEventRule, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260527L;

    /** Standard Constructor */
    public X_BXS_FieldEventRule (Properties ctx, int BXS_FieldEventRule_ID, String trxName)
    {
      super (ctx, BXS_FieldEventRule_ID, trxName);
      /** if (BXS_FieldEventRule_ID == 0)
        {
			setBXS_FieldEventRule_ID (0);
			setBXS_RuleType (null);
			setName (null);
			setTriggerEvent (null);
        } */
    }

    /** Standard Constructor */
    public X_BXS_FieldEventRule (Properties ctx, int BXS_FieldEventRule_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, BXS_FieldEventRule_ID, trxName, virtualColumns);
      /** if (BXS_FieldEventRule_ID == 0)
        {
			setBXS_FieldEventRule_ID (0);
			setBXS_RuleType (null);
			setName (null);
			setTriggerEvent (null);
        } */
    }

    /** Standard Constructor */
    public X_BXS_FieldEventRule (Properties ctx, String BXS_FieldEventRule_UU, String trxName)
    {
      super (ctx, BXS_FieldEventRule_UU, trxName);
      /** if (BXS_FieldEventRule_UU == null)
        {
			setBXS_FieldEventRule_ID (0);
			setBXS_RuleType (null);
			setName (null);
			setTriggerEvent (null);
        } */
    }

    /** Standard Constructor */
    public X_BXS_FieldEventRule (Properties ctx, String BXS_FieldEventRule_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, BXS_FieldEventRule_UU, trxName, virtualColumns);
      /** if (BXS_FieldEventRule_UU == null)
        {
			setBXS_FieldEventRule_ID (0);
			setBXS_RuleType (null);
			setName (null);
			setTriggerEvent (null);
        } */
    }

    /** Load Constructor */
    public X_BXS_FieldEventRule (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_BXS_FieldEventRule[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Column)MTable.get(getCtx(), org.compiere.model.I_AD_Column.Table_ID)
			.getPO(getAD_Column_ID(), get_TrxName());
	}

	/** Set Column.
		@param AD_Column_ID Column in the table
	*/
	public void setAD_Column_ID (int AD_Column_ID)
	{
		if (AD_Column_ID < 1)
			set_Value (COLUMNNAME_AD_Column_ID, null);
		else
			set_Value (COLUMNNAME_AD_Column_ID, Integer.valueOf(AD_Column_ID));
	}

	/** Get Column.
		@return Column in the table
	  */
	public int getAD_Column_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Column_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Field getAD_Field() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Field)MTable.get(getCtx(), org.compiere.model.I_AD_Field.Table_ID)
			.getPO(getAD_Field_ID(), get_TrxName());
	}

	/** Set Field.
		@param AD_Field_ID Field on a database table
	*/
	public void setAD_Field_ID (int AD_Field_ID)
	{
		if (AD_Field_ID < 1)
			set_Value (COLUMNNAME_AD_Field_ID, null);
		else
			set_Value (COLUMNNAME_AD_Field_ID, Integer.valueOf(AD_Field_ID));
	}

	/** Get Field.
		@return Field on a database table
	  */
	public int getAD_Field_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Field_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Message getAD_Message() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Message)MTable.get(getCtx(), org.compiere.model.I_AD_Message.Table_ID)
			.getPO(getAD_Message_ID(), get_TrxName());
	}

	/** Set Message.
		@param AD_Message_ID System Message
	*/
	public void setAD_Message_ID (int AD_Message_ID)
	{
		if (AD_Message_ID < 1)
			set_Value (COLUMNNAME_AD_Message_ID, null);
		else
			set_Value (COLUMNNAME_AD_Message_ID, Integer.valueOf(AD_Message_ID));
	}

	/** Get Message.
		@return System Message
	  */
	public int getAD_Message_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Message_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Tab getAD_Tab() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Tab)MTable.get(getCtx(), org.compiere.model.I_AD_Tab.Table_ID)
			.getPO(getAD_Tab_ID(), get_TrxName());
	}

	/** Set Tab.
		@param AD_Tab_ID Tab within a Window
	*/
	public void setAD_Tab_ID (int AD_Tab_ID)
	{
		if (AD_Tab_ID < 1)
			set_Value (COLUMNNAME_AD_Tab_ID, null);
		else
			set_Value (COLUMNNAME_AD_Tab_ID, Integer.valueOf(AD_Tab_ID));
	}

	/** Get Tab.
		@return Tab within a Window
	  */
	public int getAD_Tab_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Tab_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Table)MTable.get(getCtx(), org.compiere.model.I_AD_Table.Table_ID)
			.getPO(getAD_Table_ID(), get_TrxName());
	}

	/** Set Table.
		@param AD_Table_ID Database Table information
	*/
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1)
			set_Value (COLUMNNAME_AD_Table_ID, null);
		else
			set_Value (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get Table.
		@return Database Table information
	  */
	public int getAD_Table_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Window getAD_Window() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Window)MTable.get(getCtx(), org.compiere.model.I_AD_Window.Table_ID)
			.getPO(getAD_Window_ID(), get_TrxName());
	}

	/** Set Window.
		@param AD_Window_ID Data entry or display window
	*/
	public void setAD_Window_ID (int AD_Window_ID)
	{
		if (AD_Window_ID < 1)
			set_Value (COLUMNNAME_AD_Window_ID, null);
		else
			set_Value (COLUMNNAME_AD_Window_ID, Integer.valueOf(AD_Window_ID));
	}

	/** Get Window.
		@return Data entry or display window
	  */
	public int getAD_Window_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Window_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Condition.
		@param BXS_Condition Condition rule that fires only if this evaluates to &#039;Y&#039; or any row is returned
	*/
	public void setBXS_Condition (String BXS_Condition)
	{
		set_Value (COLUMNNAME_BXS_Condition, BXS_Condition);
	}

	/** Get Condition.
		@return Condition rule that fires only if this evaluates to &#039;Y&#039; or any row is returned
	  */
	public String getBXS_Condition()
	{
		return (String)get_Value(COLUMNNAME_BXS_Condition);
	}

	/** Error (block save) = E */
	public static final String BXS_ERRORLEVEL_ErrorBlockSave = "E";
	/** Warning (allow continue) = W */
	public static final String BXS_ERRORLEVEL_WarningAllowContinue = "W";
	/** Set Error Level.
		@param BXS_ErrorLevel Error Level
	*/
	public void setBXS_ErrorLevel (String BXS_ErrorLevel)
	{

		set_Value (COLUMNNAME_BXS_ErrorLevel, BXS_ErrorLevel);
	}

	/** Get Error Level.
		@return Error Level	  */
	public String getBXS_ErrorLevel()
	{
		return (String)get_Value(COLUMNNAME_BXS_ErrorLevel);
	}

	/** Set Field Event Rule.
		@param BXS_FieldEventRule_ID Field Event Rule
	*/
	public void setBXS_FieldEventRule_ID (int BXS_FieldEventRule_ID)
	{
		if (BXS_FieldEventRule_ID < 1)
			set_ValueNoCheck (COLUMNNAME_BXS_FieldEventRule_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_BXS_FieldEventRule_ID, Integer.valueOf(BXS_FieldEventRule_ID));
	}

	/** Get Field Event Rule.
		@return Field Event Rule	  */
	public int getBXS_FieldEventRule_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_BXS_FieldEventRule_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set BXS_FieldEventRule_UU.
		@param BXS_FieldEventRule_UU BXS_FieldEventRule_UU
	*/
	public void setBXS_FieldEventRule_UU (String BXS_FieldEventRule_UU)
	{
		set_Value (COLUMNNAME_BXS_FieldEventRule_UU, BXS_FieldEventRule_UU);
	}

	/** Get BXS_FieldEventRule_UU.
		@return BXS_FieldEventRule_UU	  */
	public String getBXS_FieldEventRule_UU()
	{
		return (String)get_Value(COLUMNNAME_BXS_FieldEventRule_UU);
	}

	/** SET (data consequence) = S */
	public static final String BXS_RULETYPE_SETDataConsequence = "S";
	/** VALIDATE = V */
	public static final String BXS_RULETYPE_VALIDATE = "V";
	/** Set Rule Type.
		@param BXS_RuleType SET (data consequence), VALIDATE
	*/
	public void setBXS_RuleType (String BXS_RuleType)
	{

		set_Value (COLUMNNAME_BXS_RuleType, BXS_RuleType);
	}

	/** Get Rule Type.
		@return SET (data consequence), VALIDATE
	  */
	public String getBXS_RuleType()
	{
		return (String)get_Value(COLUMNNAME_BXS_RuleType);
	}

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Sequence.
		@param SeqNo Method of ordering records; lowest number comes first
	*/
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Both = B */
	public static final String TRIGGEREVENT_Both = "B";
	/** On Save (model) = S */
	public static final String TRIGGEREVENT_OnSaveModel = "S";
	/** UI = U */
	public static final String TRIGGEREVENT_UI = "U";
	/** Set Trigger Event.
		@param TriggerEvent On field change (UI), On save (model), Both
	*/
	public void setTriggerEvent (String TriggerEvent)
	{

		set_Value (COLUMNNAME_TriggerEvent, TriggerEvent);
	}

	/** Get Trigger Event.
		@return On field change (UI), On save (model), Both
	  */
	public String getTriggerEvent()
	{
		return (String)get_Value(COLUMNNAME_TriggerEvent);
	}

	/** Set Search Key.
		@param Value Search key for the record in the format required - must be unique
	*/
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}