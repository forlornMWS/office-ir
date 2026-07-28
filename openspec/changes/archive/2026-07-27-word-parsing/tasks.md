## 1. 改名 office-ir(multi-format-cli:改名)

- [x] 1.1 pom `artifactId` excel-ir→office-ir;`shade` finalName→office-ir;主类不变(io.officeir.Main)
- [x] 1.2 Java 包 io.excelir→io.officeir;excel 代码进 `io.officeir.excel`(Main 例外见 1.3);IDE 全局重构 50 文件
- [x] 1.3 共享层进 `io.officeir`:`ir`(共享 record 若有)、`json`、`project`(excel Projection 暂留 excel 子包或共享,见 4.x)、`Main`、`CliArgs`
- [x] 1.4 excel 既有测试适配新包名(`io.officeir.excel.*`),确认全绿
- [x] 1.5 README 改名说明 + `office-ir.jar` 用法 + BREAKING 标注(excel 行为不变)

## 2. Word IR records(word-document-ir:形状)

- [x] 2.1 `io.officeir.word.ir.WordIr`(schemaVersion:1, properties, styles, sections, media)
- [x] 2.2 密封 `BlockIr` 接口 + Jackson `@JsonTypeInfo(property="kind")` + `@JsonSubTypes`;`ParagraphIr`/`TableIr` 实现;`TableIr.rows[].cells[].blocks: List<BlockIr>` 递归
- [x] 2.3 `ParagraphIr`(style?, numbering?, align?, spacing?, indent?, runs: List<RunIr>)
- [x] 2.4 密封 `RunIr` + 判别;`TextRunIr`(text, style?, font?, size?, bold?, italic?, underline?, color?, strike?, vertAlign?);`InlineImageRunIr`(path, format, widthPx, heightPx, alt?)
- [x] 2.5 `StyleIr`(id, name, basedOn?, + 解析后段落/字符格式有效字段)+ `Styles`(paragraphStyles, characterStyles)
- [x] 2.6 `NumberingIr`(numId, ilvl, kind)
- [x] 2.7 `TableCellIr`(blocks, colSpan?, vMerge?, widthPx?, shading?, border?);`TableRowIr`(cells);`TableIr`(rows)
- [x] 2.8 `SectionIr`(pageSetup, headers, footers, body);`PageSetupIr`(paperSize/orientation/margins/...)
- [x] 2.9 `WordMediaIr`(path, format, widthPx, heightPx, anchor: {relativeTo, offsetXPx, offsetYPx})
- [x] 2.10 `WordProperties`(title/creator/created/...)

## 3. Word 提取(word-document-ir:提取)

- [x] 3.1 `io.officeir.word.extract.WordDocumentExtractor.extract(Path, mediaDir) → WordIr`
- [x] 3.2 properties:核心属性(title/creator/created)
- [x] 3.3 styles:遍历 `XWPFStyles`,解析 basedOn 继承链到有效值(深度上限+防环),产出 paragraphStyles/characterStyles
- [x] 3.4 sections:遍历 body + sectPr;每节 {pageSetup, headers, footers, body}
- [x] 3.5 body 块流:段落→`ParagraphIr`、表格→`TableIr`,递归(table cell → blocks)
- [x] 3.6 段落 runs:遍历 XWPFRun,产出 TextRunIr;行内图→InlineImageRunIr 保位置
- [x] 3.7 run 合并(D3):相邻同有效格式+同链接+无内嵌对象→拼接 text(默认);`--full` 时跳过
- [x] 3.8 编号(D5):段落 `numbering{numId, ilvl, kind}`,kind 从 numbering.xml 解析(CTNumbering XMLBeans)
- [x] 3.9 表格 cell 合并(D16):gridSpan→colSpan;vMerge→"restart"/"continue";widthPx/shading/border
- [x] 3.10 图片落盘:行内图 + 锚定图落盘 `out.media/`;锚定图进顶层 media[] 带 anchor;JSON 只放 path
- [x] 3.11 页眉页脚(D8):每节 oddHeader/evenHeader/firstHeader/oddFooter/...,解析为 `List<BlockIr>`,未定义省略

## 4. Word 投影 + 序列化(word-projection + word-compaction)

- [x] 4.1 `io.officeir.word.project.WordProjection.project(WordIr, Set<Section>) → WordIr`(复用 record,纯数据)
- [x] 4.2 区段过滤:7 词词汇;layout/page/media 等聚合;省略 select=全留
- [x] 4.3 paragraphs 递归穿透(D10):收集顶层+表 cell 内段落,带来源定位(顶层 idx / 表 cell 坐标)
- [x] 4.4 tables 整表留含内嵌
- [x] 4.5 样式闭包(D15):选 paragraphs/tables/headers 带引用样式子集;显式 select styles 取全表;无 range 维度
- [x] 4.6 序列化:WordIr 多态(BlockIr/RunIr 的 @JsonTypeInfo)+ NON_NULL 默认 + 空 run 丢弃 + 空段压缩
- [x] 4.7 `JsonWriter.write(wordIr, pretty, full)` 重载;`--pretty` 缩进、`--full` 不合并+留空 run+完整字段(NON_NULL 固定)
- [x] 4.8 单测:run 合并/链接边界/空 run 丢/空段压缩/--full/--pretty/闭包/穿透

## 5. CLI 分发(multi-format-cli)

- [x] 5.1 `CliArgs`:按扩展名区分;`.xlsx` 走 excel 链(既有 flags),`.docx` 走 Word 链(继承 --output/--pretty/--select + 新 --full)
- [x] 5.2 Word 输入遇 `--range`/`--sheet`→抛 IAE(exit 2,消息「Word 不支持 --range/--sheet」)
- [x] 5.3 `.doc`/`.xls` 拒收 exit 2 + libreoffice 转换提示
- [x] 5.4 `Main`:扩展名分发到 excel/word 提取链 → 投影 → 序列化
- [x] 5.5 单测:扩展名分发、Word --range 拒收、.doc 拒收、Word flag 继承

## 6. 文档与构建

- [x] 6.1 README:Word IR 字段速览 + Word 用法(--select 词表/--full/--pretty)+ 改名 BREAKING + .doc 转换
- [x] 6.2 `mvn clean package` 出 `office-ir.jar`,excel+Word 测试全绿
- [x] 6.3 冒烟:对真实 .docx 跑默认/`--select paragraphs`/`--select tables,styles`/`--full`/`--pretty`/.docx/.doc 拒收/Word --range 拒收
