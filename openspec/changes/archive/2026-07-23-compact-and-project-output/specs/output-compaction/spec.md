## ADDED Requirements

### Requirement: 默认序列化输出紧凑形态

默认(无任何 flag)序列化 `IrDocument` 时,系统 SHALL 产出紧凑形态:单行 JSON;所有值为 `null` 的字段 SHALL 被省略(`NON_NULL` 包含);类型为 `blank` 的单元格 SHALL 只输出 `ref` 与 `style` 两字段;当单元格 `formatted` 与 `raw` 相等时 SHALL 省略 `formatted`;当 `formula` 为 `null` 时 SHALL 省略 `formula`;列宽 SHALL 只输出 `widthPx`,省略可推导的 `widthChars`;当 `headerFooter` 的奇/偶/首/尾页眉页脚字段全部为 null 时 SHALL 省略整个 `headerFooter` 块。

#### Scenario: 简单表默认输出体积显著缩减

- **WHEN** 对一张 8 行 15 列的简单薪酬表执行 `java -jar excel-ir.jar book.xlsx`(无 flag)
- **THEN** stdout 输出为单行 JSON
- **AND** 输出中不出现任何 `:null` 字面量
- **AND** 每个 blank 单元格对象仅含 `ref` 与 `style` 两个键
- **AND** 对于 `formatted` 等于 `raw` 的单元格,该单元格对象不含 `formatted` 键

#### Scenario: 格式化数值保留 formatted

- **WHEN** 某 numeric 单元格 `raw` 为数值 `12345.5`、`formatted` 为 `"12,345.50"`(二者不等)
- **THEN** 序列化该单元格时 SHALL 同时输出 `raw`(数值)与 `formatted`(字符串)
- **AND** `formatted` 键不被省略

### Requirement: 紧凑形态无信息损失

默认紧凑形态 SHALL 与完整 IR 在语义上等价:仅省略可推导或冗余的字段(blank 的空字段、`null` 值、`formatted==raw` 的重复、`widthChars`),不省略任何承载独立信息的字段。`--full-cells` + `--pretty` 组合 SHALL 能恢复除逐字节 null 写出之外的全部旧字段形态(详见「逃逸阀」)。

#### Scenario: 紧凑输出可重建等价 IR

- **WHEN** 用默认紧凑形态序列化一个 IrDocument 再解析回对象
- **THEN** 非 null 字段、非冗余字段与完整 IR 一致
- **AND** blank 单元格的 `style` 引用保留
- **AND** 没有任何单元格的独立信息(值、类型、公式、样式引用)丢失

### Requirement: 逃逸阀开关

系统 SHALL 提供两个正交的序列化层开关:`--pretty` 恢复缩进输出;`--full-cells` 恢复旧单元格形态(blank 单元格输出完整 `{ref,type,raw,formula,formatted,style}` 对象,非 blank 单元格输出 `formula` 含 null、`formatted` 含 `==raw` 的冗余值)。两开关正交,可单独或组合使用。系统 SHALL NOT 提供逐字节复现旧默认(含显式 null 字面量)的开关。

#### Scenario: --full-cells 恢复冗余字段

- **WHEN** 执行 `java -jar excel-ir.jar book.xlsx --full-cells`
- **THEN** 类型为 `blank` 的单元格输出完整对象,含 `type`、`raw`(null)、`formula`(null)、`formatted`("")
- **AND** `formatted==raw` 的字符串单元格仍输出 `formatted` 键

#### Scenario: --pretty 恢复缩进

- **WHEN** 执行 `java -jar excel-ir.jar book.xlsx --pretty`
- **THEN** 输出为缩进的 pretty JSON
- **AND** `NON_NULL` 仍生效(null 字段仍被省略)

#### Scenario: 两开关组合

- **WHEN** 执行 `java -jar excel-ir.jar book.xlsx --full-cells --pretty`
- **THEN** 输出同时具备缩进与完整单元格形态
- **AND** null 字段仍被省略(不逐字节复现旧默认)
