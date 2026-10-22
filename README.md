# SpendWise — Personal Expense Tracker

SpendWise is a beginner-friendly offline Java AWT application for manually
recording expenses and monthly budgets in Indian rupees.

## Requirements and commands

- Java Development Kit 17 or newer (developed and checked with JDK 26).
- Windows PowerShell.

From the `SpendWise` folder:

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object FullName)
java -cp out spendwise.Main
```

To run the lightweight business-logic checks:

```powershell
javac -encoding UTF-8 -cp out -d out checks\LogicCheck.java
java -cp out LogicCheck
```

## Project structure

```text
SpendWise/
  src/spendwise/
    Main.java
    model/       Expense, Budget, Category
    service/     validation and calculations
    storage/     file and H2 JDBC repositories
    ui/          AWT frame, panels, dialogs, reports, and UI helpers
  web/
    src/         Jakarta Servlet monthly report
    WEB-INF/     JSP and web.xml
  checks/LogicCheck.java
```

## Features and usage

1. Start the application and use the left navigation for Dashboard, Expenses,
   Budgets, and Reports.
2. In Expenses, add an amount, category, date (`dd/MM/yyyy`), and optional
   note. Dates in the future, invalid dates, and invalid money are rejected
   without clearing the form.
3. Sort the table by clicking a column header. Use the month and category
   filters together. Add, edit, delete (with confirmation), or export the
   currently filtered rows to CSV.
4. In Budget, select a month/year, set or update a budget, and remove it with
   confirmation. The progress bar is capped visually at 100%, while text
   shows the true percentage and overspending.
5. Dashboard values are for its selected month and refresh after changes.
6. Use Reports for a text preview and save-as-text/CSV reports for a selected
   month. Use File > Backup data or File > Restore data for complete
   plain-properties backups. Restore validates everything first, confirms the
   replacement, and writes a `.before-restore` safety backup.
7. Expense dialogs now support Essential expense and Cash/UPI/Card payment
   methods. Old records default to Unspecified. Expenses also support an
   essential-only filter, minimum-amount scrollbar, double-click editing, and
   Escape-to-cancel dialogs.

## Data files

The authoritative desktop database is stored under:

```text
%USERPROFILE%\SpendWiseData\
  spendwise.mv.db
```

The H2 database uses DECIMAL amounts and stores expense UUID, category, date,
note, essential flag, payment method, and monthly budgets. The original
`expenses.properties` and `budgets.properties` files remain readable. On the
first database startup they are validated and imported; old records receive
`essential=false` and `paymentMethod=Unspecified`. The original files are not
deleted. If migration validation fails, the database is not marked migrated.

Text exports and backup files remain UTF-8 properties/CSV files.

## JDBC and web-module commands

The project includes H2 `2.5.252` and Jakarta Servlet API `6.1.0` jars in
`lib`. Compile the desktop:

```powershell
javac -encoding UTF-8 -cp "lib\h2-2.5.252.jar" -d out `
  (Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName)
java -cp "out;lib\h2-2.5.252.jar" spendwise.Main
```

Compile the servlet for Tomcat 10.1:

```powershell
javac -encoding UTF-8 -cp "out;lib\h2-2.5.252.jar;lib\jakarta.servlet-api-6.1.0.jar" `
  -d web\WEB-INF\classes (Get-ChildItem web\src -Recurse -Filter *.java | ForEach-Object FullName)
