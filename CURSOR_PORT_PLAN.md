# CursorJ port plan

Port every feature of CursorJ (`/run/media/vistall/repositories/JetBrains/CursorJ`, ~19k lines Kotlin, Swing) into
Consulo. Rule of the split: whatever works for any ACP agent (or any AI provider) goes to the platform
(`ai-api` / `ai-acp-api` / `ai-impl`, uni UI on AWT/Qt/web); only Cursor's own wire extensions, binary, login and
`~/.cursor` files go to the plugin `consulo-cursor`.

Status legend: **done**, *next*, open.

## 0. Already done (2026-11-13)

- `ai-acp-api`: session config options (`AcpConfigOption`/`AcpConfigValue`, `config_option(s)_update`,
  `session/set_config_option`, `models` + `session/set_model` fallback); models and per-model efforts come from the
  agent and are applied before every turn; prompt `usage`; `clientInfo.version` (Cursor refused `initialize`
  without it - the generic ACP type could never talk to Cursor); `_meta` client capabilities hook;
  `prepareAgent` / `configureSession` hooks; `onSessionRequest` / `onSessionNotification` / `appendText`;
  extension handlers may answer their own JSON-RPC error code; cancelling the chat future cancels the agent turn.
- `consulo-cursor`: `agent acp` (PATH, `cursor-agent`, installer paths, account override), `cursor_login`,
  `_meta.parameterizedModelPicker`, mode option (agent/plan/ask) set per turn, settings page (executable, mode,
  `agent status --format json`, Log In through `agent login` with `NO_OPEN_BROWSER`, Log Out),
  `cursor/ask_question` -> `onQuestion`, `cursor/create_plan` -> plan text in the answer, todos/tasks acknowledged.
- Verified on web: account page shows "Signed in as ...", provider listed with icon.
- **Phase 1 (protocol) done**, verified on web against agent 2026.11.11: chat answers, Cursor's title names the
  chat, the model picker lists Cursor's 48 models from the session options, the chat list comes from
  `session/list`, a restored chat replays its history with `session/load` and continues after an IDE restart.
  - `ai-api`: `AIContent.Thinking/ResourceLink/Resource/ToolCall`, `AIToolCallContent`, `AIToolCallStatus`,
    `AIPlanEntry(+Status/Priority)`, `AICommand`, `AIContextUsage`, `AISessionOption(+Value)` (moved from ACP),
    `AIChatSessionListener`; `AIStreamListener.onToolCall/onPlan/onContextUsage`; session options, commands and
    their listener live on `AIProviderType` keyed by session id (not on `AIProviderTypeWithChats`, which is only the
    list / restore API).
  - `ai-acp-api`: every `session/update` kind, replay keeps thoughts and merges tool-call updates, `modes` and
    `models` synthesized as options (`session/set_mode`, `session/set_model`), multi-block prompts gated on
    `promptCapabilities`, first-answer watchdog (90 s), `reportToolCall/findToolCall` for extensions, error
    `data.message`, warning when an agent exits on its own.
  - Fixed an old bug: the agent key used `GeneralCommandLine.getEnvironment()` whose map has no `toString`, so every
    call started a new agent process (sessions were never found again).
  - Plugin: todos -> `onPlan`, `cursor/task` -> tool calls of kind `task`, account mode only seeds a new session.
  - Transcript stores the new content kinds; the chat panel names agent tool calls (cards come in phase 2).

- **Phase 2 (chat UI) mostly done**, verified on web with a real Cursor turn (create + edit a file, run a command):
  - answers interleave text, collapsible thinking and tool-call cards (status icon, project-relative title, file
    link, output under Show/Hide) - `AIChatAnswer`, `AIChatToolCallView`;
  - changes are a `+N -M` box per card and a summary box per answer listing the changed files; a file opens in the
    platform diff viewer as a chain over all changed files (user decision: no inline diff text) - `AIChangesViewer`
    service in ai-api, implemented in the new module `ai-editor-impl` (diff-api stays out of ai-impl);
  - Stop button (`AIChatSession.cancel()` reaches the provider), messages sent while answering queue up
    (send now / remove), plan panel, token + context usage line, mode button and the session options of the model
    (context, fast, thinking) in the model popup, model/effort set on the session at once;
  - history replay shows the same cards; Cursor's malformed created-file diff (`-- /dev/null` / `++ b/...`) is
    normalized; web code blocks scroll inside the message.
  - still open in phase 2: permission card with arguments/diff and the agent's own options, interactive permissions,
    actions (focus chat, model picker, cycle effort), AWT/Qt check of the new parts.
