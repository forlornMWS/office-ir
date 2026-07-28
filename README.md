# office-ir

> **中文版**: [README-zh.md](./README-zh.md)

Parse `.xlsx` / `.docx` into a stack-agnostic, LLM-agent-friendly JSON IR (intermediate representation). **Dispatches by extension**: `.xlsx` → Excel IR, `.docx` → Word IR.

> The former name `excel-ir` has been renamed to `office-ir` (artifactId / fat-jar / Java package `io.officeir`). Excel behavior (compaction / range / projection / style override) is unchanged — only coordinates / package names changed. Callers can simply swap `excel-ir.jar` for `office-ir.jar`.

## Build

```bash
mvn clean package      # produces target/office-ir.jar (fat-jar)
```

> Building with Maven requires JDK 17+ (runtime also requires JDK 17+, see below). If your machine's default `JAVA_HOME` points to JDK 8, specify it explicitly before building: `JAVA_HOME=/path/to/java17 mvn clean package`.

## Run (requires JDK 17+ runtime)

```bash
java -jar office-ir.jar <input.xlsx|input.docx> [options]
```

> ⚠️ The jar requires a JDK 17+ runtime. If the system's default `java` is JDK 8, run it explicitly with JDK 17's java:
> ```bash
> "/path/to/java17/bin/java" -jar office-ir.jar input.docx
> ```

## Install as a skill (skills.sh)

