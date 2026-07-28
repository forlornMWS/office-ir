## ADDED Requirements

### Requirement: WordIr 顶层结构

`WordIr` SHALL 为 `{schemaVersion, properties, styles, sections, media}`。`schemaVersion` SHALL 为 `1`。`properties` 为文档核心属性(title/creator/created 等,null 字段省略)。`sections` 为节数组,每节 `{pageSetup, headers, footers, body}`。`media` 为锚定图列表(行内图不在其中)。

#### Scenario: 最小 Word 文档

- **WHEN** 解析一个仅含一段纯文本的 .docx
- **THEN** 输出顶层含 `schemaVersion:1`、`properties`、`styles`、`sections`(1 节)、`media`([])
- **AND** 该节 `body` 含一个 `ParagraphIr` 块

### Requirement: body 为密封块接口(递归)

`body` SHALL 为 `List<BlockIr>`,其中 `BlockIr` 为密封接口,序列化带 `kind` 判别(`"para"`/`"table"`)。`ParagraphIr` 与 `TableIr` 实现 `BlockIr`。`TableIr` 的 `rows[].cells[].blocks` SHALL 递归为 `List<BlockIr>`(cell 内嵌段落/表格)。

#### Scenario: 表格内嵌段落

- **WHEN** 文档含一个表格,某 cell 内有一段文字
- **THEN** 该 `TableIr` 的对应 cell `blocks` 含一个 `kind:"para"` 块
- **AND** JSON 通过 `kind` 判别块类型

#### Scenario: 嵌套表格

- **WHEN** 某 cell 内又含一个表格
- **THEN** 该 cell `blocks` 含 `kind:"table"` 块,递归结构成立

### Requirement: 段落与 run 结构

`ParagraphIr` SHALL 含 `style`(命名样式 ref,可空)、`numbering`(可空)、`align?`/`spacing?`/`indent?` 等段落格式稀疏覆盖、`runs: List<RunIr>`。`RunIr` 为密封接口序列化带 `kind` 判别:`"text"`/`"image"`。`TextRunIr` 含 `text` + `style`(命名字符样式 ref,可空)+ 稀疏直接覆盖(font/size/bold/italic/underline/color/strike/vertAlign,非空才出)。`InlineImageRunIr` 含 `path`/`format`/`widthPx`/`heightPx`/`alt?`。

#### Scenario: 段落带多 run

- **WHEN** 一段含「正常」+「加粗」两段文字
- **THEN** 该段 `runs` 含两个 `kind:"text"` run,第二个带 `bold:true` 覆盖

#### Scenario: 行内图保位置

- **WHEN** 一段为「见下图」+ 行内图 + 「所示」
- **THEN** `runs` 顺序为 text / image(kind:"image") / text,行内图位置忠实保留

### Requirement: 命名样式表(解析继承)

`styles` SHALL 含 `paragraphStyles` 与 `characterStyles`,每条 `{id, name, basedOn?, + 解析后的有效格式字段}`。基于 `basedOn` 链的继承 SHALL 被解析合并到有效值(子覆盖父),同时保留 `basedOn` ref 供参考。run/段落引用样式 id,有效格式 = run 直接覆盖 `||` 样式解析值。

#### Scenario: 标题样式继承解析

- **WHEN** 文档定义 Heading1 basedOn Normal,Normal 字号 11、Heading1 覆盖字号 14 加粗
- **THEN** `paragraphStyles` 中 Heading1 条目字号为 14、加粗(解析后),`basedOn` 指向 Normal
- **AND** Normal 条目字号 11

### Requirement: 编号/列表表示

带编号的段落 SHALL 含 `numbering:{numId, ilvl, kind}`。`kind` SHALL 从 numbering.xml 解析为可读枚举(`bullet`/`decimal`/`lowerLetter`/`upperLetter`/`lowerRoman`/`upperRoman`/…)。系统 SHALL NOT 预计算渲染序号文本(如 "1."/"a)")。`numId` 标识所属列表(同 numId 的项属同一列表)。

#### Scenario: 无序列表

