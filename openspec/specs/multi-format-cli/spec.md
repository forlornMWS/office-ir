# multi-format-cli

## Purpose

定义 office-ir 多格式 CLI:改名 + 扩展名分发 + flag 继承 + 老格式拒收。excel/Word 共享序列化/IO 层,各自提取链。

## Requirements

### Requirement: office-ir 改名

项目 SHALL 改名为 `office-ir`(artifactId、fat-jar `office-ir.jar`、Java 包 `io.officeir`)。Excel 代码 SHALL 进 `io.officeir.excel`、Word 进 `io.officeir.word`、共享(ir/json/通用 extract/project)进 `io.officeir`。Excel 既有行为(压缩/区间/投影/样式覆盖)SHALL NOT 改变。

#### Scenario: 改名后 excel 行为不变

- **WHEN** 执行 `java -jar office-ir.jar book.xlsx`(原 excel-ir.jar 改名)
- **THEN** 输出与改名前等价(excel IR,schemaVersion 2,压缩/区间/投影行为一致)

#### Scenario: 包名归属

- **WHEN** 查看源码包结构
- **THEN** excel 在 `io.officeir.excel`、word 在 `io.officeir.word`、共享在 `io.officeir`

### Requirement: 扩展名分发

CLI SHALL 按输入扩展名分发:`.xlsx` → excel 提取链(WorkbookExtractor→IrDocument),`.docx` → Word 提取链(XWPFDocument→WordIr)。两链共享序列化层(`JsonWriter`)与投影层思路。

#### Scenario: docx 走 Word 链

- **WHEN** 执行 `java -jar office-ir.jar book.docx`
- **THEN** 输出为 `WordIr`(顶层含 `sections`/`styles`,非 excel 的 `sheets`)

#### Scenario: xlsx 走 excel 链

- **WHEN** 执行 `java -jar office-ir.jar book.xlsx`
- **THEN** 输出为 excel `IrDocument`(顶层含 `sheets`)

### Requirement: Word flag 继承与拒绝

Word 输入 SHALL 继承 `--output`/`-o`、`--pretty`、`--select`(Word 词汇)。Word 输入 SHALL 支持 `--full` 逃逸(不合并 run + 留空 run + 完整字段)。Word 输入遇 `--range` 或 `--sheet` SHALL 以退出码 2 退出并打印「Word 不支持 --range/--sheet」诊断(静默忽略会误导)。

#### Scenario: Word 接受继承 flag

- **WHEN** 执行 `java -jar office-ir.jar book.docx --select paragraphs --pretty`
- **THEN** 输出为缩进 JSON,仅 paragraphs 区段

#### Scenario: Word 拒绝 --range

- **WHEN** 执行 `java -jar office-ir.jar book.docx --range A1:E6`
- **THEN** 退出码为 2
- **AND** stderr 打印「Word 不支持 --range」

#### Scenario: Word --full 逃逸

- **WHEN** 执行 `java -jar office-ir.jar book.docx --full`
- **THEN** 输出不合并相邻 run、保留空 run

### Requirement: .doc 拒收

`.doc` 输入 SHALL 以退出码 2 拒收并提示用 libreoffice 转换为 .docx(对称 excel 拒 `.xls`)。

#### Scenario: .doc 拒收

- **WHEN** 执行 `java -jar office-ir.jar book.doc`
- **THEN** 退出码为 2
- **AND** stderr 提示 `libreoffice --headless --convert-to docx book.doc`
