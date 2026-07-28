## Context

excel-ir 把 `.xlsx` 解析成对 LLM agent 友好的 JSON IR。当前管线:`WorkbookExtractor`(含落盘)→ `IrDocument`(record 集合)→ `JsonWriter`(Jackson `INDENT_OUTPUT`,全字段展开)。

现状问题:一张 8×15 简单表输出 ~8KB,冗余来自美化缩进(~35%)、空单元格全字段展开(~22%,44 个 blank)、冗余 null 字段(~15%)、`formatted==raw` 重复(~10%)、`widthChars` 与 `widthPx` 并存(~8%)。样式表已用 `S4→F4/B4` 引用去重(为最优部分,不动)。同时 CLI 无法只看区域或部分结构;且 `shrinkToFit`/对角线边框/填充底色等基础格式未提取。

无仓库内消费者依赖 null 字段存在(`JsonWriterTest` 仅断言 `formatted`/`ref`/`fonts.name`/`schemaVersion`)。

## Goals / Non-Goals

**Goals:**
- 默认序列化即经济形态(单行 + NON_NULL + blank 压缩 + 冗余省略),零信息损失。
- `--range`(A1 原生)空间裁剪、`--select` 区段投影,二者正交可组合。
- `cells` 自动样式链闭包,输出自洽可解;闭包源随区间收敛。
- 补 `shrinkToFit`/对角线边框/填充底色,schemaVersion→2。
- 提取、投影、序列化三层职责清晰、开关正交、各自可单元测试。

**Non-Goals:**
- 不做逐字节复现旧默认(含显式 null)的逃逸——无消费者依赖。
- 不做字段路径式投影(如 GraphQL 字段掩码),仅做命名区段投影。
- 不把 `--range` 下沉到提取层做判交落盘(保持分层)。
- 不引入新 record 类型表示「投影后 IR」——复用现有 record。

## Decisions

### D1: 改默认为紧凑形态,而非 opt-in flag

**选:** 默认即紧凑。**否:** `--compact` opt-in。
**为什么:** 使命是「LLM 友好」,LLM 应开箱即得经济输出,而非它得先学会传 `--compact`。blast radius 仅一个测试断言(`JsonWriterTest.cells[0].formatted`,该 cell `formatted==raw` 故新默认会省略),无外部消费者依赖。逃逸阀 `--full-cells`/`--pretty` 复现旧形态供调试与测试。

### D2: 投影严格语义,cells 非隐式常开

**选:** `--select` 严格只返命名区段。**否:** cells 始终隐式带上。
**为什么:** 若 cells 隐式常开,`--select borders` 仍拖全部单元格,违背投影的省体积初衷。「只看边框」= `--select cells,borders` 显式且可预测。

### D3: cells 触发样式链全闭包

**选:** 选 cells 自动带出引用到的 xfs→fonts/fills/borders 全链。**否:** 悬空引用 / 自适应删字段。
**为什么:** `cell.style → cellXfs[].border → borders[]` 是引用链,部分带出会产生不可解输出。样式表本就小且去重(样本:7 fonts/5 borders/7 xfs),闭包子集更小,代价可忽略。要只看样式定义则显式 `--select borders` 不触发闭包。

### D4: --range 不强制 sheet,作用于所有被选 sheet

**选:** 无 `--sheet` 时区间裁每个被选 sheet。**否:** 缺 `--sheet` 报错。
**为什么:** 与现有「无 `--sheet` = 解析全部」语义一致;单 sheet 文件不强制写 sheet 名。

### D5: media 按锚点判交,图实体不切割

**选:** 锚点矩形 from..to 与区间矩形判交决定去留,保留图的 path/width/height/完整锚点。**否:** 切图像素 / media 不受区间影响。
**为什么:** 与 cells/merges 裁剪逻辑统一;锚点矩形本身完整,不破坏图实体。

### D6: 投影与区间正交合取,纯全局区段不受区间

**选:** 区间仅裁有空间坐标的区段(cells/merges/layout/media);`--select` 仅裁类别;同时给取交集。**否:** 互斥。
**为什么:** 两个维度真正正交。borders/fonts/fills/xfs/page 无空间归属,区间对它们天然无效,本就该全留。

### D7: 闭包源 = 区间内 cells

**选:** 先区间裁 cells,再对这些 cells 做闭包。**否:** 闭包源=全表 cells。
**为什么:** 大表有 50 种边框但某区域仅用 2 种,基于区间内 cells 闭包只带 2 种,体积最优且自洽。

### D8: 逃逸阀 `--full-cells`+`--pretty` 两正交小开关,放弃逐字节复现

