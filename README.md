# excel-ir

把 `.xlsx` 模板解析成技术栈无关、对 LLM agent 友好的 JSON IR(中间表示)。

解析内容覆盖:单元格值(原始值/类型/公式/格式化显示串)、规范化样式表(字体/字号/填充/边框/对齐/数字格式)、行高、列宽、合并单元格、冻结窗格、自动筛选、页面布局(纸张/方向/缩放/居中)、页边距、打印区域、打印标题(重复行列)、页眉页脚、定义名称、文档属性;图片/图表/形状只记元数据(图片落盘 + 路径引用)。

## 构建

```bash
cd excel-ir
mvn clean package      # 生成 target/excel-ir.jar (fat-jar)
```

> 构建 Maven 需 JDK 17+(运行时也需 JDK 17+,见下)。若机器默认 `JAVA_HOME` 指向 JDK 8,构建前请显式指定:`JAVA_HOME=/path/to/java17 mvn clean package`。

## 运行(需 JDK 17+ 运行时)

```bash
java -jar excel-ir/target/excel-ir.jar <input.xlsx> [-o out.json] [--sheet 名称|索引]
```

> ⚠️ 该 jar 需要 JDK 17+ 运行时。若系统默认 `java` 是 JDK 8,直接 `java -jar` 会失败;请用 JDK 17 的 java 显式运行,例如:
> ```bash
> "/path/to/java17/bin/java" -jar excel-ir/target/excel-ir.jar input.xlsx
> # 或
> JAVA_HOME=/path/to/java17 java -jar excel-ir/target/excel-ir.jar input.xlsx
> ```

- 不带 `-o`:JSON 输出到 stdout
- `-o out.json`:写入文件;图片像素落盘到与 `out.json` 同级的 `out.media/` 目录(JSON 里只放相对路径引用)
- `--sheet`:只解析指定 sheet(名称或 0 起索引);省略则解析全部

## 仅支持 .xlsx

`.xls` 输入会报错退出(exit 2)。请先转换为 `.xlsx`:

```bash
libreoffice --headless --convert-to xlsx book.xls
```

## IR 字段速览

- `schemaVersion`:IR 版本(当前为 1)
- `workbook.properties`:文档属性(title / creator / created)
- `workbook.styles`:规范化样式表(`fonts` / `fills` / `borders` / `cellXfs`),每个单元格 `style:"S{n}"` 引用其中 `cellXfs[n]`;`cellXf` 又引用 `fonts` / `fills` / `borders` 表项
- `workbook.definedNames`:定义名称(含 `_xlnm.Print_Titles` / `_xlnm.Print_Area` 等)
- `sheets[]`:每 sheet 含
  - `dimensions` / `columns`(列宽 `widthPx` + `widthChars`)/ `rows`(行高 `heightPt`)/ `mergedRegions`
  - `cells`:每格 `ref` / `type`(s/n/b/e/f/blank)/ `raw`(有缓存值时)/ `formula` / `formatted` / `style`
  - `images`(路径 + 锚点 + 尺寸 + 格式)/ `charts`(类型 + 锚点 + 标题)/ `shapes`(锚点 + 文字 + 样式)
  - `freezePanes` / `autoFilter`
  - `pageSetup`(纸张/方向/fitToPage/缩放/网格线/标题/打印区域/重复行列)
  - `margins`(英寸)/ `headerFooter`(奇偶/首页 页眉页脚)

## 单位约定

列宽 `widthPx`、行高 `heightPt`、字号 `sizePt`、页边距 `*In`(英寸)、图片锚偏移 `*Px`。值已归一化为可读单位。
