---
name: office-ir
description: Parse .xlsx/.docx into a compact LLM-friendly JSON IR via the office-ir jar. Use when the user wants to extract structure or formatting from an Excel or Word file into JSON, convert xlsx/docx to data for an LLM, inspect spreadsheet/document styles, slice a region or section of a workbook/document, or reproduce a template's layout. Covers cell values, styles, merges, columns/rows, page setup, paragraphs, runs, named styles, numbering, tables, images.
license: MIT
compatibility: Requires JDK 17+ runtime and the office-ir jar (built from the office-ir repo, or pre-packaged beside this SKILL.md).
metadata:
  author: office-ir
  version: "1.0"
---

Parse an `.xlsx` or `.docx` into a compact, LLM-friendly JSON **IR** (intermediate representation). The IR is single-line by default, omits nulls, and deduplicates styles via references — economical for an LLM to read, zero semantic loss. The jar dispatches by extension: `.xlsx` → Excel IR (`schemaVersion: 2`), `.docx` → Word IR (`schemaVersion: 1`).

## 1. Ensure the jar

Resolve the jar in this order; stop at the first that exists:

1. `$OFFICE_IR_JAR` env var.
2. `./office-ir.jar` co-located with this `SKILL.md`. **This is the normal case** for a skill installed via `npx skills add` — the jar is committed into the skill folder and ships with it.
3. `<repo>/target/office-ir.jar`, where `<repo>` is the office-ir source checkout (set `$OFFICE_IR_REPO`).
4. Otherwise: clone and build (the lean, no-jar-committed variant). If `$OFFICE_IR_REPO` is unset, `git clone https://github.com/<owner>/office-ir`, then in that checkout:

```bash
JAVA_HOME=/path/to/java17 mvn -o clean package -DskipTests   # produces target/office-ir.jar
```

## 2. Run

Always invoke with an explicit JDK 17 `java` (a JDK 8 default `java` will fail):

```bash
"/path/to/java17/bin/java" -jar <jar> <input.xlsx|input.docx> [flags]
```

- No `-o`: JSON to stdout (preferred for in-context LLM consumption).
- `-o out.json`: write to file; image pixels land in a sibling `out.media/` (JSON keeps only paths).

**Completion criterion:** a valid JSON IR object is produced — Excel IR has top-level `sheets[]`; Word IR has top-level `sections[]`. If the run exits non-zero, read stderr (exit 2 = bad flag/unsupported format; exit 1 = parse failure with stack trace) and fix the invocation, do not silently proceed.

## 3. Flags (reference)

| Flag | Excel | Word | Effect |
|---|---|---|---|
| `-o, --output <file>` | ✓ | ✓ | write to file (default stdout); images to sibling `out.media/` |
| `--pretty` | ✓ | ✓ | re-indent output (default single-line); `NON_NULL` still on |
| `--select <list>` | ✓ | ✓ | return only named sections, comma-separated |
| `--sheet <name\|idx>` | ✓ | — | parse only one sheet |
| `--range <A1:E6>` | ✓ | — | A1-native rectangle clip (cells/merges/layout/media) |
| `--full-cells` | ✓ | — | restore verbose cell shape (blank full, `formatted` even if `==raw`) |
| `--full` | — | ✓ | no run-merge, keep empty runs (Word debug escape) |

`--range` / `--sheet` / `--full-cells` on a `.docx` exit 2 ("Word 不支持 …"). `.xls` / `.doc` exit 2 with a libreoffice convert hint.

### `--select` vocabularies

- **Excel (9):** `cells merges borders fonts fills xfs layout page media`. Selecting `cells` auto-pulls the style chain (`xfs`/`fonts`/`fills`/`borders`) referenced by the kept cells; explicit `--select borders` etc. takes the full table.
- **Word (7):** `paragraphs tables styles media headers page properties`. `paragraphs` recursively penetrates table cells (unless `tables` is also selected, then top-level only to avoid duplication); selecting body blocks pulls referenced `styles` (closure).

## IR shape (reference)

- **Excel `IrDocument`:** `workbook.{properties, definedNames, styles{fonts,fills,borders,cellXfs}}` + `sheets[]{dimensions, columns, rows, mergedRegions, cells[], images/charts/shapes, freezePanes, autoFilter, pageSetup, margins, headerFooter}`. Each cell: `ref`/`type`(s/n/b/e/f/blank)/`raw`/`formula`/`formatted`/`style`; blank cells compress to `{ref,style}`.
- **Word `WordIr`:** `properties` + `styles{paragraphStyles, characterStyles}` + `sections[]{pageSetup, headers, footers, body[]}` + `media[]`. `body` is a sealed block tree: `ParagraphIr`/`TableIr` (table cells recurse); paragraphs hold `runs[]` of `TextRunIr`/`InlineImageRunIr`.

## Default compaction (reference)

Both IRs ship compact by default: single-line, `NON_NULL`, blank-cell / empty-run / empty-paragraph compression, `formatted==raw` omission (Excel), adjacent same-format run merge (Word). Lossless; `--full-cells` / `--full` restore verbose shape for debugging.
