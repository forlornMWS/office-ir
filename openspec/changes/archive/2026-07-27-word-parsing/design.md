## Context

excel-ir 已建立分层架构:提取(POI,含落盘)→ 纯数据投影层(`--select`/闭包,复用 record)→ 序列化层(NON_NULL/紧凑/逃逸阀),两层开关正交、各可单测。现把 `.docx` 纳入同一 ethos。Word 是递归块流(段落/表格,cell 嵌块流),与 Excel 扁平网格本质不同,故独立 `WordIr`(schemaVersion 1)。XWPF 已在 poi-ooxml(已验证 `XWPFDocument.class` 在 poi-ooxml-5.2.5.jar),零新依赖。无仓库内消费者依赖旧包名/旧 jar 名。

## Goals / Non-Goals

**Goals:**
- Word `.docx` → `WordIr` JSON,默认即经济(NON_NULL + run 合并 + 空 run 丢 + 空段压缩),零语义损失。
- 复用已建分层:Word 投影层(递归 `--select` + 闭包)、序列化层(WordIr 多态序列化 + `--full`/`--pretty`)。
- 改名 office-ir + 扩展名分发,excel 行为不变。
- 覆盖 v1 Word 核心:段落/run/表格(含嵌套+合并)/命名样式(解析继承)/编号/节+页面布局+页眉页脚/行内+锚定图/属性。

**Non-Goals:**
- 不做 Word `--range` 空间裁剪(无文档级网格)。
- 不支持 `.doc`(HWPF,留 v2+);不引入 poi-scratchpad。
- 不解析脚注/尾注/TOC/批注/修订跟踪/SmartArt/公式(留 v2)。
- 不预渲染列表序号文本(只给 kind 枚举)。
- 不引入新 IR 类型表示"投影后 Word"(复用 WordIr)。

## Decisions

### D1: 扩展+改名,artifactId 与 Java 包同步
**选:** office-ir / io.officeir(excel→.excel, word→.word, 共享→io.officeir)。**否:** 只改 artifactId 保留 io.excelir。
**为什么:** 共享层(json/project/通用 extract)将同时服务两种文档,包若仍叫 io.excelir 则 word 代码引用 `io.excelir.json` 归属错乱。50 文件 IDE 重构零风险,一次性到位。excel 行为不变(只坐标/包名变)。

### D2: body 密封 BlockIr + Jackson 多态判别
**选:** 密封接口 `BlockIr` + `@JsonTypeInfo(property="kind")` + `@JsonSubTypes`。**否:** 单 record + kind 字段;按类型分列。
**为什么:** 类型安全、可扩展(日后加 imageBlock/sectionBreak 只加子类型);递归通过 `cell.blocks: List<BlockIr>` 自然表达。Jackson 2.16 对 record + `@JsonTypeInfo` 支持完善。Word 本是递归结构,忠实建模。

### D3: run 严格边界合并
**选:** 相邻 + 同有效格式(样式 ref + 直接覆盖)+ 同链接 + 无内嵌对象 → 拼接 text。**否:** 只看直接格式;不合并。
**为什么:** 保文本流顺序与链接/图边界,无损且大幅省体积(Word 常把一词切成几十 run)。边界处(图/换行/链接/对象)run 独立保留。`--full` 可还原原始边界。

### D4: 命名样式表(解析继承)+ run 稀疏覆盖
**选:** styles.paragraphStyles/characterStyles 每条解析 basedOn 到有效值 + 保留 basedOn ref;run 带 styleRef + 仅非空直接覆盖。**否:** runXf 表;无样式表内联。
**为什么:** Word 命名样式承载语义(标题级),LLM 想直接看有效值。Excel 用 xf 表因样式无名;Word 有命名语义,解析表+稀疏覆盖既保留语义又经济。相邻合并(D3)已处理最高频去重,非相邻重复靠稀疏覆盖+styleRef 足够。