office-ir ships as an installable agent skill on [skills.sh](https://skills.sh). Install the whole skill folder — `SKILL.md` plus the prebuilt `office-ir.jar` — with one command:

```bash
npx skills add forlornMWS/office-ir --full-depth
```

After install, the skill folder contains `SKILL.md` and the prebuilt `office-ir.jar`, so it runs immediately — **no clone / build step needed**, only a JDK 17+ runtime on the consumer machine. See [`skills/office-ir/SKILL.md`](./skills/office-ir/SKILL.md) for the skill contract and [`skills/office-ir/PUBLISHING.md`](./skills/office-ir/PUBLISHING.md) for publishing details.

## Common options

- `-o, --output <file>`: write to a file (default stdout); image pixels land in a sibling `out.media/` (the JSON keeps only relative paths)
- `--pretty`: restore indented output (default single-line compact)

## Excel (`.xlsx`)

Parse coverage: cell values (raw value / type / formula / formatted display string), normalized style table (font / font size / fill / border / alignment / number format / shrink-to-fit), row height, column width, merged cells, freeze panes, auto filter, page layout, margins, print area, print titles, header/footer, defined names, document properties; images / charts / shapes record metadata only (on-disk + reference).

### Options

| Option | Description |
|---|---|
| `-o, --output <file>` | write to a file (default stdout); image pixels land in `out.media/` sibling to `out.json` (the JSON keeps only relative-path references) |
| `--sheet <name\|index>` | parse only the specified sheet (by name or 0-based index); omit to parse all |
| `--range <A1:E6>` | A1-native rectangle clip. Only clips sections that carry spatial coordinates (cells/merges/layout/media); pure global style sections (borders/fonts/fills/xfs/page) are unaffected. Out-of-range is silently clipped to actual size without error; illegal format exits with code 2 |
| `--select <section>` | return only the named sections, comma-separated. Omit to return all. Legal values in the table below |
| `--pretty` | restore indented output (default single-line compact) |
| `--full-cells` | restore the legacy verbose cell shape (blank cells include all fields; `formatted` is emitted even when equal to `raw`) |

**The default output is the compact form** (single-line + omit `null` fields + blank cells compressed to `{ref,style}` + `formatted` omitted when `==raw` + column widths omit the derivable `widthChars` + all-null header/footer blocks omitted), with zero information loss. `--pretty` / `--full-cells` are debug escape valves, and the two are orthogonal.

### `--select` section vocabulary

| Section | Content |
|---|---|
| `cells` | cells (with style references) |
| `merges` | merged cell ranges |
| `borders` / `fonts` / `fills` / `xfs` | corresponding global style table slices (no spatial ownership, not clipped by range) |
| `layout` | dimensions + columns + rows + freezePanes + autoFilter |
| `page` | pageSetup + margins + headerFooter |
| `media` | images + charts + shapes |

- **Closure**: selecting `cells` automatically pulls in the `xfs` referenced by the kept cells, plus the `fonts`/`fills`/`borders` subsets referenced by those xfs, so the output is self-consistent and resolvable. Explicitly `--select`-ing a style subtable takes the full table.
- **Order of application**: projection (select category) → range (clip space) → closure (back-fill reference chain). `--range` and `--select` combine as an orthogonal conjunction.

### Examples

```bash
# Default compact output (recommended for LLMs)
java -jar office-ir.jar book.xlsx

# Inspect only the payroll data region A5:O8
java -jar office-ir.jar book.xlsx --range A5:O8

# Inspect only border definitions + which cells have borders
java -jar office-ir.jar book.xlsx --select cells,borders

# Inspect merge ranges in a region (no cells/styles)
java -jar office-ir.jar book.xlsx --range A1:E6 --select merges

# Debug: indented + full cell shape
java -jar office-ir.jar book.xlsx --pretty --full-cells
```

## Word (`.docx`)

Parse coverage: document properties, sections (single section; multi-section / header-footer content / page layout simplified in v1, deferred to v2), body block flow (paragraphs / tables; table cells recursively nest block flows), runs (text + inline images; D3 merges adjacent same-format runs), numbering (numId/ilvl; kind not parsed in v1), inline images written to disk. Default compact form: NON_NULL + run merge + empty-run drop + empty-paragraph compression to `{style, numbering?}`.

> v1 best-effort / simplified (deferred to v2): multi-section page layout, header/footer content, table gridSpan/vMerge/shading, named style table definitions (paragraphs / runs still carry style ref + direct overrides), numbering kind, anchored images, theme color parsing.

### Options

| Option | Description |
|---|---|
| `--select <section>` | return only the named sections, comma-separated |
| `--full` | debug escape: no run merge + keep empty runs + full paragraph shape |

`--range` / `--sheet` / `--full-cells` exit with code 2 on Word (Word has no grid / sheet).

### `--select` section vocabulary (Word)

| Section | Content |
|---|---|
| `paragraphs` | paragraphs (selecting this penetrates table cells to fetch all paragraphs; if `tables` is also selected, only top-level paragraphs are taken to avoid duplication) |
| `tables` | tables (whole table including nested paragraphs) |
| `styles` | named style table (empty in v1, deferred to v2) |
| `media` | anchored images (empty in v1) |
| `headers` | per-section headers/footers (simplified in v1) |
| `page` | per-section pageSetup/margins (simplified in v1) |
| `properties` | document properties |

### Examples

```bash
# Parse a Word document
java -jar office-ir.jar book.docx

# Take only body paragraphs (including those inside tables)
java -jar office-ir.jar book.docx --select paragraphs

# Debug: do not merge runs
java -jar office-ir.jar book.docx --full --pretty
```

## Format restrictions

`.xls` / `.doc` inputs error out (exit 2) with a hint to convert to `.xlsx` / `.docx` via libreoffice:

```bash
libreoffice --headless --convert-to xlsx book.xls
libreoffice --headless --convert-to docx book.doc
```

## IR field overview

- `schemaVersion`: IR version (currently **2**)
- `workbook.properties`: document properties (creator / created etc.; `null` fields omitted)
- `workbook.styles`: normalized style table (`fonts` / `fills` / `borders` / `cellXfs`); each cell references `cellXfs[n]` via `style:"S{n}"`; a `cellXf` in turn references `fonts` / `fills` / `borders` entries
  - `cellXf` fields: `font`/`fill`/`border` references + `hAlign`/`vAlign`/`wrapText`/`shrinkToFit`/`textRotation`/`indent`/`numberFormat`/`locked`/`hidden`
  - `border` fields: `top`/`bottom`/`left`/`right` + `diagonalLeft` (corresponds to OOXML diagonalDown, \) + `diagonalRight` (corresponds to diagonalUp, /)
  - `fill` fields: `pattern` + `fgColor` (solid-fill visible color) + `bgColor` (pattern background color)
- `workbook.definedNames`: defined names (including `_xlnm.Print_Titles` / `_xlnm.Print_Area` etc.)
- `sheets[]`: each sheet contains
  - `dimensions` / `columns` (column width `widthPx`) / `rows` (row height `heightPt`) / `mergedRegions`
  - `cells`: each cell has `ref` / `type` (s/n/b/e/f/blank) / `raw` (when a cached value exists) / `formula` / `formatted` (when different from `raw`) / `style`. Blank cells compress to `{ref,style}`
  - `images` (path + anchor + size + format) / `charts` (type + anchor + title) / `shapes` (anchor + text + style)
  - `freezePanes` / `autoFilter`
  - `pageSetup` (paper / orientation / fitToPage / scale / gridlines / titles / print area / repeating rows/cols)
  - `margins` (inches) / `headerFooter` (odd/even/first page header/footer; omitted entirely if all null)

## Unit conventions

Column width `widthPx`, row height `heightPt`, font size `sizePt`, page margins `*In` (inches), image anchor offsets `*Px`. Values are normalized to human-readable units.