**选:** `--full-cells`(单元格形态含冗余 formatted/formula/完整 blank)+ `--pretty`(缩进);NON_NULL/widthChars 去除/全null块省略为默认固定。**否:** `--legacy` 逐字节复现旧默认(含 null)。
**为什么:** 无消费者依赖 null 存在;逐字节复现无价值。两小开关职责最小、正交。

### D9: 9 词区段表,layout/page/media 聚合

**选:** `cells merges borders fonts fills xfs layout page media`。**否:** 粗粒度 `values`/`styles` 或极细粒度单到 margin/images。
**为什么:** pageSetup/margins/headerFooter 与 images/charts/shapes 几乎总配套看,聚合减负;dimensions/columns/rows 同属布局。其余原子。

### D10: 错误处理——格式严格校验 exit 2,空间越界宽容

**选:** `--range` 非法形态 / `--select` 未知词 → exit 2 + 诊断(区段附词表);区间空间越界 → 静默裁到实际尺寸不报。**否:** 静默容错 / 越界报错。
**为什么:** 格式错静默会产出「看似正常但残缺」的输出,最坑 LLM(无法自我纠错);越界裁剪符合「看这块有多少给多少」的宽容直觉,且无需先查 dimensions。

### D11: media 落盘照旧,裁剪只动 JSON

**选:** 图片提取阶段全部落盘;裁剪只决定 ImageIr 进不进 JSON,不回滚磁盘。**否:** 删盘 / 先判交再落盘。
**为什么:** 保持「提取落盘」与「裁剪纯数据」分层一致(D5/D6/D7 都假设裁剪是数据层操作);图片数量有限,落盘垃圾代价小。

### D12: 复用 record,提取与序列化间加纯数据投影层

**选:** 投影层输入完整 IrDocument + range + select,输出裁剪后 IrDocument(同类型);序列化层只管压缩,不感知 range/select。**否:** 序列化层内裁剪 / 引入 ProjectedDocument 新类型。
**为什么:** 两层正交开关各自可单元测试(`--range`/`--select` 属投影层,`--pretty`/`--full-cells` 属序列化层);record 不可变性让「序列化器边写边过滤」只能靠 if 跳字段,测试要从 JSON 字符串反推,难单元化。

### D13: 格式覆盖补 shrinkToFit+对角线+bgColor,schemaVersion→2

**选:** 三项一并补,版本升 2。**否:** 只补 shrinkToFit / 单开 change。
**为什么:** 三项同属样式表提取层,且与已定 `borders`/`fills` 投影区段咬合(补了 bgColor/对角线,投影出的样式子表才完整)。additive 兼容,旧消费者忽略新字段。

## Risks / Trade-offs

- [默认改动破坏旧调用方] → 仅一处测试断言受影响;提供 `--full-cells`/`--pretty` 逃逸,README 标注 BREAKING。
- [blank 压缩丢失空框架单元格样式] → 压缩形态保留 `style` 引用,样式链闭包(Q3/D3)保证可解;`--full-cells` 可恢复完整 blank。
- [闭包隐式带出样式子表与「严格投影」张力] → 闭包只作用于已选中的 `cells`(使被选数据自洽),非把区段强加于未选者;文档明确「选 cells ⇒ 带样式链」。
- [`--range` 多 sheet 输出多个裁剪 sheet 体积大] → 已裁剪,可接受;真要看单 sheet 配 `--sheet`。
- [对角线边框/填充底色增加 record 字段] → additive,schemaVersion 2 标记;旧消费者兼容。
- [区间空间越界静默裁剪可能误导] → 与「裁剪」语义一致,文档与 help 说明。

## Migration Plan

1. 实现样式覆盖扩展(record 加字段 + extractor 补行 + schemaVersion 2)。
2. 实现序列化层压缩(NON_NULL、blank 压缩、冗余省略、widthChars 去、全null块省略)+ `--pretty`/`--full-cells` 开关。
3. 实现投影层(range 裁剪 + select 过滤 + cells 闭包),CliArgs 新增解析,Main 接线。
4. 改 `JsonWriterTest`:断言改匹配新默认(`formatted!=raw` 场景或 `--full-cells`)。
5. 新增投影层、序列化压缩、区间、区段、闭包、错误处理单元测试。
6. 更新 README(字段速览、新 flag 用法、schemaVersion 2)。
7. `mvn clean package` 重新出 fat-jar;冒烟:对张三薪酬表跑默认、`--range`、`--select`、组合、逃逸阀各一遍。

**回滚:** 各层独立提交,任一层可单独 revert;默认改动可通过临时改回 `INDENT_OUTPUT`+全字段回滚(但 schemaVersion 2 与新字段不回退)。

## Open Questions

- 无。所有关键决策已在 grilling 中与用户逐条确认(D1-D13)。