### D5: 编号 {numId, ilvl, kind} 不渲染序号
**选:** kind 解析枚举(bullet/decimal/lowerLetter/...),不渲染 "1."/"a)"。**否:** 仅原始 id;全渲染。
**为什么:** LLM 需"列表结构"(项/层级/有序无序),序号由位置可推;渲染需手算位置计数器、POI 不直接给、易错且属表现层。numId 保留供分组。

### D6: 行内图=密封 RunIr 保位;锚定图进 media 列表
**选:** `InlineImageRunIr` 夹在文字 run 间保位置;锚定图进顶层 media[] 带 `{relativeTo, offsetXPx, offsetYPx, w, h}`。**否:** 全进 media(丢行内位置);run 字段。
**为什么:** 行内图位置对 LLM 理解重要("见下图"后即图),不能丢;锚定图本就浮动不在流中,进独立列表诚实。与 D3 一致(含图 run 不参与合并)。

### D7: 顶层 sections[] 含 body
**选:** `sections[]{pageSetup, headers, footers, body[]}`。**否:** 分节符块穿插;平铺无节。
**为什么:** 节持有正文+页面参数+页眉页脚,语义自洽;多节混合方向文档的布局差异得以保留。单节文档自然一元素。excel 的 pageSetup 全局→Word 升格 per-section。

### D8: 页眉页脚同构块流
**选:** 解析为 `List<BlockIr>`(与 body 同构)。**否:** 文本拼接;独立类型。
**为什么:** 页眉页脚本质是受限文档块流,同构复用 BlockIr 体系(含图落盘),零新类型。空则省略(NON_NULL),同 excel headerFooter "全空省略整块" ethos。

### D9: --select 词汇 {paragraphs,tables,styles,media,headers,page,properties}
**选:** 7 词,与 excel 同源思路按 Word 结构调整。**否:** v1 不带;极简 body/styles/meta。
**为什么:** 与 excel ethos 对称,LLM 可精确取子结构。投影层复用 excel 思路改递归。

### D10: 表格递归穿透(paragraphs 进表 cell)
**选:** 选 paragraphs 走进表 cell 取所有段落带定位;选 tables 整表留含内嵌。**否:** 不穿透(段落仅顶层)。
**为什么:** "所有段落"应含表内,带定位保结构感;tables 整表含内嵌自洽(F6)。需在 paragraphs 输出标注来源(顶层 vs 表cell 坐标)避免定位丢失。

### D11: 空段压缩/空 run 丢弃
**选:** 空段→`{style?, numbering?}`(保留结构意义);空 run(无文本无图)丢弃。**否:** 空段也丢;全保留。
**为什么:** 空段是块级,可能承列表占位/分节/样式空行,有结构意义→保留 style/numbering;空 run 是行内,无信息(格式已由同段 run/样式体现)→丢弃无损。同 excel "blank→{ref,style}" ethos。

### D12: Word schemaVersion 独立从 1 起
**选:** WordIr v1;excel 保持 2。**否:** 共享版本;统一+判别字段。
**为什么:** 版本是"某套 IR 形状的契约";Word 与 excel 不同顶层类型、不同形状,独立演进最诚实,Word 演化不污染 excel 契约。顶层类型已判别形状,无需额外 docType。

### D13: 扩展名分发 + Word --full + 无用 flag 报错
**选:** 按扩展名分发;Word 继承 `--output`/`--pretty`/`--select` + 新 `--full`;`--range`/`--sheet`→Word exit 2。**否:** Word 无逃逸;静默忽略。
**为什么:** 逃逸阀对称(excel --full-cells / Word --full)供调试还原原始 run 边界;`--range`/`--sheet` 对 Word 无意义,报错防 LLM 误传拿残缺输出。`--pretty`/`--output` 是 IO 层两文档通用。

