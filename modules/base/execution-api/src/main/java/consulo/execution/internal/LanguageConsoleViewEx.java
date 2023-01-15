/*
 * Copyright 2013-2023 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.internal;

import consulo.codeEditor.EditorEx;
import consulo.document.util.TextRange;
import consulo.execution.ui.console.ConsoleRootType;
import consulo.execution.ui.console.language.LanguageConsoleView;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 15/01/2023
 */
public interface LanguageConsoleViewEx extends LanguageConsoleView {
  @Nonnull
  String addTextRangeToHistory(@Nonnull TextRange textRange, @Nonnull EditorEx inputEditor, boolean preserveMarkup);

  void doAddPromptToHistory();

  @Nonnull
  String prepareExecuteAction(boolean addToHistory, boolean preserveMarkup, boolean clearInput);

  void addToHistory(String text);

  void installConsoleHistory(ConsoleRootType consoleRootType, String historyPersistenceId);
}
