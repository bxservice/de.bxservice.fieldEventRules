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

public class MBXSFieldEventAction extends X_BXS_FieldEventAction {

	private static final long serialVersionUID = 1000041L;

	public MBXSFieldEventAction(Properties ctx, int BXS_FieldEventAction_ID, String trxName) {
		super(ctx, BXS_FieldEventAction_ID, trxName);
	}

	public MBXSFieldEventAction(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}
