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
package de.bxservice.fieldEventRules.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.GridTab;
import org.compiere.model.PO;

/**
 * Immutable value object carrying everything the engine needs at evaluation time.
 * Both {@code po} and {@code gridTab} may be null simultaneously (valid for tests).
 */
public final class EvaluationContext {

	private final Properties ctx;
	private final PO po;
	private final GridTab gridTab;
	private final Map<String, Object> currentValues;
	private final Map<String, Object> resolvedParams;

	private EvaluationContext(Builder b) {
		this.ctx = b.ctx;
		this.po = b.po;
		this.gridTab = b.gridTab;
		this.currentValues = new HashMap<>(b.currentValues);
		this.resolvedParams = Collections.unmodifiableMap(new HashMap<>(b.resolvedParams));
	}

	/** Returns the Env context, never null (falls back to empty Properties). */
	public Properties getCtx() {
		return ctx != null ? ctx : new Properties();
	}

	/** The persistent object being edited; null on the UI-only path. */
	public PO getPo() {
		return po;
	}

	/** The ZK grid tab; null on the model-validator path. */
	public GridTab getGridTab() {
		return gridTab;
	}

	/** Live column values populated by the caller from the PO or the GridTab.
	 *  The map is mutable; the model validator writes back updated values after
	 *  each assignment so that later rules in the same pass see the new state. */
	public Map<String, Object> getCurrentValues() {
		return currentValues;
	}

	/** Pre-resolved named params from AD_FieldEventRuleParam. */
	public Map<String, Object> getResolvedParams() {
		return resolvedParams;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private Properties ctx;
		private PO po;
		private GridTab gridTab;
		private Map<String, Object> currentValues = new HashMap<>();
		private Map<String, Object> resolvedParams = new HashMap<>();

		public Builder ctx(Properties ctx) {
			this.ctx = ctx;
			return this;
		}

		public Builder po(PO po) {
			this.po = po;
			return this;
		}

		public Builder gridTab(GridTab gridTab) {
			this.gridTab = gridTab;
			return this;
		}

		public Builder currentValues(Map<String, Object> values) {
			this.currentValues = new HashMap<>(values);
			return this;
		}

		public Builder resolvedParams(Map<String, Object> params) {
			this.resolvedParams = new HashMap<>(params);
			return this;
		}

		public EvaluationContext build() {
			return new EvaluationContext(this);
		}
	}
}
