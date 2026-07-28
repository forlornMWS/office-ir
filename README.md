# office-ir

把 `.xlsx` / `.docx` 解析成技术栈无关、对 LLM agent 友好的 JSON IR(中间表示)。**按扩展名分发**:`.xlsx`→Excel IR,`.docx`→Word IR。

> 历史名 `excel-ir` 已改名 `office-ir`(artifactId / fat-jar / Java 包 `io.officeir`)。Excel 行为(压缩/区间/投影/样式覆盖)不变,仅坐标/包名变。调用方把 `excel-ir.jar` 换成 `office-ir.jar` 即可。

## 构建

```bash
mvn clean package      # 生成 target/office-ir.jar (fat-jar)
```

> 构建 Maven 需 JDK 17+(运行时也需 JDK 17+,见下)。若机器默认 `JAVA_HOME` 指向 JDK 8,构建前请显式指定:`JAVA_HOME=/path/to/java17 mvn clean package`。

## 运行(需 JDK 17+ 运行时)

```bash
java -jar office-ir.jar <input.xlsx|input.docx> [选项]
```

> ⚠️ 该 jar 需要 JDK 17+ 运行时。若系统默认 `java` 是 JDK 8,请用 JDK 17 的 java 显式运行:
> ```bash
> "/path/to/java17/bin/java" -jar office-ir.jar input.docx
> ```

## 通用选项

- `-o, --output <file>`:写入文件(默认 stdout);图片像素落盘到同级 `out.media/`(JSON 只放相对路径)
- `--pretty`:恢复缩进输出(默认单行紧凑)

## Excel(`.xlsx`)

解析覆盖:单元格值(原始值/类型/公式/格式化显示串)、规范化样式表(字体/字号/填充/边框/对齐/数字格式/缩小填充)、行高、列宽、合并单元格、冻结窗格、自动筛选、页面布局、页边距、打印区域、打印标题、页眉页脚、定义名称、文档属性;图片/图表/形状只记元数据(落盘+引用)。

### 选项

| 选项 | 说明 |
|---|---|
| `-o, --output <file>` | 写入文件(默认 stdout);图片像素落盘到与 `out.json` 同级的 `out.media/`(JSON 里只放相对路径引用) |
| `--sheet <名称\|索引>` | 只解析指定 sheet(名称或 0 起索引);省略则解析全部 |
| `--range <A1:E6>` | A1 原生区间裁剪。只裁剪有空间坐标的区段(cells/merges/layout/media);纯全局样式区段(borders/fonts/fills/xfs/page)不受影响。区间越界静默裁到实际尺寸,不报错;格式非法则 exit 2 |
| `--select <区段>` | 严格只返回命名区段,逗号分隔。省略则返回全部。合法值见下表 |
| `--pretty` | 恢复缩进输出(默认单行紧凑) |
| `--full-cells` | 恢复旧单元格冗余形态(blank 含完整字段、`formatted` 即使等于 `raw` 也输出) |

**默认输出即紧凑形态**(单行 + 省略 `null` 字段 + 空单元格压缩为 `{ref,style}` + `formatted==raw` 时省略 `formatted` + 列宽省略可推导的 `widthChars` + 全 null 的页眉页脚块省略),零信息损失。`--pretty` / `--full-cells` 为调试逃逸阀,二者正交。

### `--select` 区段词表

| 区段 | 内容 |
|---|---|
| `cells` | 单元格(含 style 引用) |
| `merges` | 合并单元格区域 |
| `borders` / `fonts` / `fills` / `xfs` | 全局样式表对应分片(无空间归属,区间不裁) |
| `layout` | dimensions + columns + rows + freezePanes + autoFilter |
| `page` | pageSetup + margins + headerFooter |
| `media` | images + charts + shapes |

- **闭包**:选 `cells` 时自动带出区间内 cells 引用到的 `xfs`,以及那些 xfs 引用到的 `fonts`/`fills`/`borders` 子集,保证输出自洽可解。显式 `--select` 某样式子表则取全表。
- **叠加顺序**:投影(选类别)→ 区间(裁空间)→ 闭包(补引用链)。`--range` 与 `--select` 正交合取。

### 示例

```bash
# 默认紧凑输出(推荐给 LLM)
java -jar office-ir.jar book.xlsx

# 只看薪酬数据区 A5:O8
java -jar office-ir.jar book.xlsx --range A5:O8

# 只看边框定义 + 哪些单元格有边框
java -jar office-ir.jar book.xlsx --select cells,borders

# 看某区域的合并区域(不要 cells/样式)
java -jar office-ir.jar book.xlsx --range A1:E6 --select merges

# 调试用:缩进 + 完整单元格形态
java -jar office-ir.jar book.xlsx --pretty --full-cells
```

