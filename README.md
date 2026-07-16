# Field Event Rules

> This plug-in was sponsored by **[Energy Kinetics, Inc.](https://energykinetics.com/)**.

Field Event Rules let you define "when this field changes, do that" logic directly inside the iDempiere Application Dictionary — without writing Java code, installing plugins, or touching an IDE.

Think of it as a lightweight form event system: you attach rules to fields or columns, describe what should happen in plain SQL or a simple expression, and the system takes care of executing them both in the UI and when records are saved through any channel.

---

## What you can do with it

- **Auto-fill fields** — when a user picks a Business Partner, automatically populate the Payment Term, Price List, or any other field from a lookup.
- **Recalculate derived values** — when Quantity or Unit Price changes, recompute the Line Amount.
- **Copy values between fields** — when an Org is selected, default the Warehouse from that org's configuration.
- **Conditional defaults** — set a field only when it is currently blank, leaving intentional values untouched.
- **Validate data** — show an error or warning when a value breaks a business rule, either immediately when the field is changed or when the record is saved.
- **Auto-create related records** — when a new record is saved, automatically create a related record in a different table (e.g., grant default org access when a user is created).

All of these work whether the record is created through a window, a background process, a data import, or the API.

---

## How it works conceptually

A Field Event Rule sits between a field (or column) and a piece of logic you define. It has three parts:

**1. Trigger** — when does it fire?
- *On field change* (UI / callout) — fires immediately in the UI when a user leaves the field, the same moment a callout would. Does not fire on background saves.
- *On save* (model) — fires when the record is being saved, regardless of how it was created. With a Field/Column set, it fires only when that field's value actually changes; with no Field/Column, it fires on **every** save (see [Firing on every save](#firing-on-every-save-no-watch-field) below).
- *UI & Save* — fires at both moments, so the UI stays responsive and data consistency is guaranteed for non-UI operations too.
- *After New* — fires once, right after a brand-new record has been inserted and has an ID. Dedicated to [cross-table actions](#cross-table-actions-create-records-in-another-table); source-record assignments and validations are not evaluated on this trigger.

> **Field/Column is optional for *On save* and *After New*.** These are model-level triggers, so they can run without watching a specific field. For *On field change* and *UI & Save*, a Field/Column is required — the rule attaches a UI callout, which must be bound to a field.

**2. Condition** *(optional)* — should it fire this time?
An optional guard expression. If it evaluates to false, the rule is skipped entirely. Useful for rules that only apply in certain situations (e.g. only on Sales Orders, only when the amount exceeds a threshold).

**3. Actions** — what happens?
One or more ordered steps. Each action sets a field to a computed value or clears it.

Validation is not an action — it is a property of the rule itself. A rule whose Rule Type is VALIDATE shows a message instead of running actions; see [Validation rules in detail](#validation-rules-in-detail).

---

## Where to configure rules

Open the **Field Event Rules** window from the Application Dictionary menu (System tenant or your tenant, depending on your access).

Each rule record has:

| Field | What it does |
|---|---|
| Name | A label for the rule, shown in lists |
| Window / Tab / Field | Scope the rule to a specific place in the UI. Leave blank to apply model-wide. |
| Table / Column | Attach the rule at the column level so it fires for any window using that column. Column is optional for the *On save* and *After New* triggers — leave it blank to fire on every save (see [Firing on every save](#firing-on-every-save-no-watch-field)). |
| Trigger | On field change / On save / UI & Save / After New |
| Execution scope | UI only / Model only / Both |
| Condition | Optional guard (see Conditions below). For VALIDATE rules this is the validation test itself. |
| Rule type | SET (data consequence) or VALIDATE |
| Message | VALIDATE only — the message shown when the rule fires. Falls back to the rule's Name if left blank. |
| Error Level | VALIDATE only — Error (blocks) or Warning (acknowledge and continue). Defaults to Error. |
| Active | Enable or disable without deleting |
| Sequence | Controls execution order when multiple rules exist on the same field |

Below each rule you define its **Actions** (what to do) and optionally **Parameters** (named values to simplify your expressions).

---

## Scoping a rule

Rules can be scoped at two levels and you can combine them.

**Field-level scope** (UI-specific): attach a rule to a particular field in a specific Window + Tab. The rule only fires when a user interacts with that field in that window. Use this when the logic is UI-specific or when the same column behaves differently in different windows.

**Column-level scope** (model-wide): attach a rule to a column on a table. The rule fires whenever any record on that table is saved, regardless of which window was used — or even if no window was involved. Use this for data integrity rules that must hold universally.

If you want a rule to do both — show responsive feedback in the UI *and* enforce the consequence on save — set the Trigger to "UI & Save" and the Execution Scope to "Both". The engine avoids double-applying the same value when the UI path already set it.

> **Window field left blank** — if you leave the Window (and Tab / Field) blank, the rule applies to every window that uses the configured table/column. Use this when the logic should be universal rather than window-specific.

### Firing on every save (no watch field)

Column-level rules normally fire when the watched column changes. Sometimes you want a rule that runs on **every** save of a table, no matter which fields were touched — for example a validation that must always hold, or a data consequence that should always be recomputed.

To do this, set the **Table** but leave **Column** (and Field) blank, and choose a model-level trigger:

- **On save** with no Column → the rule runs on every insert *and* every update of the table.
- **After New** with no Column → the rule runs on every insert (for [cross-table actions](#cross-table-actions-create-records-in-another-table)).

This works for both **SET** and **VALIDATE** rule types. Because there is no field to bind a UI callout to, a blank Column is only allowed for these model-level triggers — *On field change* and *UI & Save* still require a Column, and the configuration screen will block saving a rule that leaves it empty for those triggers.

> **Performance note:** an every-save rule is evaluated on every save of the table. Keep its Condition and expressions lightweight, and use the Condition to short-circuit cases the rule does not apply to.

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
    SELECT SO_CreditLimit
    FROM   C_BPartner
    WHERE  C_BPartner_ID = @C_BPartner_ID@
)
```

```
@SQL=@DocumentNo@ NOT SIMILAR TO '[A-Za-z0-9]{1,}'
```

Do **not** put quotes around a token — the engine already substitutes text values as quoted SQL literals (see [Variable substitution](#variable-substitution-in-expressions)). The example above becomes `'LS80003' NOT SIMILAR TO '[A-Za-z0-9]{1,}'`.

If the Condition is left blank, the rule always fires (subject to Trigger and Active).

---

## Actions

Each rule has one or more actions, executed in sequence order. An action either sets a value on a field or clears it.

Actions apply to SET rules only. A VALIDATE rule does not run actions — its Condition is the test and its Message is the output.

### Action types

**SET** — always writes the computed value to the target column, overwriting whatever is there.

**SET IF BLANK** — writes the computed value only if the target column is currently empty. Useful for defaults that should not override intentional entries.

**CLEAR** — sets the target column to null. No expression needed.

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

Tokens use iDempiere's standard syntax and behave exactly as they do in display logic — the same forms and the same operators work in Conditions, in value expressions, and inside `@SQL=`, on both the callout and the save path.

| Syntax | Resolves to |
|---|---|
| `@ColumnName@` | Current value of that column in the record |
| `@C_BPartner_ID.Description@` | Reference operator: a column on the referenced record, reached through the foreign key (here, the Description of the linked Business Partner). The part before the dot must be the `_ID` column |
| `@C_Location_ID:0@` | Default-value operator: the literal after the colon is used when the value is empty |
| `@#Variable@` | Global system context (e.g. `@#AD_Client_ID@`) |
| `@$Variable@` | Window-level context |

The default fires only when the value is null or empty — not when it is `0`. A number-like default is substituted as a number (`@C_Location_ID:0@` becomes `0`, not `'0'`), so on a text column use a non-numeric default.

If a variable cannot be resolved, it is substituted with `NULL` and a warning is logged. The rule continues rather than failing hard.

#### Quoting inside `@SQL=`

The engine substitutes each token as a ready-to-use SQL literal, so **you do not write the quotes yourself**:

| Token | Value | Becomes |
|---|---|---|
| `@DocumentNo@` | `LS80003` | `'LS80003'` |
| `@Description@` | `O'Brien` | `'O''Brien'` (apostrophes escaped) |
| `@GrandTotal@` | `123.45` | `123.45` (numbers unquoted) |
| `@IsSOTrx@` | checked | `'Y'` |
| `@DateOrdered@` | a date | a SQL date literal |
| `@AnyColumn@` | empty | `NULL` |

If you do wrap a token in quotes, the engine leaves your quotes alone and only escapes the value, so `Name = '@Name@'` still works. This detection needs the quotes to touch the token exactly — `'%@Name@%'` is **not** recognized and produces broken SQL. For `LIKE` patterns, concatenate instead:

```
@SQL=@Description@ LIKE '%' || @Name@ || '%'
```

A token resolved through the reference operator or a default is quoted by the same rules — `@C_BPartner_ID.Name@` becomes a quoted text literal, `@C_Location_ID:0@` an unquoted `0`.

> **Breaking change** — the reference operator now uses the `_ID` column, matching core: write `@C_BPartner_ID.Name@` where an older rule said `@C_BPartner.Name@`. The old form is rejected with an error naming the token, rather than silently resolving to `NULL`.

> **SQL validation on save** — when you save a rule configuration, the system performs a dry run to detect malformed SQL before the rule can affect real data. Fix any reported syntax errors before the rule will activate. The dry run replaces every token with `NULL`, so it catches structural errors only; a type mismatch that depends on the actual value surfaces when the rule runs, and aborts the save with the rule name in the message.

---

## Examples

### Example 1 — Fill description and check credit status from Business Partner

When the Business Partner is changed on a Sales Order, copy the partner's description into the order's Description field and write a credit status indicator into PO Reference.

| Field | Value |
|---|---|
| Window | Sales Order |
| Tab | Order |
| Field | Business Partner |
| Trigger | On field change (UI) |
| Rule Type | SET |

Action 1 — copy the partner description:

| Field | Value |
|---|---|
| Type | SET |
| Target | `Description` |
| Expression | `@C_BPartner_ID.Description@` |

Action 2 — write credit status into PO Reference:

| Field | Value |
|---|---|
| Type | SET |
| Target | `POReference` |
| Expression | `SELECT CASE WHEN @GrandTotal@ <= SO_CreditLimit THEN 'OK' ELSE 'Over the limit' END FROM C_BPartner WHERE C_BPartner_ID = @C_BPartner_ID@` |

---

### Example 2 — Validate credit limit on Business Partner change

Warn the user immediately when the order's Grand Total already exceeds the selected Business Partner's credit limit. The Condition describes the violation, so the rule fires only when there is actually a problem.

| Field | Value |
|---|---|
| Window | Sales Order |
| Tab | Order |
| Field | Business Partner |
| Trigger | On field change (UI) |
| Condition | `@SQL=(SELECT bp.SO_CreditLimit FROM C_BPartner bp WHERE bp.C_BPartner_ID = @C_BPartner_ID@) < @GrandTotal@` |
| Rule Type | VALIDATE |
| Message | Grand Total exceeds the credit limit for this Business Partner. |
| Error Level | Error |

No actions are needed. When the Condition is true — the limit is breached — the rule shows its Message.

---

### Example 3 — Auto-set Drop Ship flag based on delivery region

When the Partner Location is selected on a Sales Order, automatically enable Drop Ship if the delivery address is in New Jersey.

| Field | Value |
|---|---|
| Window | Sales Order |
| Tab | Order |
| Field | Partner Location |
| Trigger | On field change (UI) |
| Condition | `@SQL=(SELECT l.RegionName FROM C_Location l JOIN C_BPartner_Location cbl ON l.C_Location_ID = cbl.C_Location_ID WHERE cbl.C_BPartner_Location_ID = @C_BPartner_Location_ID@) = 'NJ'` |
| Rule Type | SET |

Action:

| Field | Value |
|---|---|
| Type | SET |
| Target | `IsDropShip` |
| Expression | `'Y'` |

---

### Cross-table actions (create records in another table)

When an action has a **Target Table** set, the engine creates a new record in that table instead of writing a value back to the source record. This lets you auto-create related records as a side-effect of saving.

| Field | What it does |
|---|---|
| Target Table | The table to create a new record in |
| Target Column | The column to set on the new record. When Target Table is filled, the dropdown shows columns from the target table; when empty, it shows columns from the source table (standard behavior) |
| Value Expression | The value to set on the target column. Use `@ColumnName@` to reference values from the source record |

**How it works:**

- Cross-table actions require the rule's **Trigger** to be set to **After New**. That is what causes the rule to be evaluated on `PO_AFTER_NEW` — when a brand-new record has just been inserted and has an ID. A rule with any other Trigger (On field change, On save, UI & Save) never runs its cross-table actions, even if a Target Table is configured.
- Each cross-table action sets one column on the new record. Add one action per column you want to populate.
- All actions for the same Target Table are grouped into a single `INSERT` (one new record per target table per rule evaluation).
- Only insert — updates to existing records do not trigger cross-table actions.
- If the insert fails (e.g. a constraint violation), the exception propagates and iDempiere rolls back the entire transaction — both the target insert and the source record save are undone together.

> **Note:** Target Table is optional. When left blank, the action writes to the source record as normal.

---

### Example 4 — Auto-grant org access when a new user is created

When a new `AD_User` is saved, automatically create an `AD_User_OrgAccess` record that gives the user access to the same org.

Rule:

| Field | Value |
|---|---|
| Table | AD_User |
| Column | Name |
| Trigger | After New |
| Rule Type | SET (data consequence) |

Action 1 — set the user reference:

| Field | Value |
|---|---|
| Action Type | SET |
| Target Table | AD_User_OrgAccess |
| Target Column | AD_User_ID |
| Value Expression | `@AD_User_ID@` |

Action 2 — set the org:

| Field | Value |
|---|---|
| Action Type | SET |
| Target Table | AD_User_OrgAccess |
| Target Column | AD_Org_ID |
| Value Expression | `@AD_Org_ID@` |

When a new user is saved, one `AD_User_OrgAccess` record is created automatically with the user's ID and their default org.

---

### Example 5 — Always validate on save (no watch field)

Block saving an Order whose Grand Total exceeds the Business Partner's credit limit, regardless of which field the user edited. Because there is no single "watch" field, the Column is left blank so the rule runs on every save.

| Field | Value |
|---|---|
| Table | C_Order |
| Column | *(blank — fires on every save)* |
| Trigger | On save |
| Condition | `@SQL=(SELECT bp.SO_CreditLimit FROM C_BPartner bp WHERE bp.C_BPartner_ID = @C_BPartner_ID@) < @GrandTotal@` |
| Rule Type | VALIDATE |
| Message | Grand Total exceeds the credit limit for this Business Partner. |
| Error Level | Error |

No actions are needed. The Condition is the whole test: it blocks the save whenever the limit is breached — no matter how the record reached that state.

---

## Multiple rules on the same field

You can define several rules on the same field or column. They execute in Sequence Number order. Each rule sees the values already written by the previous ones, so a later rule can depend on what an earlier rule set.

System-level rules (configured in the System tenant) and tenant-level rules are loaded together and execute in a single Sequence Number order — system rules do **not** automatically run first. A tenant rule with Sequence 10 runs before a system rule with Sequence 20. Use Sequence Number to control precedence explicitly.

---

## Validation rules in detail

When Rule Type is VALIDATE, the rule's **Condition** is the test. If the Condition evaluates to true, the rule fires and shows its Message at the configured Error Level. No expression is evaluated and no value is set — a VALIDATE rule's actions, if any, are ignored.

> **Write the Condition to describe the violation, not the valid state.** The message appears when the Condition is *true*. A rule with a blank Condition fires its message on every trigger.

The message text comes from the rule's **Message** (AD_Message). If no Message is set, the rule's **Name** is used instead.

**Error level** controls what happens when the rule fires. If left blank, it defaults to **Error**:

- **Error** — blocks the operation. On field change (UI), a popup is shown and the field is flagged. On save (model), an exception is thrown and the record cannot be saved until the condition is satisfied.
- **Warning** — allows the operation to proceed, but shows a message the user must acknowledge before continuing.

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

