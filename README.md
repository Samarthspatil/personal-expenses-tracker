# SpendWise — Personal Expense Tracker

SpendWise is a beginner-friendly offline Java Swing application for manually
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
    storage/     repository interfaces and properties-file repositories
    ui/          Swing frame, panels, dialog, table model, UI helpers
  checks/LogicCheck.java
```

## Features and usage

1. Start the application and use the left navigation for Dashboard, Expenses,
   and Budget.
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

## Data files

Data is stored under the documented folder:

```text
%USERPROFILE%\SpendWiseData\
  expenses.properties
  budgets.properties
```

The files are UTF-8 Java properties files containing a `count` and indexed
records. Expense records preserve UUID, amount, category, ISO date, and note.
Budget records preserve ISO `YearMonth` and amount. Missing files mean empty
data. Loaded records are validated. Malformed files show an error and are not
overwritten. Saves write a temporary file and then replace the target file
atomically where the operating system supports it.

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

The original working version already used `BigDecimal`, `LocalDate`,
`YearMonth`, `UUID`, streams, and `EnumMap`. They remain in place to preserve
existing saved data and avoid a risky format migration. In particular, amounts
remain decimal rupees in the existing `expenses.properties` format rather than
being silently converted to paise. New enhancement code uses straightforward
loops where practical. A future integer-paise migration would need an explicit
versioned migration tool.

## Feature-to-Java-syllabus mapping

| Application feature | Java concept |
|---|---|
| `Expense`, `Budget`, `Category` | classes, constructors, encapsulation, enum |
| `ExpenseRepository`, `BudgetRepository` | interfaces and overriding |
| `ExpenseService`, `BudgetService` | methods and method overloading |
| `ArrayList`, `EnumMap`, loops and existing streams | collections and calculations |
| `BigDecimal`, `LocalDate`, `YearMonth`, `UUID` | standard library value types |
| Swing panels and `MainFrame` | inheritance through `JPanel`/`JFrame` |
| button/filter listeners | AWT event handling |
| `CardLayout`, `BorderLayout`, `GridBagLayout` | layout managers |
| `SwingWorker` | background file work and GUI safety |
| properties repositories | file I/O, readers/writers, try-with-resources |
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

## Beginner-friendly class summary

- `Main` starts Swing on the Event Dispatch Thread.
- `MainFrame` owns the current lists, navigation, background file operations,
  and refresh coordination.
- `Expense` and `Budget` are the data objects; `Category` is the allowed list.
- `ValidationService` parses and validates user input.
- `ExpenseService` and `BudgetService` perform reusable calculations.
- Repository interfaces describe persistence; file repositories implement the
  UTF-8 properties format.
- `ExpensesPanel`, `BudgetPanel`, and `DashboardPanel` are separate screens.
- `ExpenseDialog` handles add/edit input, and `ExpenseTableModel` supplies
  typed JTable values for chronological and numeric sorting.

## Limitations

This version intentionally has no login, bank connection, income tracking, AI,
recurring expenses, database, or JavaFX. It is an offline desktop assignment
application using local files.
