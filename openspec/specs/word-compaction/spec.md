# word-compaction

## Purpose

定义 Word `WordIr` 序列化的默认紧凑形态与逃逸阀:run 合并、空 run 丢弃、空段压缩、NON_NULL;`--full`/`--pretty` 调试逃逸。零语义损失。

## Requirements

### Requirement: Word 默认序列化紧凑形态

默认(无 flag)序列化 `WordIr` 时,系统 SHALL 产出紧凑形态:单行 JSON;所有 `null` 字段 SHALL 省略(`NON_NULL`);相邻同有效格式(命名样式 ref + 直接覆盖)+ 同链接目标 + 无内嵌对象的 `TextRunIr` SHALL 合并为一个 run 并拼接 `text`;文本为空且无内嵌图的 run SHALL 丢弃;无 run 或 runs 全空的段落 SHALL 压缩为 `{style?, numbering?}`(保留结构意义,丢弃空字段)。默认仍输出全部区段。

#### Scenario: 相邻同格式 run 合并

- **WHEN** 一段含三个连续同格式(同样式、同直接覆盖、无链接、无图)的 text run,文本分别为 "Hel"/"lo"/" World"
- **THEN** 输出该段 `runs` 仅一个 text run,`text` 为 "Hello World"

#### Scenario: 链接边界不合并

- **WHEN** 两段同格式文本,前段无链接、后段有超链接
- **THEN** 两段为独立 run(不合并),链接 run 携带链接信息

#### Scenario: 空 run 丢弃

- **WHEN** 一段含一个空文本且无图的 text run(同段另有一非空 run)
- **THEN** 输出该段 `runs` 不含该空 run

#### Scenario: 空段压缩

- **WHEN** 一段无 run(或 runs 全空)但带样式 S1
- **THEN** 输出该段对象为 `{style:"S1"}`,不含 `runs`/`align` 等空字段

### Requirement: 紧凑形态无语义损失

默认紧凑形态 SHALL 与完整 IR 语义等价:run 合并仅拼接同格式相邻文本(可还原)、空 run 丢弃无信息、空段压缩保留 style/numbering、`NON_NULL` 仅省 null。`--full` + `--pretty` SHALL 能恢复原始 run 边界与空 run(除逐字节 null)。

#### Scenario: 合并可还原

- **WHEN** 默认紧凑形态合并的 run 文本为 "Hello World"
- **THEN** 该 run 的样式与覆盖字段反映合并前各 run 的共同有效格式(本就相同)
- **AND** 无独立信息丢失

### Requirement: 逃逸阀开关

系统 SHALL 提供两个正交序列化层开关:`--pretty` 恢复缩进;`--full` 恢复原始形态(不合并 run + 保留空 run + 完整段落字段)。两开关正交,可组合。系统 SHALL NOT 提供逐字节复现含显式 null 的开关(`NON_NULL` 固定)。

#### Scenario: --full 不合并 run

- **WHEN** 执行 `--full` 于含三个相邻同格式 run 的段
- **THEN** 输出该段 `runs` 仍为三个独立 run(未合并)

#### Scenario: --pretty 缩进且 NON_NULL 固定

- **WHEN** 执行 `--pretty`
- **THEN** 输出为缩进 JSON
- **AND** null 字段仍被省略
