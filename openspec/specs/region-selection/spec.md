# region-selection

## Purpose

定义 `--range A1:E6` 空间裁剪语义:A1 原生坐标、与 sheet 的关系、对有空间坐标区段的裁剪、media 判交、越界宽容、错误处理。

## Requirements

### Requirement: 区域选择裁剪空间维度

`--range <A1>`(A1 原生坐标,1-based 行)SHALL 对有空间坐标的区段做矩形裁剪:cells 仅保留 `ref` 落在区间内的单元格;mergedRegions 仅保留与区间矩形相交的合并区;columns/rows 仅保留 `idx` 在区间内的行/列;dimensions 收窄到区间矩形;images/charts/shapes 仅保留锚点矩形(from..to)与区间矩形相交者,保留的图实体元数据完整不切割。纯全局区段(borders/fonts/fills/xfs/page)SHALL NOT 受 `--range` 影响。

#### Scenario: 区间裁剪 cells 与 merges

- **WHEN** 执行 `--range A1:E6` 于一张含 `A1:O1`、`B3:G4` 合并及 `O5` 单元格的表
- **THEN** 输出的 cells 仅含 ref 在 A1..E6 矩形内的单元格(`O5` 被排除)
- **AND** mergedRegions 仅含与 A1:E6 相交的合并区(`A1:O1`、`B3:G4` 留,不相交的去)
- **AND** dimensions 的 firstRow/firstCol/lastRow/lastCol 收窄到区间

#### Scenario: media 按锚点判交

- **WHEN** 一张图片锚点 `from=(0,0)` `to=(2,5)` 与区间 `A1:E6`(col0..4, row0..5)相交
- **THEN** 该图片 SHALL 出现在输出中
- **AND** 图片的 `path`、`widthPx`、`heightPx`、完整锚点 from/to 均保留不切割

#### Scenario: 纯全局区段不受区间影响

- **WHEN** 执行 `--range A1:E6` 且样式表有 7 种边框
- **THEN** 输出的 borders 表 SHALL 含全部 7 种边框(不被区间裁剪)

### Requirement: 区域选择不强制 sheet

`--range` SHALL NOT 要求同时提供 `--sheet`。当未提供 `--sheet` 时,区间 SHALL 作用于所有被解析的 sheet(每个 sheet 各自裁剪到同一区间矩形);单 sheet 文件自然只命中一个 sheet。

#### Scenario: 无 sheet 时区间作用于全部 sheet

- **WHEN** 一个含 2 个 sheet 的文件执行 `--range A1:E6`(无 `--sheet`)
- **THEN** 输出中每个 sheet 的内容均被裁剪到 A1:E6 区间

#### Scenario: 区间与 sheet 组合

- **WHEN** 执行 `--sheet "员工薪酬明细表" --range A5:O8`
- **THEN** 仅输出该 sheet,且该 sheet 内容裁剪到 A5:O8

### Requirement: 区域越界宽容

当 `--range` 指定的区间超出 sheet 实际尺寸时(如 8 行表指定 `A1:E100`),系统 SHALL 静默裁剪到实际尺寸,SHALL NOT 报错或返回非零退出码。

#### Scenario: 区间超出实际行数

- **WHEN** 一张 8 行表执行 `--range A1:E100`
- **THEN** 输出的 cells/rows 仅到实际第 8 行
- **AND** 退出码为 0
- **AND** 不打印错误诊断

### Requirement: 区间格式校验

`--range` 的值 MUST 是合法的 A1 区间 `起点:终点`,起点不晚于终点(行/列均)。非法形态(无冒号、多余冒号、起点在终点之后、非单元格引用)SHALL 以退出码 2 退出,并打印具体诊断消息。

#### Scenario: 非法区间报错

- **WHEN** 执行 `--range E6:A1`(终点早于起点)
- **THEN** 退出码为 2
- **AND** stderr 打印含区间值的诊断消息

#### Scenario: 缺冒号报错

- **WHEN** 执行 `--range A1E6`
- **THEN** 退出码为 2
