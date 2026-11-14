---
name: consulo-ide
description: Use the tools of the consulo MCP server - the indexes, inspections, refactorings and run configurations of the Consulo IDE the project is open in - instead of grepping and editing by hand, whenever code has to be found, understood, changed safely or checked.
---

# Working with the Consulo IDE

The project is open in Consulo, and its MCP server (`consulo`) answers from the indexes of the IDE. Its answers know the
language: a symbol is a declaration, not a string, and a rename changes every usage.

## Finding code

- `search_symbol` finds classes, methods and fields by name - prefer it to grepping for a declaration.
- `get_symbol_info` tells what a symbol at a position is: its declaration, type and documentation.
- `analyze_calls` lists the callers and callees of a method - use it before changing a signature.
- `search_text`, `search_regex` and `search_file` search the project files with the excludes of the IDE.

## Changing code

- `rename_refactoring` renames a symbol with all its usages, also in other files - never rename by search and replace.
- `reformat_file` formats a file with the code style of the project after an edit.

## Checking work

- `get_file_problems` reports the errors and warnings the IDE sees in a file - run it on every file you changed.
- `lint_files` runs the inspections on several files at once.
- `build_project` compiles the project; `get_run_configurations` and `execute_run_configuration` run the tests and
  applications the way the user does.

Every tool takes the path of the project; pass the working directory.