## Word(`.docx`)

解析覆盖:文档属性、节(单节;多节/页眉页脚内容/页面布局 v1 简化留 v2)、body 块流(段落/表格,表格 cell 递归嵌块流)、run(文本 + 行内图,D3 合并相邻同格式 run)、编号(numId/ilvl;kind v1 不解析 numFmt)、行内图落盘。默认紧凑形态:NON_NULL + run 合并 + 空 run 丢弃 + 空段压缩为 `{style, numbering?}`。

> v1 best-effort/简化(留 v2):多节页面布局、页眉页脚内容、表格 gridSpan/vMerge/shading、命名样式表定义(段落/run 仍带 style ref + 直接覆盖)、编号 kind、锚定图、主题色解析。

### 选项

| 选项 | 说明 |
|---|---|
| `--select <区段>` | 严格只返命名区段,逗号分隔 |
| `--full` | 调试逃逸:不合并 run + 留空 run + 完整段落形态 |

`--range`/`--sheet`/`--full-cells` 对 Word 报错 exit 2(Word 无网格/sheet)。

### `--select` 区段词表(Word)

| 区段 | 内容 |
|---|---|
| `paragraphs` | 段落(选时穿透表 cell 取所有段落;若同时选 tables 则只取顶层避免重复) |
| `tables` | 表格(整表含内嵌段落) |
| `styles` | 命名样式表(v1 为空,留 v2) |
| `media` | 锚定图(v1 为空) |
| `headers` | 各节页眉页脚(v1 简化) |
| `page` | 各节 pageSetup/margins(v1 简化) |
| `properties` | 文档属性 |

### 示例

```bash
# 解析 Word 文档
java -jar office-ir.jar book.docx

# 只取正文段落(含表内段落)
java -jar office-ir.jar book.docx --select paragraphs

# 调试:不合并 run
java -jar office-ir.jar book.docx --full --pretty
```

## 格式限制

`.xls` / `.doc` 输入会报错退出(exit 2),提示用 libreoffice 转换为 `.xlsx` / `.docx`:

```bash
libreoffice --headless --convert-to xlsx book.xls
libreoffice --headless --convert-to docx book.doc
```

## IR 字段速览

- `schemaVersion`:IR 版本(当前为 **2**)
- `workbook.properties`:文档属性(creator / created 等;`null` 字段省略)
- `workbook.styles`:规范化样式表(`fonts` / `fills` / `borders` / `cellXfs`),每个单元格 `style:"S{n}"` 引用其中 `cellXfs[n]`;`cellXf` 又引用 `fonts` / `fills` / `borders` 表项
  - `cellXf` 字段:`font`/`fill`/`border` 引用 + `hAlign`/`vAlign`/`wrapText`/`shrinkToFit`/`textRotation`/`indent`/`numberFormat`/`locked`/`hidden`
  - `border` 字段:`top`/`bottom`/`left`/`right` + `diagonalLeft`(对应 OOXML diagonalDown,\)/`diagonalRight`(对应 diagonalUp,/)
  - `fill` 字段:`pattern` + `fgColor`(实心填充可见色)+ `bgColor`(花纹底色)
- `workbook.definedNames`:定义名称(含 `_xlnm.Print_Titles` / `_xlnm.Print_Area` 等)
- `sheets[]`:每 sheet 含
  - `dimensions` / `columns`(列宽 `widthPx`)/ `rows`(行高 `heightPt`)/ `mergedRegions`
  - `cells`:每格 `ref` / `type`(s/n/b/e/f/blank)/ `raw`(有缓存值时)/ `formula` / `formatted`(与 `raw` 不同时)/ `style`。blank 单元格压缩为 `{ref,style}`
  - `images`(路径 + 锚点 + 尺寸 + 格式)/ `charts`(类型 + 锚点 + 标题)/ `shapes`(锚点 + 文字 + 样式)
  - `freezePanes` / `autoFilter`
  - `pageSetup`(纸张/方向/fitToPage/缩放/网格线/标题/打印区域/重复行列)
  - `margins`(英寸)/ `headerFooter`(奇偶/首页 页眉页脚;全空则整体省略)

## 单位约定

列宽 `widthPx`、行高 `heightPt`、字号 `sizePt`、页边距 `*In`(英寸)、图片锚偏移 `*Px`。值已归一化为可读单位。
