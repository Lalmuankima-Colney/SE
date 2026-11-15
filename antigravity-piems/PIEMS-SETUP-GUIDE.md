# PIEMS Antigravity setup

Project-specific version of the general template, filled in from your project description, SRS, decision log, test plan, risk register, coverage matrix and iteration log. Your own `docs/` files are not included in this package, so unzipping cannot overwrite them.

## 1. Contents

```
antigravity-piems/
├── GEMINI.md                      PIEMS identity, commands, layout, sources of truth (loaded every prompt)
├── .antigravityignore
├── ANTIGRAVITY_PROMPTS.md         Prompt library by stage (keep outside the project)
├── PIEMS-SETUP-GUIDE.md           This file
└── .agents/
    ├── rules/
    │   ├── 00-operating-principles.md    always_on: analyse first, evidence, safety
    │   ├── 10-evidence-and-reporting.md
    │   ├── 20-testing-standards.md
    │   ├── 30-docs-consistency.md
    │   └── 40-project-conventions.md     PIEMS rules from D-05 to D-14, D-20 (filled in)
    └── skills/
        PIEMS domain:   piems-money-accounts, piems-budgets-goals-recurring,
                        piems-persistence, piems-reports-csv, piems-doc-sync
        PIEMS process:  srs-inspection, sprint-review, viva-prep
        General:        init-project, audit-project, risk-analysis, find-errors, doc-code-check,
                        security-review, propose-fixes, test-plan, whitebox-tests, blackbox-tests,
                        system-tests, full-test-campaign, verify, new-feature, fix-bug,
                        implement-approved, sync-docs, reporting-standards
```

## 2. Install

1. Copy only `GEMINI.md`, `.antigravityignore` and `.agents/` into the project root (the folder with `pom.xml`, or the repository root while there is no `pom.xml` yet). Keep the two `.md` guides elsewhere.
2. Open that folder in Antigravity. Check Customizations: Rules shows five rules (one Always On), Skills shows the skills above.
3. Run prompts 0.1 to 0.3 in `ANTIGRAVITY_PROMPTS.md`.
4. Keep the setup out of the submission: add to `.git/info/exclude`:
   ```
   GEMINI.md
   .agents/
   .antigravityignore
   audit/
   ```
   or commit them and mark them `export-ignore` in `.gitattributes`.

If Customizations shows nothing, rename `.agents/` to `.agent/` (the older folder name, still supported). Rule files need a valid `trigger:` header or Antigravity silently ignores them. Workflows are being retired, so everything here is a skill. In the App and CLI skills appear as `/skill-name`; if that does not work in your IDE, write "use the risk-analysis skill on ..." in the prompt.

## 3. Documentation stage first

The docs describe the project but there may be no code yet. `GEMINI.md` tells the agent to treat a missing `pom.xml` or `src/` as the documentation stage, so audits compare documents with each other and skip build and test phases. Start with section 1 of the prompt library: document consistency, SRS inspection, the open design questions, plan risk analysis and test-plan review. That is the week 2 work in your schedule.

## 4. Which skill for which week

| Week | Work | Skills and prompts |
|---|---|---|
| 1 | Requirements, risk spike, Money | `/srs-inspection`, `/risk-analysis`, `piems-money-accounts`, prompts 1.x, 2.1 to 2.3 |
| 2 | Design, skeleton | `/doc-code-check`, prompts 2.4 to 2.6 |
| 3 to 4 | Core | `/new-feature` with the domain skills, prompts 3.x |
| 5 | Budgets, recurring, goals | prompts 4.x, then `/audit-project` on Must items |
| 6 | Reports, CSV, GUI | prompts 5.x |
| 7 | QA campaign | `/full-test-campaign`, `/whitebox-tests`, `/blackbox-tests`, `/system-tests`, prompts 6.x |
| 8 | Finish | `/sprint-review`, `/sync-docs`, `/viva-prep`, prompts 7.x |

## 5. Questions the domain skills make the agent ask

The agent is told to stop and ask instead of guessing when the docs leave something open: bank overdraft rule, whether `Money` can be negative, default currency, stored or derived balance, what counts as a new budget "crossing", month-end handling for recurring rules, savings goal states, and the CSV behaviour for transfers and unknown accounts. Decide these early and record each in `docs/DECISIONS.md`.

## 6. Issues I noticed while reading your documents

These came from reading the attached files, not from running anything. Run prompt 1.1 to confirm them and to find more.

| # | Where | Issue |
|---|---|---|
| 1 | TEST_PLAN section 4 | The cyclomatic example says `TC-FR10-02` to `TC-FR10-05` cover the four paths. The four paths are OK (`-01`), WARNING (`-02`), EXCEEDED (`-04`) and limit 0 (`-05`). `-03` tests that a second alert is not raised, not a path. |
| 2 | TEST_PLAN section 9 | No test case for FR-15 (trend), NFR-06, NFR-07 or NFR-09, although section 3 says all FR and NFR are tested. |
| 3 | SRS 3.4 versus FR-05 | A category in use is "reassigned or archived", but the `categories` table has no `archived` column. |
| 4 | D-12 versus project description 14.5 | D-12 stores a transfer as one row and rejects the debit-and-credit two-row design. Section 14.5 describes a transfer as a debit and a credit leg. |
| 5 | D-12 versus SRS 3.4 | D-12 writes the transfer "with the balance updates", but the schema has no balance column. Stored or derived balance is undecided. |
| 6 | FR-16 versus SRS 3.3 | Export must re-import without loss, but the CSV has one account column and no transaction kind, so transfers cannot round-trip. |
| 7 | FR-12 versus section 9 | Goal states Overdue and Cancelled appear in the diagram plan and test aids, but FR-12 only specifies Active to Achieved. |
| 8 | NFR-07 versus D-16 | The branch coverage target (70%) is in D-16 and the test plan, but NFR-07 states only the line target. The test plan says targets are "in the SRS". |
| 9 | Budget limit 0 | Test plan: "validation error"; black-box notes: `ValidationException`; D-13: constructors throw `IllegalArgumentException`. |

I recomputed the numbers in the plan: the hours (178 subtotal, 36 contingency, 214 total), COCOMO (about 8.94 person-months, 5.75 months) and function points (155) all match.

## 6b. What the setup cannot do

- It improves process and discipline; it does not guarantee that every bug is found. Read the "Not tested" section of each report.
- It has not been run in your Antigravity install. The folder name, header format and slash-command behaviour follow the current documentation and may differ in your version.
- Check the course rules on AI tool use. `viva-prep` is written to help you understand the code, not to produce answers to recite.

## 7. Troubleshooting

| Symptom | Fix |
|---|---|
| Rules or skills not listed | check folder name (`.agents` or `.agent`), rule headers, and that you opened the project root |
| Agent edits code during an audit | stop it and say "analysis task, rule 00 applies"; check rule 00 is Always On |
| Agent claims tests pass without running them | ask for the command and summary line |
| `./mvnw` fails | say which part of `pom.xml` is missing; the agent should report it, not invent output |
| Agent scans the whole repo | give a scope with file names and IDs |
| Long audit loses context | start a new conversation: "Resume from audit/<run>/00-STATE.md" |
