## ADDED Requirements

### Requirement: 区段投影严格语义

`--select <逗号分隔区段>` SHALL 严格只返回命名区段。合法区段词表为固定 7 个:`paragraphs tables styles media headers page properties`。`paragraphs` = body 顶层段落 + 表 cell 内段落(穿透);`tables` = 整表(含内嵌段落);`styles` = 命名样式表;`media` = 锚定图;`headers` = 各节页眉页脚;`page` = 各节 pageSetup/margins;`properties` = 文档属性。省略 `--select` = 全部区段。含未知词 SHALL 以退出码 2 退出并打印合法词表。

#### Scenario: 只选部分区段

- **WHEN** 执行 `--select paragraphs,styles`
- **THEN** 输出仅含 paragraphs 与 styles,不含 tables/media 等

#### Scenario: 省略 select 返回全部

- **WHEN** 执行 `java -jar office-ir.jar book.docx`(无 `--select`)
- **THEN** 输出含全部 7 个区段

#### Scenario: 未知区段词报错

- **WHEN** 执行 `--select paragraf`
- **THEN** 退出码为 2
- **AND** stderr 打印合法词表(paragraphs tables styles media headers page properties)

### Requirement: paragraphs 递归穿透表 cell

选 `paragraphs` 时,系统 SHALL 收集 body 顶层段落 + 所有表格 cell 内的段落(含嵌套表的),平铺到一个 paragraphs 流并 SHALL 标注每个段落的来源定位(顶层位置 / 表 cell 坐标)。选 `tables` 时 SHALL 整表保留(含内嵌段落,表自洽)。

#### Scenario: paragraphs 收集表内段落

- **WHEN** 执行 `--select paragraphs`,文档 body 含一段 + 一表(表 cell 内有 2 段)
- **THEN** paragraphs 输出含 3 个段落(顶层 1 + 表内 2)
- **AND** 每个段落带来源定位

#### Scenario: tables 整表含内嵌

- **WHEN** 执行 `--select tables`
- **THEN** 输出整表(行/cell/内嵌段落全留),不残缺

### Requirement: 选正文块触发样式链闭包

选 `paragraphs`/`tables`/`headers` 时,系统 SHALL 自动带出这些块及其 runs 引用到的 `styles` 子集(命名的段落/字符样式),保证 styleRef 可解。闭包源 = 选中的块(Word 无 `--range`,无区间收敛层)。显式 `--select styles` SHALL 取整张命名样式表。

#### Scenario: 选 paragraphs 自动带样式

- **WHEN** 执行 `--select paragraphs`,段落引用样式 Heading1
- **THEN** 输出含 paragraphs 与 styles(至少含 Heading1 定义)

#### Scenario: 显式选 styles 取全表

- **WHEN** 执行 `--select styles`
- **THEN** 输出含整张命名样式表(全部定义),不受其它过滤

#### Scenario: 不选正文块不触发闭包

- **WHEN** 执行 `--select media`(锚定图)
- **THEN** 输出不含 styles(无正文块引用,不触发闭包)
