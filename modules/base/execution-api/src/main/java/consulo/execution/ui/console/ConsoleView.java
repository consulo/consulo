/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.ui.console;

import consulo.codeEditor.Editor;
import consulo.execution.ui.ExecutionConsole;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.BiPredicate;

/**
 * @see TextConsoleBuilder
 */
public interface ConsoleView extends ExecutionConsole {
  static Key<ConsoleView> KEY = Key.create(ConsoleView.class);

  void print(@Nonnull String s, @Nonnull ConsoleViewContentType contentType);

  void clear();

  void scrollTo(int offset);

  void attachToProcess(ProcessHandler processHandler);

  void setOutputPaused(boolean value);

  boolean isOutputPaused();

  boolean hasDeferredOutput();

  void performWhenNoDeferredOutput(Runnable runnable);

  void setHelpId(String helpId);

  void addMessageFilter(Filter filter);

  /**
   * Set filter for console message consumer. If predicate return true - message will be not print to console
   * @param filter
   */
  void setProcessTextFilter(@Nullable BiPredicate<ProcessEvent, Key> filter);

  @Nullable
  BiPredicate<ProcessEvent, Key> getProcessTextFilter();

  void printHyperlink(String hyperlinkText, HyperlinkInfo info);

  int getContentSize();

  boolean canPause();

  @Nonnull
  AnAction[] createConsoleActions();

  void allowHeavyFilters();

  default void requestScrollingToEnd() {
  }

  @Nullable
  default Editor getEditor() {
    return null;
  }
}