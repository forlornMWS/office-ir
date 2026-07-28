## Why

excel-ir 的使命是「对 LLM agent 友好的 JSON IR」,但当前默认输出对一张 8×15 的简单表也要 ~8KB:美化缩进、空单元格全字段展开、冗余 null 字段、`formatted==raw` 重复、`widthChars` 与 `widthPx` 重复并存。消费者(LLM)拿到的是冗余而非信息。同时 CLI 只能整表输出,无法只看指定 sheet 的指定区域、无法只取边框/字体/合并等部分结构,也无法重建一些基础单元格格式(缩小填充、对角线边框、填充底色)。

## What Changes

- **BREAKING**:默认序列化改为紧凑形态——单行输出、`NON_NULL` 包含(所有 `:null` 字段省略)、空单元格压缩为 `{"ref","style"}`、`formatted==raw` 时省略 `formatted`、`formula==null` 时省略 `formula`、列宽去除冗余 `widthChars`、全 `null` 的 `headerFooter` 块省略。默认仍输出全部区段。
- 新增 `--pretty` 恢复缩进、`--full-cells` 恢复旧单元格形态(含冗余 `formatted`/`formula`/完整 blank 对象),二者正交。不再提供「连 null 都写」的逐字节复现旧默认(无消费者依赖)。
- 新增 `--range A1:E6`(A1 原生坐标):区间裁剪有空间坐标的区段(cells/merges/layout/media);纯全局区段(borders/fonts/fills/xfs/page)不受影响。区间不强制 `--sheet`,无 `--sheet` 时作用于所有被解析 sheet 各自裁同一区间。空间越界静默裁到实际尺寸不报错。
- 新增 `--select <区段>`:严格只返回命名区段,词表 9 个——`cells merges borders fonts fills xfs layout page media`(`layout`/`page`/`media` 为聚合组)。省略 `--select` = 全部区段。
- 选 `cells` 时自动做样式链闭包:带出这些 cells 引用到的 `xfs`,以及那些 xfs 引用到的 `fonts`/`fills`/`borders`,保证输出自洽可解。闭包源 = 经 `--range` 裁剪后的 cells 集合;叠加顺序固定 投影→区间→闭包。显式 `--select` 的样式子表取全表,闭包带出的是区间内引用子集。
- `--range` 格式非法 / `--select` 含未知词 → `exit 2` + 具体诊断(区段报错附合法词表);空间越界 → 静默裁剪不报错。
- 图片照常全部落盘;`--range` 裁剪只决定 `ImageIr` 进不进 JSON,不回滚磁盘文件。
- 架构:提取(含落盘)→ 纯数据投影层(range/select/闭包,输入完整 IrDocument + range + select,输出裁剪后 IrDocument)→ 序列化层(形态压缩)。两层开关正交、各自可单元测试;`--range`/`--select` 是投影层开关,`--pretty`/`--full-cells` 是序列化层开关。
- 格式覆盖扩展(与 `borders`/`fills` 投影区段咬合):`CellXfIr` 补 `shrinkToFit`;`BorderIr` 补 `diagonalLeft`/`diagonalRight`(各含 style/color);`FillIr` 补 `bgColor`(非实心填充底色)。`schemaVersion` 升到 2。

## Capabilities

### New Capabilities

- `output-compaction`:默认序列化的紧凑形态契约——单行、`NON_NULL`、空单元格压缩、冗余字段省略、全 null 块省略;`--pretty`/`--full-cells` 逃逸阀。
- `region-selection`:`--range A1:E6` 空间裁剪——作用区段、与 `--sheet` 关系、media 判交、dimensions 收窄、越界宽容、错误处理。
- `section-projection`:`--select` 区段投影——9 词词表、严格语义、`cells` 样式链闭包、闭包与区间叠加顺序、与 `--range` 正交合取。
- `style-coverage`:样式表字段覆盖——`shrinkToFit`、对角线边框、填充底色 `bgColor`;schemaVersion 升 2。

### Modified Capabilities

<!-- 无既有 spec,所有能力均为新增。 -->

## Impact

- 受影响代码:`JsonWriter`(序列化层压缩 + `--pretty`/`--full-cells` 开关)、`CliArgs`(新增 `--range`/`--select`/`--pretty`/`--full-cells` 解析)、`Main`(接线投影层)、新增投影层模块(裁剪 + 闭包,复用现有 record)、`CellXfIr`/`BorderIr`/`FillIr`/`StyleTableExtractor`(字段覆盖扩展)、`IrDocument`(schemaVersion 2)。
- 新增投影层位于提取与序列化之间,纯数据、无文件 IO;图片落盘逻辑不变。
- `--range` 区间解析复用 POI `CellReference` 做 A1↔坐标换算与矩形判交。
- 测试:`JsonWriterTest` 中 `cells[0].formatted` 断言需改为匹配新默认(或 `formatted!=raw` 场景),或改断言;新增投影层、序列化压缩形态、区间、区段、闭包、错误处理的单元测试。
- README:更新字段速览(`widthChars` 去除、新增 `shrinkToFit`/对角线/`bgColor`)、新增 `--range`/`--select`/`--pretty`/`--full-cells` 用法、schemaVersion 2 说明。
