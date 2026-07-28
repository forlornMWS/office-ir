## ADDED Requirements

### Requirement: 区段投影严格语义

`--select <逗号分隔区段>` SHALL 严格只返回命名的区段;`cells` SHALL NOT 作为隐式常开区段。省略 `--select` 时 SHALL 返回全部区段(经默认压缩与可能的区间裁剪)。合法区段词表为固定 9 个:`cells merges borders fonts fills xfs layout page media`,其中 `layout` 聚合 dimensions+columns+rows,`page` 聚合 pageSetup+margins+headerFooter,`media` 聚合 images+charts+shapes。含未知词时 SHALL 以退出码 2 退出并打印合法词表清单。

#### Scenario: 只选部分区段

- **WHEN** 执行 `--select merges,borders`
- **THEN** 输出仅含 merges 与 borders,不含 cells、fonts、layout 等其它区段

#### Scenario: 省略 select 返回全部

- **WHEN** 执行 `java -jar excel-ir.jar book.xlsx`(无 `--select`)
- **THEN** 输出含全部 9 个区段对应的结构

#### Scenario: 未知区段词报错

- **WHEN** 执行 `--select cellz`
- **THEN** 退出码为 2
- **AND** stderr 打印合法词表清单(cells merges borders fonts fills xfs layout page media)

### Requirement: cells 触发样式链闭包

当选中 `cells` 时,系统 SHALL 自动带出这些 cells 引用的 `xfs`(cellXfs),以及那些 xfs 引用的 `fonts`/`fills`/`borders` 子表,保证每个 `cell.style` 都可解析到带出的样式子表。闭包 SHALL 基于经 `--range` 裁剪后的 cells 集合(区间内 cells 引用到的样式子集),而非全表 cells 引用集。显式 `--select` 某样式子表时 SHALL 取整张该子表(因样式子表无空间归属,区间不裁)。

#### Scenario: 选 cells 自动带样式链

- **WHEN** 执行 `--select cells`(无 `--range`),区间内 cells 引用样式 S4(其 border=B4、font=F4)
- **THEN** 输出含 cells、xfs(至少含 S4)、borders(至少含 B4)、fonts(至少含 F4)、fills(S4 引用的填充)

#### Scenario: 闭包基于区间内 cells

- **WHEN** 执行 `--range A5:O8 --select cells`,全表有 50 种边框但 A5:O8 仅用到 2 种
- **THEN** 输出的 borders 表仅含那 2 种被区间内 cells 引用的边框
- **AND** 不含区间内未引用的边框

#### Scenario: 显式选样式子表取全表

- **WHEN** 执行 `--select borders`(无 `--range`)
- **THEN** 输出含整张 borders 表(全部边框定义),不受任何区间裁剪

### Requirement: 投影与区间正交合取

`--select` 与 `--range` SHALL 正交合取:`--range` 仅裁剪有空间坐标的区段(cells/merges/layout/media);`--select` 仅决定类别去留。同时提供时取交集——被 `--select` 排除的区段不出现(无论是否在区间内),被 `--select` 选中的有空间区段再经区间裁剪。叠加顺序 SHALL 固定为 投影(选类别)→ 区间(裁空间)→ 闭包(补引用链)。

#### Scenario: 同时给 select 与 range

- **WHEN** 执行 `--range A1:E6 --select cells,borders`
- **THEN** 输出含 cells(裁到 A1:E6)与 borders(全表,区间不裁)
- **AND** cells 的 style 闭包带出区间内引用的 xfs/fonts/fills/borders 子集
- **AND** merges、layout、page、media 等未选区段不出现

#### Scenario: 区间不选 cells 时闭包不触发

- **WHEN** 执行 `--range A1:E6 --select merges`
- **THEN** 输出含 merges(裁到与区间相交)
- **AND** 不含 cells,因此不触发样式链闭包