- **Phase 3 started**, verified on web: "Add to AI Chat" in the editor popup / floating code toolbar (the selection
  with its lines, or the file) and in the project view popup (files) - actions in `ai-editor-impl`, going through
  the `AIChatContextManager` project service of ai-api; the attachment shows as a removable chip over the message
  box of the chat in front (else the one opened last, else a new chat of the default account); the message goes as
  text + `resource_link` / `resource` blocks (`AIChatSession.send(List<AIContent>)`), Cursor answered about the
  attached selection. Update of an editor action runs off the ui thread, so it must not read the selection.
- **Chat box is an editor now** (user decision): `AIChatInputFactory` / `AIChatInput` seam in ai-api, implemented
  in ai-editor-impl on `EditorBox` (plain text) - the completion of the platform offers the commands of the agent
  after `/` (verified: Cursor's 48 commands with descriptions, filtering with Ctrl+Space works) and the files of
  the project after `@` through `FilenameIndex`; `@path` in a sent message goes as a `resource_link`. Enter sends,
  Shift+Enter breaks the line, Up/Down walk the sent messages - actions on the editor (`AnActionWithUIUpdate`,
  disabled while a lookup is active; `consulo.ui.ex.internal` now exported to `consulo.ai.editor.impl`).
  A `TypedHandlerDelegate` is never called for an `EditorBox` document, so a document listener schedules the
  auto-popup. MessageInput completion/history hooks were started and reverted - not needed.
  Open: (1) the automatic popup after `/` closes as soon as more characters are typed (Ctrl+Space then filters
  fine); (2) `@` found nothing in the test sandbox - Go to File finds nothing there either, so the project of the
  sandbox has no file index (environment, recheck on a healthy project); (3) Up/Down history not verified yet;
  (4) drag & drop / paste of files and images, the active file.
- Chat box layout (user request): `EditorBoxStyle.INPLACE` (new uni style, AWT empty border, Qt frame fills its
  base colour instead of the line-edit panel, web `consulo-editor-box-inplace`) - the borderless editor fills the
  top, the buttons (attach image, working directory, mode, model, stop, send) are a fixed row under it.
  `BorderBuilder` does not help here: it only manages the extra hairlines of a uni component, not the native frame.
- Images, verified on web: "Attach Image" (png/jpg/gif/webp, up to 10 MB) adds an `AIContent.Image` chip; ACP sends
  it as an `image` block (Cursor `promptCapabilities.image = true`) - Cursor described the test image correctly.
  Open: paste / drop of a screenshot (clipboard and drop hooks per frontend).
- Images from the agent: `AIStreamListener.onImage`, ACP `agent_message_chunk` / replay with an `image` block ->
  `AIContent.Image`, shown inline in the answer (`ImageBox`, scaled to at most 480 px wide). Verified on web with a
  fake ACP agent (scratchpad `fake_acp_agent.py`) through the generic ACP account - which also proves the generic
  type connects now.
- Tool-call images (`AIToolCallContent.Image`, e.g. a screenshot tool) show in the tool card, expanded by default.
  Verified on web with the fake agent.
- Pasted images: `Clipboard.addPasteListener` + `ClipboardFeature.PASTE_LISTENER` (web: a document paste listener
  reads the image file and sends it back as `consulo-image-paste`); desktop takes over the paste shortcut of the box
  and reads `DataTransferType.IMAGE` (AWT imageFlavor / Qt `QMimeData.hasImage` -> PNG) or image files, else pastes
  as always. Verified on web (a "Pasted image" chip, Cursor named the colours). AWT/Qt not verified yet.
- Sent messages show their images as thumbnails (live and from history); an image attachment needs no label.
  Cursor's `session/load` replay does not send the user's images back - `user_message_chunk` images are parsed
  anyway for agents that do.
- Web fix on the way: `WebFocusTracker` had a plain `focusin` listener next to a debounced one of the same type on
  the frame root - the plain one was never called, so `hasFocus()` on web never followed a focus change. The scope
  update now lives in the single plain `focusin` listener.
- Permission card (rows 18/19), verified on web with the fake agent: `AIToolPermissionRequest` carries the agent's
  options (`AIToolPermissionOption` + kind), the tool-call content and an `interactive` flag; the decision may name an
  option (`AIToolPermissionDecision.option`). The card shows the description/text, a `+N -M` button into the diff
  viewer, Show/Hide Arguments (pretty JSON), one button per agent option (else Allow / Allow for This Chat / Deny)
  and "Run Everything" for the rest of the chat. A question of the agent (`kind: other` / `ask_question` ids) shows
  its answers and is never auto-approved.
- Actions (row 42): `AI.FocusChat`, `AI.OpenModelPicker`, `AI.CycleEffort` - no default shortcuts (Alt+C is the
  Code menu mnemonic here; CursorJ leaves the other two unbound too). Built, not clicked yet.
- Watchdog (row 6): a running tool call without news for 30 s shows "X is running for N s" as the writing status
  (`AIStreamListener.onStatus`), fails the turn after 5 min; a turn the agent is silent in for 15 min fails (idle,
  not absolute like CursorJ). Time stands still while the agent waits on the user (permission / extension requests).
  Built, the 30 s note not yet seen.
- Drop (row 24): files and images dropped on the chat box attach (image files as images, others as links). Built,
  not tested.
- Active file (row 25): a ticked checkbox with the name of the file worked on last sits in the chips row and goes
  with the message as a link. Verified on web that it shows. Found on the way: the unified editor window never told
  `selectionChanged` for another tab of the same window - fixed in `UnifiedFileEditorWindow`.
- **Phase 4 started** (built, not yet clicked through - the browser pane was hidden):
  - carryover (decision 5): `AcpAgent` knows the sessions it started which had no prompt yet (`isFresh`); a chat
    whose turn lands in one - the old session could not be loaded, or the chat started over - sends the end of the
    local history first (`AcpCarryover`, CursorJ's format: 16 messages / 12k chars, "Context from a previous chat:
    ... Current prompt:"). `session/load` replays strip it again, so the user sees what they wrote. Before this a
    chat which lost its session silently went on without any context.
  - context exhaustion (row 30): `resource_exhausted` / `context_length_exceeded` / "maximum context length" -> the
    chat starts over in a new session and the turn is sent once more with carryover; the user sees a status note.
  - edit & resend (row 29, decision 6): an Edit button on every message of the user puts it back into the box with
    a banner (Cancel); sending drops it and everything after it from the list and the session
    (`AIChatSession.startOver` -> `AIProviderType.startOver`, ACP: next prompt in a new session + carryover).
  - decision 4 done: `AIChatEntry.SESSION_ID` (empty = the chat itself); the agent tells a new session through
    `AIChatSessionListener.sessionChanged`, the store keeps it, a chat opened later talks to it. The platform keeps
    its transcript for every chat now (also Cursor's) - a moved chat loads it instead of the replay, which knows
    only the new session. Sessions a chat moved to are hidden from the provider's chat list; deleting a chat
    deletes both sessions.
  - Undo turn (row 28): `AIChatCheckpointManager` / `AIChatCheckpoint` in ai-api, Local History system labels in
    ai-editor-impl ("AI chat before: ..."); every turn gets one, the answer of the last completed turn an Undo
    button which (after a confirmation) reverts the files under the working directory. An interrupted turn offers
    none, as in CursorJ.
  - all of phase 4 is built, nothing clicked through yet (browser pane hidden).
- **Phase 5 started** (built, not tested - browser pane hidden):
  - IDE MCP server for every ACP session (decision 1, row 9): `AIMcpServer` + `AIMcpServerProvider` EP in ai-api,
    implemented in mcp-server-impl while the server is enabled (off by default - Settings | AI | MCP Server). The
    agents sign in with a token of their own (in memory, new per IDE start; principal "AI chat agent"), accepted
    without the static token. `session/new|load|resume` pass it as an http server when the agent has
    `mcpCapabilities.http` (Cursor 2026.11.11: http + sse).
  - Client files (decision 9, row 31): `fs.readTextFile/writeTextFile = true`; `AIAgentFileSystem` (ai-api) /
    `AIAgentFileSystemImpl` (ai-editor-impl): a file of an open project is read from its document (unsaved text
    included, `line`/`limit` honoured) and written through it in an undoable command, then saved; other files go
    to the disk. Writes outside the working directory of the session are refused (`-32602`).
  - Terminal (row 32): not done - Cursor never calls `terminal/*`.
- **Verified on web 2026-11-14 against Cursor:**
  - IDE MCP server: a new chat lists the server `consulo` and its tools (the sandbox needs another port - the
    user's own Consulo holds 64342).
  - Cursor never calls `fs/*` either, even with the capability declared (probe: it edits the disk itself). The
    documents route stays for other ACP agents. Because of that the IDE did not see Cursor's files: the chat now
    refreshes the paths of a finished tool call and, at the end of a turn, the working directory
    (`AIChatFileRefresher`).
  - Undo turn: reverts Cursor's edit. Found and fixed data loss - files made outside the IDE before the turn were
    unknown to the local history at the label, so going back deleted them; the checkpoint now refreshes the working
    directory first and the turn starts once the label is in place (`AIChatCheckpointManager.create` is async).
  - Edit & resend: the edited question went to a new session, Cursor quoted the first message of the chat from the
    carryover. A moved chat keeps its title now (the new session named it after the carryover). Cursor's replay
    writes attachments as "Additional ACP context: [ACP resource_link] ..." - the plugin turns them back into links
    (`CursorReplay` through the new `AcpAIProviderTypeBase.replayed` hook).
  - Permission card, interactive questions, active file chip, paste, images: verified earlier the same day.
  - Not yet clicked: the 30 s note, drag & drop.
- **Verified on AWT 2026-11-14** (Xvfb sandbox, scratchpad `run-awt.sh`): Cursor edit turn with tool card, `+N -M`,
  changed files list, diff tab; Undo button on the last answer; image paste from the X clipboard (Cursor named the
  colours); Find Action "Cycle AI Effort Level" / "Open AI Model Picker" (popup with Cursor's models); restored chat
  with attachment links. Fixed on AWT: a uni VerticalLayout stretches a part over the width, so the changed-files
  button and images sat in the middle - both are wrapped in a left row now.
- Chat box polish: soft wraps and no horizontal scrollbar in the box; web inplace box has no rounded focus ring;
  a too long component part in a message (a command title) is cut instead of making the list scroll sideways.
- Answers keep one Thinking block at their top for all the reasoning of the answer (user remark: a block before
  every tool call buried the answer). It lives only while the answer is written - `AIChatAnswer.finish()` drops it,
  and history replay never shows it (user remark: no Thinking left after the thinking). The transcript keeps it.
- Terminal, verified 2026-11-13: Cursor never calls `terminal/*`, even with `terminal: true` - it runs commands
  itself after `session/request_permission` and reports `rawOutput {exitCode, stdout, stderr}`, which the card shows.
  Client terminals (row 32) only matter for other ACP agents.

Bugs found while testing (not Cursor code):
- Web sandbox: opening a plain folder fails - `FolderProjectOpenProcessor` -> `ModuleRootManager is not bind`
  for the module created by `modifiableModel.newModule` (HEAD 677719767b0 + pulled 457c7337e05).
- Web sandbox: Settings OK shows "Cannot save scheme 'null'. Can not set a null value for an Attribute".
- consulo.ruby `RailsHighlightPassFactory` asserts the UI thread from the daemon -> log grows by GBs per minute.

## 1. Decisions

1. **No CursorJ indexing subsystem** (SQLite lexical index, PSI walk "symbol index", embedding-less "semantic" index,
   hybrid ranker, freshness, `fs/find_text_in_files`, invented `editor/*` methods). Its retrieval matches the first
   500 chars of the prompt as one substring, so for natural language it returns nothing. Instead every ACP session
   gets the IDE's own MCP server (`mcpServers: [{type: http, url: .../mcp, headers: token}]`), which already exposes
   search, symbol, references, diagnostics, VCS, build and refactoring toolsets backed by the real indexes.
2. **Tool calls are a new content kind, not `ToolUse`.** `AIContent.ToolUse` stays for IDE tools the platform runs
   itself (`AIChatSession` tool loop). Agent-run tools become `AIContent.ToolCall` snapshots
   (id, parentId, title, kind, status, locations, content = text | diff | terminal, rawInput); the stream sends the
   whole snapshot on every update and the UI replaces by id. Cursor subagents (`cursor/task`) are tool calls of kind
   `task` - no separate task model.
3. **Session options are generic.** `AISessionOption` (mirror of `AcpConfigOption`) + `AIProviderTypeWithChats`
   `getSessionOptions/setSessionOption`; chosen values persist per chat (`AIChatEntry.OPTIONS`). Mode, context, fast,
   thinking render in the chat footer from whatever the agent offers. Account-level options (Cursor mode, Claude and
   Codex permission modes) become the default of a new chat.
4. **Chat id and session id split.** `AIChatEntry.SESSION_ID` keeps the agent session behind a chat. It changes on
   edit & resend, context exhaustion, an unloadable session; the chat (tab, title, group) stays. Replaces the
   in-process alias map of `AcpAgent`.
5. **Carryover is platform logic.** When a chat moves to a new session, the first prompt carries the local transcript
   (last 16 messages / 12k chars, CursorJ's format). The platform keeps its own transcript for every chat, also for
   providers which keep chats (needed for carryover and edit).
6. **Edit & resend = new session + carryover** (ACP has no `session/fork`; Cursor does not advertise it). Use
   `session/fork` when an agent advertises it.
7. **Debug mode is a platform pseudo-mode** ("Debug" next to the agent's modes): hidden prompt + per-turn log file
   and a localhost endpoint on the built-in web server (not `com.sun.net.httpserver`); wire mode stays `agent`.
8. **Plan mode Build/View Plan are generic**: shown after a turn whose session option `mode` was `plan` and the turn
   produced a plan (ACP `plan` update, or a provider-reported plan file). Cursor contributes the plan file location.
9. **Writes go through Documents.** ACP `fs/read_text_file` returns unsaved Document text, `fs/write_text_file`
   writes the Document in a command (undoable, Local History, live editor update). Requires declaring
   `fs.readTextFile/writeTextFile = true`; ai-acp-api gets an `AcpClientHandler` SPI implemented in ai-impl so it
   stays free of document/editor deps.
10. **Terminal** `terminal/*` runs through `ProcessHandlerBuilderFactory`; the tool-call card embeds a console for
    the terminal id. Shell = platform terminal settings.
11. **Editor-side work lives in new modules `ai-editor-api` / `ai-editor-impl`** (package `consulo.ai.editor`), so
    `ai-api` / `ai-impl` stay free of document, editor, diff and local-history dependencies:
    - `ai-api` gains the seams: an `AIChatContextManager` service (add a file / selection / image to the active chat
      of a project, implemented in ai-impl) and an `AIToolCallPresenter` extension (renders tool-call content in a
      card - a diff, a terminal - with a plain-text fallback in ai-impl);
    - `ai-editor-impl`: "Add Selection / File to AI Chat" actions (editor popup, floating code toolbar, project view),
      auto-attached active file, diff presenter (diff-api request panel, open at line, highlight inserted lines),
      terminal presenter (execution console), Local History checkpoints for Undo turn, plan file editor sync, and
      the ACP client handler for `fs/*` over Documents (`WriteCommandAction`) and `terminal/*`.
    JetBrains intellij-community has no ACP client or AI chat (AI Assistant is closed); the only related code is
    `plugins/mcp-server/mcpserver.skills` (bundled skills installed into agents, `AgentSkillTarget`), worth a small
    port next to rules/skills (row 34).
12. Not ported: CursorJ's tab manager, history popup, transcript/prompt stores, `CentralStorage` (chat tree + editor
    tabs + `AIChatStore` already cover them), paste-interceptor hack, global status singleton, raw ACP logging
    (use a `consulo.ai.acp` debug log category), dead code (`AUTO_APPROVE_SAFE`, `editor/apply_edit` stub),
    `agent models` CLI (ACP options supersede it), `defaultModel` setting (never reached the wire).

## 2. Feature map

| # | CursorJ feature | Target | Phase |
|---|---|---|---|
| 1 | stdio JSON-RPC, error `data.message` surfaced | ai-acp-api `AcpConnection` | 1 |
| 2 | handshake, `cursor_login`, `_meta` | done (plugin) | 0 |
| 3 | reconnect after agent exit | ai-acp-api: dead agent restarts on next use (done); turn fails with stderr tail; session reopened via load/resume/carryover | 1 |
| 4 | `session/new/load/prompt/cancel/set_mode/set_config_option/set_model` | ai-acp-api (`set_mode` for agents with `modes` but no mode option) | 1 |
| 5 | `agent_message_chunk`, thoughts, `tool_call(_update)`, `plan`, `current_mode_update`, `available_commands_update`, `usage_update`, `session_info_update`, `user_message_chunk` replay | ai-acp-api -> new `AIStreamListener` callbacks | 1 |
| 6 | watchdogs (first update 90 s, tool call stuck 30 s warn / 5 min fail, turn 15 min) | ai-acp-api, warnings as status | 1 |
| 7 | session not loadable before first prompt (Cursor) | ai-acp-api: remember prompt-less sessions | 1 |
| 8 | multi-block prompt (text, `resource_link`, `resource`, `image`) gated on `promptCapabilities` | ai-api `AIContent.ResourceLink/Resource`, ai-acp-api | 1 |
| 9 | IDE MCP server in `mcpServers` | ai-acp-api + mcp-server-impl provider | 5 |
| 10 | chat tabs, history, rename, restore | covered by chat tree + editor tabs | - |
| 11 | stop button, send while busy = queue (edit / send now / delete rows) | ai-impl `AIChatPanel` | 2 |
| 12 | tool-call cards: verb + file link + line range, status, collapsible diff (+N/-N, open at line, highlight inserted lines) | ai-impl, `MessageListItem.setParts` components; diff via diff-api request panel | 2 |
| 13 | thinking (collapsible) | ai-impl | 2 |
| 14 | plan / todo panel | ai-impl footer-top panel, `onPlan` | 2 |
| 15 | subagent tasks strip | ai-impl, tool calls of kind `task` | 2 |
| 16 | per-turn usage + context bar (thresholds 75/90/95, cost) | ai-api `AIUsage` + context/cost, ai-impl | 2 |
| 17 | model picker sections: model (Auto first), effort radio, toggles (thinking, fast), context; mode dropdown | ai-impl, session options | 2 |
| 18 | permission card: args (pretty JSON), diff, agent's own options, "run everything" per chat | ai-api request gets content + options, ai-impl | 2 |
| 19 | interactive permission (`toolCall.kind == other` / `ask_question` option ids) never auto-approved | ai-acp-api | 2 |
| 20 | `cursor/ask_question` card | done (`AIChatQuestionPrompt`) | 0 |
| 21 | plan questions recovered from prose (A)/1. option runs) | ai-impl, plan mode only - `AIChatPlanQuestionParser` (CursorJ rules + a numbered question line over lettered choices) shown with `AIChatQuestionPrompt`, answers go as the next prompt, typing a message drops it, no Build while questions wait - **done, AWT-verified with Cursor** | 6 |
| 22 | attachments: chips row, images, selection blocks, `@file` pills | ai-impl + `MessageInput` attachment API (3 frontends) | 3 |
| 23 | "Add Selection to AI Chat" (editor popup, floating code toolbar), "Add File" (project view) | ai-impl actions | 3 |
| 24 | drag & drop files/images, paste images | `MessageInput` drop/paste hooks (3 frontends) | 3 |
| 25 | auto-attach active file | ai-impl setting | 3 |
| 26 | `/` commands (+ skills), `@` skills/files completion | `MessageInput` completion SPI (3 frontends) | 3 |
| 27 | prompt history Up/Down with draft | `MessageInput` history hook | 3 |
| 28 | Undo All (Local History label per turn, revert, interrupted turns blocked) | ai-impl | 4 |
| 29 | edit & resend | ai-impl + decision 4/5/6 | 4 |
| 30 | context exhaustion -> new session + resend with carryover | ai-impl | 4 |
| 31 | `fs/read_text_file`, `fs/write_text_file` (+ protect writes outside workspace) | ai-acp-api SPI + ai-impl | 5 |
| 32 | `terminal/*` | ai-acp-api SPI + ai-impl | 5 |
| 33 | global user rules (hidden block every prompt) | ai-impl AI settings (Settings > AI > Rules, `AIChatUserRules`) - **done, AWT-verified** | 6 |
| 34 | rules + skills of `.cursor/.claude/.codex/.agents` (list, add from template, edit, remove) | ai-impl "Agent Instructions" page (tabs: instruction files / rules / skills); `AIAgentInstructionProvider` EP - platform `AGENTS.md`+`.agents/skills`, Cursor `.cursor/rules`+skills, Claude `CLAUDE.md`+`.claude/rules`+skills, Codex `.codex/skills`; global skills listed, not removable - **done, AWT-verified** | 6 |
| 35 | Debug mode (hidden prompt, log file, endpoint, I reproduced it / Clean up / View logs) | ai-impl, decision 7 - `AIDebugLogManager` (log `.consulo/ai-debug/<chat>.log`, fresh on entering Debug), `AIDebugLogRequestHandler` (`/ai-debug/log|logs/<chat>/<token>` on the built-in server), hidden `<mode_instructions>` via `AIChatHiddenContext` - **done, AWT-verified with Cursor** | 6 |
| 36 | Plan mode Build / View Plan, plan editor sync | ai-impl + Cursor plan file locator - hidden plan instructions + Build (switch to the first editing mode, "Implement the plan above.") **done, AWT-verified with Cursor**; View Plan waits for #38 | 6 |
| 37 | MAX mode (`~/.cursor/cli-config.json` `maxMode` + `model.maxMode`, restart agent, 10 s poll) | plugin - `CursorMaxMode` + a synthetic `cursor.maxMode` toggle through the new `AcpAIProviderTypeBase.sessionOptions` hook; setting it writes the config and stops the agent, the next turn starts it and reopens the session; session listeners are now per account and re-attach to a new agent process (`AcpSessionListenerRegistration`); no poll (the file is read when options are shown) - **done, AWT-verified** | 6 |
| 38 | `cursor/create_plan` path hint, "Plan saved to file:" detection, `.cursor/plans/*.md` | plugin - `AcpAgent.onSessionUpdate` observer + `AIStreamListener.onPlanFile`; Cursor parses the `Plan saved to file:` tool-call text (plans go to `~/.cursor/plans/<name>-<id>.plan.md`); chat shows View Plan next to Build - **done, AWT-verified** | 6 |
| 39 | `_cursor/update_todos` -> `onPlan` | plugin | 1 |
| 40 | `cursor/task` (request + notification) -> tool call `task` | plugin | 1 |
| 41 | Windows `.cmd`/`.bat` agent via `cmd.exe /c` | plugin | 1 |
| 42 | actions: focus AI Chat, open model picker, cycle effort | ai-impl | 2 |
| 43 | connection/model status | chat footer + status line; status bar widget optional | 2 |
| 44 | markdown extras (task lists, autolinks), highlighted code blocks | ui-impl markdown (commonmark extensions); highlighting later | 2 |
| 45 | hidden prompt strings (plan prefix, debug prefix, carryover, model switched, selection) | ported verbatim into localize YAML | 2-6 |

## 3. Phases

1. *Protocol completeness* - ai-api stream callbacks (`onToolCall`, `onPlan`, `onUsage`, `onCommands`,
   `onSessionOptions`, thinking), `AIContent` (`ToolCall`, `Thinking`, `ResourceLink`, `Resource`), `AIUsage`
   context/cost; AcpAgent handles every `session/update` kind, replay keeps thoughts and tool calls, multi-block
   prompts, error `data.message`, watchdogs, prompt-less sessions; plugin: todos, tasks, Windows wrapper.
2. *Chat UI core* - stop, queue, tool-call cards with diff, thinking, plan panel, tasks strip, usage, session
   options in the footer + model picker sections, permission card, actions; transcript v2 for the new content kinds.
3. *Context* - `MessageInput` attachments / completion / history / drop / paste on AWT, Qt, web, headless;
   selection + file actions; active file; `@` and `/`.
4. *History editing* - session id split, carryover, edit & resend, Undo turn, context exhaustion.
5. *Client capabilities* - fs through Documents, terminal, IDE MCP server.
6. *Modes & instructions* - rules, skills, Debug mode, Plan Build/View, plan questions from prose; Cursor MAX mode
   and plan files.

7. *Provider parity* - consulo-codex emits `onToolCall` (`commandExecution` + output, `fileChange` diffs,
   `mcpToolCall`, item status), `onPlan` (plan updates), `onContextUsage` (token usage), permission mode as a session
   option; consulo-claude parses `tool_use` / `tool_result` / `TodoWrite` / usage from stream-json the same way. The
   chat UI is provider-neutral, so nothing else changes.

Each phase: platform `mvn package`, sync changed jars for the plugin, web sandbox first (park consulo.ruby), then
AWT/Qt parity.

## 7a. Claude over the CLI - done 2026-11-14 (consulo-claude, uncommitted), verified on AWT with claude 2.1.281

Modelled on Zed's `claude-code-acp` (cloned to the session scratchpad `ref/`; it drives the CLI through the Agent SDK).
- Prompt: content blocks - text, `image` (base64 source), file link as `[@name](uri)`, selection as the link in place
  plus a trailing `<context ref="uri">...</context>` block (adapter format). Restored chats turn those back into
  attachments; a carryover block is stripped (`AIChatCarryover`, moved to ai-api from ai-acp-api and shared).
- Tool calls from the stream-json `assistant` (`tool_use`) and `user` (`tool_result` + `tool_use_result`) events
  (`ClaudeToolCalls`): titles/kinds per tool, Edit/MultiEdit/Write diffs made whole from `tool_use_result.originalFile`,
  Bash output from `stdout`/`stderr`, failed calls, subagent parent from `parent_tool_use_id`.
- Plan: `TodoWrite` (in-progress shows `activeForm`) and the newer `TaskCreate`/`TaskUpdate` (id from
  `tool_use_result.task.id`, fallback "Task #N created"); `ToolSearch` hidden.
- Status: `system/task_summary` -> `onStatus`; context + cost from `result.usage.iterations[last]` and
  `modelUsage.*.contextWindow`, `total_cost_usd`.
- IDE MCP server via `--mcp-config` JSON (http + headers) - verified "consulo: Connected". The provider now gives
  the server only while it is bound (another process may hold the port).
- Edit & resend: the adapter forks with `forkSession(upToMessageId)`; we copy `~/.claude/projects/<dir>/<id>.jsonl`
  up to the edited prompt under a new id (`ClaudeChats.fork`, counted by real user prompts) and `--resume` it -
  verified, Claude quoted the first message from the kept history. Carryover only when there is nothing to copy.
  `AIProviderType.startOver` takes the kept history for that.
- Restored chats: tool cards rebuilt from the transcript (`toolUseResult`), one answer per turn.
- Not done: live partial tool input (`input_json_delta`), ExitPlanMode/plan mode, thinking settings, Codex parity.

## 7b. Codex over the app server - done 2026-11-14 (consulo-codex, uncommitted), verified on AWT with codex-cli 0.165.0

Zed's `codex-acp` links codex-core directly (no app-server, no fork/rollback) - used for titles, approvals, MCP keys
and usage only. Item shapes taken from `codex app-server generate-json-schema` and a live probe.
- Items -> tool calls (`CodexItems`): `commandExecution` (title from `commandActions`: read/listFiles/search/command;
  live output from `item/commandExecution/outputDelta`, `aggregatedOutput` + `exitCode` at the end), `fileChange`
  (per file; update hunks split into before/after, add/delete), `mcpToolCall` ("Tool: server/tool" + result text),
  `webSearch`, `imageView` / `imageGeneration` (the image file), `collabAgentToolCall` (task).
- `turn/plan/updated` -> plan; `thread/tokenUsage/updated` -> context (`last.totalTokens` of `modelContextWindow`).
- Prompt: text, image as data url, file link as a native `mention` input, selection as link + `<context>` text.
- IDE MCP server in the thread config: `config.mcp_servers.<name> = {url, http_headers}`.
- Edit & resend: `thread/revert` with `beforeTurnId` of the turn of the edited prompt - the same thread goes on, no
  carryover needed. Verified: after the cut Codex denied ever being asked the removed question.
- Restored threads show tool calls (same mapper), mentions as attachments and pasted images (data urls) - verified.
- Approvals carry Codex's own decisions as options (accept, acceptForSession, acceptWithExecpolicyAmendment with the
  proposed prefix, decline, cancel) and, for file changes, the diff of the item - verified in "untrusted" mode,
  the prefix rule was accepted. The permission card wraps its buttons now (FlowLayout).
- Plugins now build properly (mvn package in consulo-claude / consulo-codex / consulo-cursor) against the platform
  jars synced into ~/.m2 (ai-api, ai-acp-api; unpushed platform changes).
- Not done: MCP elicitations, network policy amendments.
