---
name: markdown-to-docx
description: 将 Markdown 文档转换为真实 Word .docx 文件，适用于需要把 PRD、需求文档或产品产物包导出为 Word 的场景。
---

# Markdown to DOCX

将一个 UTF-8 Markdown 文件转换为真实 `.docx` 文件。

## 使用方式

运行脚本：

```bash
python .claude/skills/markdown-to-docx/scripts/md2docx.py <input.md> <output.docx>
```

## 输入

- `<input.md>`：Markdown 文件路径。
- `<output.docx>`：输出 Word 文件路径。

## 输出

- 一个真实的 Word `.docx` 文件。

## 依赖

需要 Python 包 `python-docx`。如果缺失，先安装：

```bash
pip install python-docx
```

## 规则

- 保持中文 UTF-8 内容。
- 保留标题、列表、表格、代码块和基础加粗/斜体格式。
- 不要把 Markdown 文本直接写入 `.docx` 后缀文件，必须通过脚本生成 Word 文档。
