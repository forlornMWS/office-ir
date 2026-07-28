# style-coverage

## Purpose

定义样式表的字段覆盖完整性:`shrinkToFit`、对角线边框、填充底色 `bgColor`;以及 schema 版本升级标记。

## Requirements

### Requirement: 单元格样式补 shrinkToFit

`CellXfIr`(cellXfs 表项)SHALL 新增 `shrinkToFit` 布尔字段,取自 `CellStyle.getShrinkToFit()`。该字段 SHALL 与现有 `wrapText`/`hAlign`/`vAlign` 等对齐格式字段并列输出。

#### Scenario: 缩小填充被提取

- **WHEN** 某 cell style 设置了「缩小到填充」(shrink to fit)
- **THEN** 对应 cellXf 输出含 `"shrinkToFit": true`
- **AND** 未设置缩小的 cellXf 输出 `"shrinkToFit": false`

### Requirement: 边框补对角线

`BorderIr` SHALL 新增 `diagonalLeft` 与 `diagonalRight` 两条边,各为与现有四边同形的 `Edge(style,color)`(无边时为 null)。取自 `XSSFCellBorder` 的对角线边样式与颜色。

#### Scenario: 对角线边框被提取

- **WHEN** 某 border 设置了对角线(down)边为 thin
- **THEN** 对应 border 输出含 `"diagonalRight": {"style":"thin","color":...}`
- **AND** 未设对角线的 border 该两字段为 null(默认序列化时省略)

### Requirement: 填充补底色 bgColor

`FillIr` SHALL 新增 `bgColor` 字段(非实心填充的底色/背景色,ARGB hex),与现有 `fgColor` 并列。取自 `XSSFCellFill` 的背景色。实心填充(solid)的可见色仍由 `fgColor` 承载,`bgColor` 反映花纹底色。

#### Scenario: 花纹填充底色被提取

- **WHEN** 某 fill 为 `darkgray` 花纹,有底色
- **THEN** 对应 fill 输出含 `pattern`、`fgColor`、`bgColor` 三字段
- **AND** 无底色时 `bgColor` 为 null(默认序列化时省略)

### Requirement: schema 版本升至 2

`IrDocument.schemaVersion` SHALL 为 `2`,标记本版本引入的格式覆盖扩展(shrinkToFit、对角线边框、填充底色)与默认紧凑序列化形态。新增字段对旧消费者为 additive 兼容(旧消费者忽略未知字段)。

#### Scenario: 输出携带 schemaVersion 2

- **WHEN** 对任意 .xlsx 执行 `java -jar excel-ir.jar book.xlsx`
- **THEN** 输出 JSON 顶层 `"schemaVersion": 2`
