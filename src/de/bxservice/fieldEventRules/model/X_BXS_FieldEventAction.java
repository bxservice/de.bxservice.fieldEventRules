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

/** Generated Model for BXS_FieldEventAction
 *  @author iDempiere (generated)
 *  @version Release 14 - $Id$ */
@org.adempiere.base.Model(table="BXS_FieldEventAction")
public class X_BXS_FieldEventAction extends PO implements I_BXS_FieldEventAction, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260526L;

    /** Standard Constructor */
    public X_BXS_FieldEventAction (Properties ctx, int BXS_FieldEventAction_ID, String trxName)
    {
      super (ctx, BXS_FieldEventAction_ID, trxName);
      /** if (BXS_FieldEventAction_ID == 0)
        {
			setAD_Column_ID (0);
			setBXS_ActionType (null);
			setBXS_FieldEventAction_ID (0);
			setBXS_FieldEventRule_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_BXS_FieldEventAction (Properties ctx, int BXS_FieldEventAction_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, BXS_FieldEventAction_ID, trxName, virtualColumns);
      /** if (BXS_FieldEventAction_ID == 0)
        {
			setAD_Column_ID (0);
			setBXS_ActionType (null);
			setBXS_FieldEventAction_ID (0);
			setBXS_FieldEventRule_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_BXS_FieldEventAction (Properties ctx, String BXS_FieldEventAction_UU, String trxName)
    {
      super (ctx, BXS_FieldEventAction_UU, trxName);
      /** if (BXS_FieldEventAction_UU == null)
        {
			setAD_Column_ID (0);
			setBXS_ActionType (null);
			setBXS_FieldEventAction_ID (0);
			setBXS_FieldEventRule_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_BXS_FieldEventAction (Properties ctx, String BXS_FieldEventAction_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, BXS_FieldEventAction_UU, trxName, virtualColumns);
      /** if (BXS_FieldEventAction_UU == null)
        {
			setAD_Column_ID (0);
			setBXS_ActionType (null);
			setBXS_FieldEventAction_ID (0);
			setBXS_FieldEventRule_ID (0);
        } */
    }

    /** Load Constructor */
    public X_BXS_FieldEventAction (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_BXS_FieldEventAction[")
        .append(get_UUID()).append("]");
      return sb.toString();
    }

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Column)MTable.get(getCtx(), org.compiere.model.I_AD_Column.Table_ID)
			.getPO(getAD_Column_ID(), get_TrxName());
	}

	/** Set Target Column.
		@param AD_Column_ID Column name to write into (e.g. C_BPartner_ID)
	*/
	public void setAD_Column_ID (int AD_Column_ID)
	{
		if (AD_Column_ID < 1)
			set_Value (COLUMNNAME_AD_Column_ID, null);
		else
			set_Value (COLUMNNAME_AD_Column_ID, Integer.valueOf(AD_Column_ID));
	}

	/** Get Target Column.
		@return Column name to write into (e.g. C_BPartner_ID)
	  */
	public int getAD_Column_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Column_ID);
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

	/** SET IF BLANK = B */
	public static final String BXS_ACTIONTYPE_SETIFBLANK = "B";
	/** CLEAR = C */
	public static final String BXS_ACTIONTYPE_CLEAR = "C";
	/** SET = S */
	public static final String BXS_ACTIONTYPE_SET = "S";
	/** Set Action Type.
		@param BXS_ActionType Action Type
	*/
	public void setBXS_ActionType (String BXS_ActionType)
	{

		set_Value (COLUMNNAME_BXS_ActionType, BXS_ActionType);
	}

	/** Get Action Type.
		@return Action Type	  */
	public String getBXS_ActionType()
	{
		return (String)get_Value(COLUMNNAME_BXS_ActionType);
	}

	/** Set Field Event Action.
		@param BXS_FieldEventAction_ID Field Event Action
	*/
	public void setBXS_FieldEventAction_ID (int BXS_FieldEventAction_ID)
	{
		if (BXS_FieldEventAction_ID < 1)
			set_ValueNoCheck (COLUMNNAME_BXS_FieldEventAction_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_BXS_FieldEventAction_ID, Integer.valueOf(BXS_FieldEventAction_ID));
	}

	/** Get Field Event Action.
		@return Field Event Action	  */
	public int getBXS_FieldEventAction_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_BXS_FieldEventAction_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set BXS_FieldEventAction_UU.
		@param BXS_FieldEventAction_UU BXS_FieldEventAction_UU
	*/
	public void setBXS_FieldEventAction_UU (String BXS_FieldEventAction_UU)
	{
		set_Value (COLUMNNAME_BXS_FieldEventAction_UU, BXS_FieldEventAction_UU);
	}

	/** Get BXS_FieldEventAction_UU.
		@return BXS_FieldEventAction_UU	  */
	public String getBXS_FieldEventAction_UU()
	{
		return (String)get_Value(COLUMNNAME_BXS_FieldEventAction_UU);
	}

	@Deprecated(since="13") // use better methods with cache
	public I_BXS_FieldEventRule getBXS_FieldEventRule() throws RuntimeException
	{
		return (I_BXS_FieldEventRule)MTable.get(getCtx(), I_BXS_FieldEventRule.Table_ID)
			.getPO(getBXS_FieldEventRule_ID(), get_TrxName());
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

	/** Set Value Expression.
		@param BXS_ValueExpression SQL scalar subquery or @variable@ expression that produces the new value
	*/
	public void setBXS_ValueExpression (String BXS_ValueExpression)
	{
		set_Value (COLUMNNAME_BXS_ValueExpression, BXS_ValueExpression);
	}

	/** Get Value Expression.
		@return SQL scalar subquery or @variable@ expression that produces the new value
	  */
	public String getBXS_ValueExpression()
	{
		return (String)get_Value(COLUMNNAME_BXS_ValueExpression);
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
}