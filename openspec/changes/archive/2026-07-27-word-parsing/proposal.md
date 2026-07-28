## Why

excel-ir 已是「对 LLM agent 友好的 .xlsx JSON IR」,并已建立压缩(NON_NULL/紧凑/逃逸阀)+ 投影(`--select` 闭包)+ 分层(提取→投影→序列化)的架构。现需对称地把 `.docx` 也解析成同 ethos 的 JSON IR,复用已建架构,并把项目改名 `office-ir` 以反映多格式定位。Word 是递归块流(段落/表格,表格 cell 嵌块流),与 Excel 的扁平网格本质不同,需独立 `WordIr` 形状。

## What Changes

- **BREAKING**:项目改名 `excel-ir` → `office-ir`;Java 包 `io.excelir` → `io.officeir`(excel→`io.officeir.excel`、word→`io.officeir.word`、共享→`io.officeir`);fat-jar `office-ir.jar`;CLI 按扩展名分发(`.xlsx`→excel IR,`.docx`→Word IR)。excel 行为(压缩/区间/投影/样式覆盖)不变,只是坐标/包名变。
- 新增 `WordIr`:`{schemaVersion:1, properties, styles, sections[], media}`。`sections[]` 每节 `{pageSetup, headers, footers, body: List<BlockIr>}`。
- `body` 为密封 `BlockIr` 接口(Jackson `@JsonTypeInfo(kind)`):`ParagraphIr`/`TableIr`;`TableIr.cell.blocks` 递归回 `List<BlockIr>`。
- 段落 `runs` 为密封 `RunIr`:`TextRunIr`/`InlineImageRunIr`(行内图保流位置)。
- 命名样式表(解析 `basedOn` 继承到有效值 + 保留 basedOn ref);run 带 styleRef + 稀疏直接覆盖。
- run 合并:相邻同有效格式(样式 ref + 直接覆盖)+ 同链接目标 + 无内嵌对象 → 拼接 text 成一个 run。
- 编号:段落带 `numbering:{numId, ilvl, kind}`(kind 解析枚举 bullet/decimal/lowerLetter/…,不渲染序号)。
- 表格 cell:`{blocks, colSpan?, vMerge?(restart|continue), widthPx?, shading?, border?}`(NON_NULL 省空)。
- 图片:行内图 = `InlineImageRunIr`(保位置);锚定图进顶层 `media[]` 带简化 anchor(relativeTo + px 偏移);图片落盘到 `out.media/`(同 excel)。
- Word 默认压缩:run 合并 + 空 run 丢弃(无文本无图)+ 空段压缩为 `{style, numbering?}` + `NON_NULL`;`--full` 逃逸(不合并 + 留空 run + 完整段落字段);`--pretty` 缩进(NON_NULL 固定)。
- Word `--select` 词汇 `{paragraphs, tables, styles, media, headers, page, properties}`;递归穿透(选 `paragraphs` 走进表 cell 取所有段落带定位,选 `tables` 整表留含内嵌);选正文块闭包带引用样式子集,显式 `--select styles` 取全表;Word 无 `--range` 空间裁剪。
- `.doc` 拒收 exit 2 + 转换提示(对称 excel 拒 `.xls`);XWPF 已在 poi-ooxml,零新依赖。
- `--range`/`--sheet` 对 Word 报 exit 2「Word 不支持」。

## Capabilities

### New Capabilities

- `word-document-ir`:Word IR 形状与提取——`WordIr`/`BlockIr`(密封:Paragraph/Table,递归)/`RunIr`(密封:Text/InlineImage)/命名样式表(解析继承)/编号/表格 cell(合并+宽+底纹+边框)/节(pageSetup+headers+footers+body)/图片(行内保位+锚定落盘)/文档属性。
- `word-compaction`:Word 默认序列化压缩——run 合并、空 run 丢弃、空段压缩、`NON_NULL`;`--full`/`--pretty` 逃逸阀(正交,NON_NULL 固定)。
- `word-projection`:Word `--select` 区段投影——9 词词汇、递归穿透(paragraphs 进表 cell)、样式链闭包(选正文块带引用子集,显式 styles 取全表)、无 range 维度。
- `multi-format-cli`:office-ir 改名 + 扩展名分发 + flag 继承(Word 继承 `--output`/`--pretty`/`--select` + 新 `--full`)+ `.doc` 拒收 + `--range`/`--sheet` 对 Word exit 2。

### Modified Capabilities

<!-- 无。excel 既有 4 spec(output-compaction/region-selection/section-projection/style-coverage)的行为要求不变;改名属坐标/实现细节,示例中的 `excel-ir.jar` 调用名在 tasks 里刷新,不构成 spec 级 requirement 变更。 -->

## Impact

- **改名**:pom `artifactId` excel-ir→office-ir;Java 包 io.excelir→io.officeir(excel 50 文件重构进 .excel 子包,共享 ir/json/project 进 io.officeir);fat-jar 名/README/CLI 调用全改。
- **新增 `io.officeir.word`**:extract(`XWPFDocument`→`WordIr`)、ir(WordIr/BlockIr/ParagraphIr/TableIr/RunIr/StyleIr/NumberingIr/…)、project(递归投影 + 样式闭包)。
- **共享层 `io.officeir`**:json(`JsonWriter` 复用 + 加 WordIr 多态序列化)、CliArgs(扩展名分发 + Word flags)、Main(分发到 excel/word 提取链)。
- **依赖**:无新增(`poi-ooxml` 5.2.5 已含 XWPFDocument/XWPFParagraph/XWPFTable 等,已验证)。
- **测试**:Word 各层单测(IR 形状/压缩/投影/CLI/编号/表格/图片)+ 冒烟;excel 既有测试适配新包名。
- **迁移**:旧 `excel-ir.jar` 调用方改用 `office-ir.jar`(同 args);包名变动的下游(无仓库内消费者)。
