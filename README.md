# Field Event Rules

Field Event Rules let you define "when this field changes, do that" logic directly inside the iDempiere Application Dictionary — without writing Java code, installing plugins, or touching an IDE.

Think of it as a lightweight form event system: you attach rules to fields or columns, describe what should happen in plain SQL or a simple expression, and the system takes care of executing them both in the UI and when records are saved through any channel.

---

## What you can do with it

- **Auto-fill fields** — when a user picks a Business Partner, automatically populate the Payment Term, Price List, or any other field from a lookup.
- **Recalculate derived values** — when Quantity or Unit Price changes, recompute the Line Amount.
- **Copy values between fields** — when an Org is selected, default the Warehouse from that org's configuration.
- **Conditional defaults** — set a field only when it is currently blank, leaving intentional values untouched.
- **Validate data** — show an error or warning when a value breaks a business rule, either immediately when the field is changed or when the record is saved.

All of these work whether the record is created through a window, a background process, a data import, or the API.

---

## How it works conceptually

A Field Event Rule sits between a field (or column) and a piece of logic you define. It has three parts:

**1. Trigger** — when does it fire?
- *On field change* — fires immediately in the UI when a user leaves the field, the same moment a callout would.
- *On save* — fires when the record is being saved, regardless of how it was created.
- *Both* — fires at both moments, so the UI stays responsive and data consistency is guaranteed for non-UI operations too.

**2. Condition** *(optional)* — should it fire this time?
An optional guard expression. If it evaluates to false, the rule is skipped entirely. Useful for rules that only apply in certain situations (e.g. only on Sales Orders, only when the amount exceeds a threshold).

**3. Actions** — what happens?
One or more ordered steps. Each action sets a field to a computed value, clears it, or validates it.

---

## Where to configure rules

Open the **Field Event Rules** window from the Application Dictionary menu (System tenant or your tenant, depending on your access).

Each rule record has:

| Field | What it does |
|---|---|
| Name | A label for the rule, shown in lists |
| Window / Tab / Field | Scope the rule to a specific place in the UI. Leave blank to apply model-wide. |
| Table / Column | Attach the rule at the column level so it fires for any window using that column. |
| Trigger | On field change / On save / Both |
| Execution scope | UI only / Model only / Both |
| Condition | Optional guard (see Conditions below) |
| Rule type | SET (data consequence) or VALIDATE |
| Active | Enable or disable without deleting |
| Sequence | Controls execution order when multiple rules exist on the same field |

Below each rule you define its **Actions** (what to do) and optionally **Parameters** (named values to simplify your expressions).

---

## Scoping a rule

Rules can be scoped at two levels and you can combine them.

**Field-level scope** (UI-specific): attach a rule to a particular field in a specific Window + Tab. The rule only fires when a user interacts with that field in that window. Use this when the logic is UI-specific or when the same column behaves differently in different windows.

**Column-level scope** (model-wide): attach a rule to a column on a table. The rule fires whenever any record on that table is saved, regardless of which window was used — or even if no window was involved. Use this for data integrity rules that must hold universally.

If you want a rule to do both — show responsive feedback in the UI *and* enforce the consequence on save — set the Trigger to "Both" and the Execution Scope to "Both". The engine avoids double-applying the same value when the UI path already set it.

---

## Conditions

The Condition field is an optional guard that must be true for the rule to execute. Two formats are accepted.

### Context expression

Uses iDempiere's standard variable syntax. Variables in `@Brackets@` are resolved from the current record and session context.

```
@IsSoTrx@=Y
@GrandTotal@ > 100
@IsSoTrx@=Y & @GrandTotal@ > 100
@C_BPartner_ID@ > 0 | @IsAnonymous@=Y
```

Operators: `&` means AND, `|` means OR. Each clause compares a `@Variable@` to a value.

### SQL WHERE clause

Starts with `@SQL=` followed by a SQL fragment. The fragment is evaluated as a condition — if it returns any row (or evaluates to true), the rule proceeds.

```
@SQL=EXISTS (
    SELECT 1 FROM C_BPartner bp
    WHERE  bp.C_BPartner_ID = @C_BPartner_ID@
      AND  bp.IsCustomer = 'Y'
)
```

```
@SQL=@GrandTotal@ > (
    SELECT CreditLimit
    FROM   C_BPartner
    WHERE  C_BPartner_ID = @C_BPartner_ID@
)
```

If the Condition is left blank, the rule always fires (subject to Trigger and Active).

---

## Actions

Each rule has one or more actions, executed in sequence order. An action either sets a value on a field or validates a condition.

### Action types

**SET** — always writes the computed value to the target column, overwriting whatever is there.

**SET IF BLANK** — writes the computed value only if the target column is currently empty. Useful for defaults that should not override intentional entries.

**CLEAR** — sets the target column to null. No expression needed.

**VALIDATE** — evaluates an expression; if the result is not true, shows a message to the user. Does not set any value.

### Value expressions

The expression that produces the new value can be written in two ways.

**SQL scalar subquery** — any expression starting with `SELECT`. Must return a single value (one row, one column).

```sql
SELECT p.PriceStd
FROM   M_ProductPrice p
WHERE  p.M_Product_ID           = @M_Product_ID@
  AND  p.M_PriceList_Version_ID = @M_PriceList_Version_ID@
  AND  p.AD_Client_ID           IN (0, @#AD_Client_ID@)
FETCH FIRST 1 ROW ONLY
```

