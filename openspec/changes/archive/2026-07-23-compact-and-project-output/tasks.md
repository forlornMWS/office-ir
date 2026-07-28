## 1. 样式覆盖扩展(style-coverage)

- [x] 1.1 `CellXfIr` record 新增 `boolean shrinkToFit` 字段(置于 `hidden` 后)
- [x] 1.2 `BorderIr` record 新增 `diagonalLeft`、`diagonalRight` 两个 `Edge` 字段(各含 style/color,null 表示无边)
- [x] 1.3 `FillIr` record 新增 `String bgColor` 字段(非实心填充底色 ARGB hex)
- [x] 1.4 `StyleTableExtractor`:cellXf 构造补 `cs.getShrinkToFit()`;`toBorderIr` 用 `XSSFCellBorder` 取对角线边样式+颜色;`toFillIr` 取 `getFillBackgroundColor()` 的 ARGBHex
- [x] 1.5 `IrDocument` 的 `schemaVersion` 常量改为 2;`Main`/提取处同步(若有硬编码 1)
- [x] 1.6 单测:构造含 shrinkToFit / 对角线 / 花纹底色的 fixture,断言 IR 字段值

## 2. 序列化层压缩(output-compaction)

- [x] 2.1 `JsonWriter`:`ObjectMapper` 默认关闭 `INDENT_OUTPUT`,启用 `NON_NULL`(Jackson `JsonInclude.Include.NON_NULL`)
- [x] 2.2 实现自定义 `JsonSerializer<CellIr>`:`type=="blank"` 时只写 `ref`+`style`;否则按 NON_NULL 写全字段,`formula==null` 省略、`formatted` 等于 `raw` 时省略(`--full-cells` 开关旁路此 serializer 走默认全字段)
- [x] 2.3 `ColumnIr` 序列化省略 `widthChars`(仅保留 `widthPx`)——评估用 `@JsonIgnore` 或投影层处理,确保 `--full-cells` 不受影响(若需 full 恢复则条件化)
- [x] 2.4 `headerFooter` 全 null 时整体省略(`NON_NULL` 配合 record 空对象处理,确认行为或加自定义 serializer)
- [x] 2.5 支持 `--pretty`(恢复 `INDENT_OUTPUT`,NON_NULL 仍生效)与 `--full-cells`(跳过 CellIr 自定义 serializer)两个序列化选项,传入 `JsonWriter.write(doc, pretty, fullCells)` 重载
- [x] 2.6 单测:blank 压缩、`formatted==raw` 省略、数值 formatted 保留、`--pretty`、`--full-cells`、组合各一例;改 `JsonWriterTest.cells[0].formatted` 断言匹配新默认

## 3. CLI 解析扩展

- [x] 3.1 `CliArgs` record 新增 `range`(String)、`select`(String/null)、`pretty`(boolean)、`fullCells`(boolean) 字段
- [x] 3.2 解析 `--range <A1>`、`--select <逗号词>`、`--pretty`、`--full-cells`;positional 与现有 `-o`/`--sheet` 共存
- [x] 3.3 `--range` 校验:必须有冒号、起点终点可解析、起点不晚于终点(行列),否则抛 `IllegalArgumentException`(exit 2),消息含区间值
- [x] 3.4 `--select` 校验:词必须在 9 词白名单内,否则 exit 2,消息附合法词表清单
- [x] 3.5 更新 `printHelp`:列出全部 flag 与区段词表
- [x] 3.6 单测:合法/非法区间、未知区段词、各 flag 组合解析

## 4. 投影层(region-selection + section-projection)

- [x] 4.1 新增 `io.excelir.project` 包 + `Projection` 类(纯函数:`IrDocument project(IrDocument doc, Range range, Set<Section> select)`)
- [x] 4.2 `Range` 值对象:解析 A1 起终点为 0-based col/row;矩形判交工具(rectanglesIntersect / contains)
- [x] 4.3 区段裁剪:cells(按 ref 坐标落区间)、merges(矩形相交)、columns/rows(idx 在区间内)、dimensions 收窄、images/charts/shapes(锚点矩形判交)
- [x] 4.4 `--select` 过滤:按 9 词决定保留哪些顶层区段;`layout`/`page`/`media` 聚合展开;省略 select = 全留
- [x] 4.5 cells 闭包:收集(经区间裁剪后)cells 引用的 style id 集合 → 带出对应 xfs 子集 → 再带出这些 xfs 引用的 fonts/fills/borders 子集;显式 select 的样式子表取全表
- [x] 4.6 叠加顺序固定:投影(选类别)→ 区间(裁空间)→ 闭包(补引用链);复用现有 record 构造新 IrDocument
- [x] 4.7 纯全局区段(borders/fonts/fills/xfs/page)不受区间影响
- [x] 4.8 单测:区间裁剪 cells/merges/media、纯全局区段不裁、闭包子集正确、闭包基于区间内 cells、显式选样式取全表、select+range 交集、越界静默裁剪

## 5. Main 接线

- [x] 5.1 `Main`:提取后调用 `Projection.project(doc, range, select)` 得裁剪后 IrDocument,再 `JsonWriter.write(doc, pretty, fullCells)` 输出
- [x] 5.2 mediaDir 逻辑保持不变(图片仍全量落盘)
- [x] 5.3 错误分支:IllegalArgumentException → exit 2(含 `--range`/`--select` 校验消息),其余 → exit 1
- [x] 5.4 冒烟:对张三薪酬表跑默认、`--range A5:O8`、`--select cells,borders`、`--range A1:E6 --select merges`、`--pretty`、`--full-cells`、非法区间/未知词 exit 2

## 6. 文档与构建

- [x] 6.1 README 字段速览:`widthChars` 去除说明、新增 `shrinkToFit`/对角线/`bgColor`、schemaVersion 2
- [x] 6.2 README 用法:新增 `--range`/`--select`/`--pretty`/`--full-cells` 说明 + 示例 + 区段词表 + 默认紧凑形态说明
- [x] 6.3 `mvn clean package` 重新生成 fat-jar,确认测试全绿