### D14: 仅 .docx,无新依赖
**选:** 仅 .docx(XWPF 在 poi-ooxml,零新依赖);.doc 拒收 exit 2。**否:** 同时 .doc(引 poi-scratchpad)。
**为什么:** 与 excel 只做 OOXML 新格式(.xlsx)对称;.doc(HWPF)需 poi-scratchpad、维护差、语义不同,留 v2。XWPF 已在 poi-ooxml(已验证)。

### D15: 选正文块闭包带引用样式子集
**选:** 选 paragraphs/tables/headers 带出引用样式子集;显式 select styles 取全表。**否:** 悬空;总带全表。
**为什么:** 与 excel 闭包 ethos 一致:被选块引用的样式随行保 styleRef 可解;样式表本小,闭包子集更小。Word 无 range,故闭包源 = 选中的块(无区间收敛层)。

### D16: 表格 cell 捕获合并+宽+底纹+边框
**选:** cell `{blocks, colSpan?, vMerge?(restart/continue), widthPx?, shading?, border?}`,NON_NULL 省空。**否:** 仅合并;仅 blocks。
**为什么:** 合并是结构必须(跨列/纵向不记则语义错);宽/底纹/边框是布局+格式,同 excel 抓列宽/填充/边框;NON_NULL 保证无则省略不膨胀。vMerge restart/continue 二态比布尔更诚实。

## Risks / Trade-offs

- [改名破坏旧调用方] → 无仓库内消费者;README 标注 BREAKING + 旧 `excel-ir.jar` 调用方改 `office-ir.jar`(同 args)。
- [50 文件包名重构易出错] → IDE 全局重构 + 编译/既有测试守门;excel 测试适配新包后应全绿。
- [run 合并丢原始边界] → 语义无损(同格式拼接可还原);`--full` 可恢复原始 run 边界。
- [样式继承解析复杂] → basedOn 链可能成环或断;解析时设深度上限 + 缺失则止,不抛异常(降级为局部有效值)。
- [表内嵌递归投影复杂] → 选 paragraphs 穿透收集需带定位;选 tables 整树保留;单测覆盖嵌套表。
- [锚定图 anchor 简化] → v1 只记 relativeTo + px 偏移,不精确还原所有 EMU/对齐;复杂锚定留 v2。
- [POI 某些格式 accessor 不稳(如 Excel bgColor 经历过)] → Word 颜色优先走底层 CT* 读 rgb;索引/主题色可能 null,代码注释标注。

## Migration Plan

1. 改名:pom artifactId→office-ir;Java 包 io.excelir→io.officeir(excel→.excel,共享→io.officeir);重构既有 50 文件;确认 excel 测试全绿。
2. 抽共享层:json/JsonWriter、CliArgs、Main 进 io.officeir,设计扩展名分发骨架。
3. 新增 io.officeir.word.ir:WordIr/BlockIr/ParagraphIr/TableIr/RunIr/StyleIr/NumberingIr/SectionIr/...(record + 密封接口 + Jackson 多态注解)。
4. 新增 io.officeir.word.extract:XWPFDocument→WordIr(段落/run 合并/样式继承解析/编号 kind/表格合并/图片落盘/节+页眉页脚)。
5. 新增 io.officeir.word.project:递归 `--select`(穿透/闭包)+ WordIr 序列化(NON_NULL/紧凑/--full/--pretty)。
6. CliArgs:扩展名分发 + Word flag 继承 + `--range`/`--sheet`→Word exit 2 + .doc 拒收。
7. Main:分发到 excel/word 提取链。
8. 测试:Word 各层单测 + 冒烟(.docx 默认/`--select`/`--full`/`--pretty`/.doc 拒收/--range 拒收)。
9. README:改名说明 + Word 用法 + 区段词表 + BREAKING。
10. `mvn clean package` 出 office-ir.jar。

**回滚:** 改名与 Word 模块可分别提交;改名可单独 revert(包/jar 名回退),但 Word 模块独立、不回退 excel。

## Open Questions

- 无。所有关键决策已在 grilling 中与用户逐条确认(D1–D16)。