**Arithmetic / inline expression** — for simple calculations without a full query.

```
@QtyOrdered@ * @PriceActual@
```

```
CASE WHEN @IsSOTrx@ = 'Y' THEN @PriceList@ ELSE @PriceStd@ END
```

### Variable substitution in expressions

Use `@ColumnName@` to reference any value from the current record. The engine resolves these before executing the SQL.

| Syntax | Resolves to |
|---|---|
| `@ColumnName@` | Current value of that column in the record |
| `@#Variable@` | Global system context (e.g. `@#AD_Client_ID@`) |
| `@$Variable@` | Window-level context |

If a variable cannot be resolved, it is substituted with `NULL` and a warning is logged. The rule continues rather than failing hard.

---

## Examples

### Example 1 — Default Payment Term from Business Partner

When the Business Partner is changed on an order, fill in the Payment Term automatically.

| Field | Value |
|---|---|
| Field | Business Partner (on Sales Order header) |
| Trigger | On field change |
| Scope | UI only |

Action:

| Field | Value |
|---|---|
| Type | SET IF BLANK |
| Target | `C_PaymentTerm_ID` |
| Expression | `SELECT C_PaymentTerm_ID FROM C_BPartner WHERE C_BPartner_ID = @C_BPartner_ID@` |

---

### Example 2 — Recalculate Line Net Amount

Whenever Qty Ordered or Price Actual changes on an order line, recompute the net amount. Because this also fires on save, the value stays correct even when lines are created by an import or a process.

| Field | Value |
|---|---|
| Column | `QtyOrdered` (on C_OrderLine) |
| Trigger | Both |
| Scope | Both |

Action:

| Field | Value |
|---|---|
| Type | SET |
| Target | `LineNetAmt` |
| Expression | `@QtyOrdered@ * @PriceActual@` |

Create a second identical rule attached to `PriceActual`.

---

### Example 3 — Default Warehouse from Org

When the Organisation is set on a new record, fill the Warehouse from that org's default — but only if Warehouse is currently blank.

| Field | Value |
|---|---|
| Field | AD_Org_ID (on the relevant window) |
| Trigger | On field change |
| Scope | UI only |

Action:

| Field | Value |
|---|---|
| Type | SET IF BLANK |
| Target | `M_Warehouse_ID` |
| Expression | `SELECT M_Warehouse_ID FROM AD_OrgInfo WHERE AD_Org_ID = @AD_Org_ID@` |

---

### Example 4 — Warn if order exceeds credit limit

Before saving an invoice, warn if the total exceeds the Business Partner's credit limit. This is a save-time validation that does not block the user (Warning level), it just requires acknowledgement.

| Field | Value |
|---|---|
| Column | `GrandTotal` (on C_Invoice) |
| Trigger | On save |
| Scope | Model |
| Rule Type | VALIDATE |
| Error Level | Warning |

Action:

| Field | Value |
|---|---|
| Type | VALIDATE |
| Expression | `SELECT CASE WHEN @GrandTotal@ <= CreditLimit THEN 'Y' ELSE 'N' END FROM C_BPartner WHERE C_BPartner_ID = @C_BPartner_ID@` |
| Message | Invoice amount exceeds the credit limit for this Business Partner. Please review before proceeding. |

---

### Example 5 — Conditional rule using a Condition

Copy the Bill-To address from the Business Partner, but only on Sales Orders (not Purchase Orders).

| Field | Value |
|---|---|
| Column | `C_BPartner_ID` |
| Trigger | On field change |
| Scope | UI only |
| Condition | `@IsSOTrx@=Y` |

Action: SET `BillTo_ID` from a lookup on the Business Partner.

---

## Multiple rules on the same field

You can define several rules on the same field or column. They execute in Sequence Number order. Each rule sees the values already written by the previous ones, so a later rule can depend on what an earlier rule set.

If a rule has **Stop on Error** enabled and produces a blocking validation error, execution stops and the remaining rules are skipped.

System-level rules (configured in the System tenant) always execute before tenant-level rules. Tenant rules can build on top of or further refine the results of system rules.

---

## Validation rules in detail

When Rule Type is VALIDATE, the action expression must evaluate to `'Y'` (case-insensitive) for the validation to pass. Any other result (including `NULL`) is treated as a failure.

**Error level** controls what happens on failure:

- **Error** — blocks the save. The user must correct the value before the record can be saved.
- **Warning** — allows the save to proceed, but shows a message the user must acknowledge.

Validation rules work at both the UI (immediate feedback on field change) and model level (enforced on save regardless of channel).

---

## What rules cannot do

Rules are intentionally limited to keep them safe for implementer-level configuration.

- Expressions are read-only. `INSERT`, `UPDATE`, `DELETE`, `DROP`, and similar statements are rejected.
- Rules always run in the context of the current tenant. Cross-tenant data access is not possible.
- SQL expressions are structural checks only during configuration — they are not executed against real data until a record is actually being edited or saved.
- Rules cannot invoke processes, send notifications, or trigger document actions. Those use cases require conventional plugin development.

---

## Deploying rules across environments

Because rules are stored as Application Dictionary records, they are fully portable using iDempiere's standard **2Pack** (Package In / Package Out) mechanism. The recommended workflow for moving rules from development to production is:

1. Configure and test rules in the development environment.
2. Export as a 2Pack XML file.
3. Import in test, then production.

No server restart is required when a new rule is saved. The system registers it immediately.