- **WHEN** 段落为 bullet 列表第 1 级
- **THEN** 该段 `numbering` 含 `kind:"bullet"`、`ilvl:0`、`numId`(某整数)

#### Scenario: 有序列表不渲染序号

- **WHEN** 段落为 decimal 列表第 2 级
- **THEN** `numbering.kind` 为 `"decimal"`,`ilvl:1`
- **AND** 不输出渲染序号字符串

### Requirement: 表格单元结构与合并

`TableIr.rows[].cells[]` 每 cell SHALL 含 `blocks: List<BlockIr>` + 可选 `colSpan`(跨列数,默认1省略)+ `vMerge`(`"restart"`/`"continue"`,无纵向合并省略)+ `widthPx?` + `shading?` + `border?`。跨列用 `colSpan`,纵向合并用 `vMerge`(restart=起,continue=续上格)。

#### Scenario: 跨列标题

- **WHEN** 表头 cell 跨 3 列
- **THEN** 该 cell `colSpan:3`,`blocks` 含其内容

#### Scenario: 纵向合并

- **WHEN** 某 cell 与上方 cell 纵向合并(上方为合并起点)
- **THEN** 上方 cell `vMerge:"restart"`,下方续接 cell `vMerge:"continue"`,`blocks` 可为空

### Requirement: 节与页面布局

`sections[]` 每节 SHALL 含 `pageSetup`(纸张/方向/margins 等)+ `headers`(奇/偶/首页页眉,可空)+ `footers`(同)+ `body: List<BlockIr>`。单节文档自然只一个 section。

#### Scenario: 多节混合方向

- **WHEN** 文档含一节竖向 A4 + 一节横向 A4
- **THEN** `sections` 有 2 节,各自 `pageSetup.orientation` 不同

### Requirement: 页眉页脚同构块流

存在的页眉/页脚 SHALL 解析为 `List<BlockIr>`,与 `body` 同构(段落/表格/图)。未定义的页眉/页脚 SHALL 省略该字段(NON_NULL)。页眉页脚内嵌图 SHALL 同样落盘引用。

#### Scenario: 页眉含 logo 图

- **WHEN** 奇数页页眉含一段文字 + 一张 logo 图
- **THEN** `headers.oddHeader` 为块流,含 text run 段落 + 行内图 run,图 path 指向落盘文件

#### Scenario: 无页眉页脚省略

- **WHEN** 文档未定义任何页眉页脚
- **THEN** 节对象不含 `headers`/`footers` 键

### Requirement: 图片落盘与锚定

行内图(InlineImageRunIr)与锚定图 SHALL 落盘到 `out.media/`(或与 `-o` 同级),IR 内只放相对 path 引用。锚定图进顶层 `media[]`,带简化 anchor `{relativeTo: page|paragraph|margin, offsetXPx, offsetYPx, widthPx, heightPx}`。图片像素数据 SHALL NOT 进 JSON。

#### Scenario: 行内图落盘

- **WHEN** 段落含行内图
- **THEN** `InlineImageRunIr.path` 指向落盘文件,JSON 不含图片字节

#### Scenario: 锚定图进 media 列表

- **WHEN** 文档含一张浮动锚定图(相对页面)
- **THEN** 顶层 `media[]` 含该图,带 `anchor.relativeTo:"page"` 与 px 偏移

### Requirement: 文档属性

`properties` SHALL 含核心属性(title/creator/created 等)。null 字段 SHALL 省略(NON_NULL)。

#### Scenario: 属性提取

- **WHEN** 文档 creator 为 "Apache POI"、title 为空
- **THEN** `properties` 含 `creator`,`title` 键省略

### Requirement: 仅支持 .docx

系统 SHALL 接受 `.docx` 输入。`.doc` 输入 SHALL 以退出码 2 拒收,并提示用 libreoffice 转换(对称 excel 拒 `.xls`)。

#### Scenario: .doc 被拒

- **WHEN** 输入 `book.doc`
- **THEN** 退出码为 2
- **AND** stderr 提示转换为 .docx