```

Deploy the `web` directory as a web application to Tomcat 10.1, and place
`h2-2.5.252.jar` in Tomcat's shared/lib directory. The servlet uses
`jakarta.servlet` consistently and reads the same H2 database. It exposes
`/monthly-report` and forwards to `WEB-INF/monthly-report.jsp`.

## New enhancement notes

- Expenses now support case-insensitive note search, combined with month and
  category filters, a clear-filters button, visible count/total, and one-level
  undo for the latest deletion.
- Navigation now includes Dashboard, Expenses, Budgets, and Reports, with a
  dark-blue sidebar, teal primary actions, red delete action, and selected
  navigation highlighting.
- Reports use the `ReportExporter` interface with separate text and CSV
  implementations.
- Backup files are UTF-8 plain properties files containing both expenses and
  budgets. Restore validates the complete file before replacing records and
  creates a safety backup beside the selected backup.

## Existing API choices

The original working version already used `BigDecimal`, `LocalDate`, `YearMonth`,
and `UUID` in its model and compatible properties files. Those established
types remain in the model/storage layer so existing user data is not silently
rewritten. In particular, amounts remain decimal rupees rather than being
silently converted to paise. The migrated UI uses only AWT; its new filtering,
dialogs, reports, and file operations use loops and named/anonymous listeners.
A future integer-paise migration would need an explicit versioned migration
tool.

## Feature-to-Java-syllabus mapping

| Application feature | Java concept |
|---|---|
| `Expense`, `Budget`, `Category` | classes, constructors, encapsulation, enum |
| `ExpenseRepository`, `BudgetRepository` | interfaces and overriding |
| JDBC repositories | `Connection`, `PreparedStatement`, `ResultSet`, transactions |
| `ExpenseService`, `BudgetService` | methods and method overloading |
| `ArrayList`, `EnumMap`, loops and existing streams | collections and calculations |
| `BigDecimal`, `LocalDate`, `YearMonth`, `UUID` | standard library value types |
| AWT panels and `MainFrame` | inheritance through `Panel`/`Frame` |
| button/filter listeners | AWT event handling |
| `CardLayout`, `BorderLayout`, `GridLayout` | layout managers |
| `Thread`, `EventQueue.invokeLater` | background file work and GUI safety |
| properties repositories and migration | file I/O, readers/writers, try-with-resources |
| AWT `Checkbox`, `CheckboxGroup`, `Scrollbar`, mouse/keyboard listeners | event handling |
| Servlet/JSP module | servlet lifecycle, request forwarding, JSP presentation |
| `ReportExporter`, text/CSV exporters | interfaces and overriding |
| backup/restore properties | validation, file handling, exception recovery |
| search and undo | string methods, collections, stable IDs |

## Manual test checklist

- Add valid expenses and confirm two-decimal currency display.
- Try blank, text, zero, negative, and three-decimal amounts.
- Try `31/02/2026`, a future date, and a valid Unicode note containing commas
  and quotes.
- Sort by date and amount, then edit and delete a selected row; verify the
  intended UUID record changes.
- Test December 2025 and January 2026 with both filters combined.
- Set no budget, set a budget, update it, remove it, and test overspending.
- Confirm dashboard totals, largest category, and five most recent rows refresh.
- Export filtered rows, inspect headers, commas, quotes, line breaks, and
  numeric amounts; test overwrite confirmation.
- Close and reopen; verify expenses, UUIDs, budgets, and Unicode notes persist.
- Back up a properties file, deliberately remove a required property, launch,
  confirm a clear load error, and verify the file was not silently replaced.
- Search by part of a note with different capitalization while a month and
  category filter are active; verify all conditions combine.
- Delete an expense, use Undo last delete, and verify the original ID and
  values return without a duplicate.
- Generate a text report and CSV report, including notes with commas, quotes,
  and line breaks.
- Create a backup, alter it to be malformed, verify restore refuses it, then
  restore a valid backup and confirm the `.before-restore` file exists.
- Verify an old properties file imports once into H2 and that old records show
  Unspecified payment method.
- Test Essential only, minimum amount threshold/reset, double-click editing,
  and Escape cancellation.

## Beginner-friendly class summary

- `Main` starts AWT on the Event Dispatch Thread.
- `MainFrame` owns the current lists, navigation, background file operations,
  and refresh coordination.
- `Expense` and `Budget` are the data objects; `Category` is the allowed list.
- `ValidationService` parses and validates user input.
- `ExpenseService` and `BudgetService` perform reusable calculations.
- Repository interfaces describe persistence; file repositories implement the
  UTF-8 properties format.
- `ExpensesPanel`, `BudgetPanel`, and `DashboardPanel` are separate screens.
- `ExpenseDialog` handles add/edit input, and `java.awt.List` displays
  readable expense summaries with a separate details area.

## Limitations

This version intentionally has no login, bank connection, income tracking, AI,
recurring expenses, database, or web UI. The interface is pure AWT and the
application uses local files.
