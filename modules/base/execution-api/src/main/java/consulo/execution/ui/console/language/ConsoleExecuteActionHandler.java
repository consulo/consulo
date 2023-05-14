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
package consulo.execution.ui.console.language;

import consulo.document.DocumentReferenceManager;
import consulo.execution.internal.LanguageConsoleViewEx;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.undoRedo.ProjectUndoManager;

import jakarta.annotation.Nonnull;

public abstract class ConsoleExecuteActionHandler {

  private boolean myAddToHistory = true;
  private final boolean myPreserveMarkup;

  boolean myUseProcessStdIn;

  public ConsoleExecuteActionHandler(boolean preserveMarkup) {
    myPreserveMarkup = preserveMarkup;
  }

  public boolean isEmptyCommandExecutionAllowed() {
    return true;
  }

  public final void setAddCurrentToHistory(boolean addCurrentToHistory) {
    myAddToHistory = addCurrentToHistory;
  }

  protected void beforeExecution(@Nonnull LanguageConsoleView consoleView) {
  }

  public void runExecuteAction(@Nonnull LanguageConsoleView consoleView) {
    if (!myUseProcessStdIn) {
      beforeExecution(consoleView);
    }

    String text = ((LanguageConsoleViewEx)consoleView).prepareExecuteAction(myAddToHistory && !myUseProcessStdIn, myPreserveMarkup, true);
    ProjectUndoManager.getInstance(consoleView.getProject()).invalidateActionsFor(DocumentReferenceManager.getInstance().create(consoleView.getCurrentEditor().getDocument()));

    if (myUseProcessStdIn) {
      consoleView.print(text, ConsoleViewContentType.USER_INPUT);
      consoleView.print("\n", ConsoleViewContentType.USER_INPUT);
    }
    else {
      addToCommandHistoryAndExecute(consoleView, text);
    }
  }

  public final void addToCommandHistoryAndExecute(@Nonnull LanguageConsoleView consoleView, @Nonnull String text) {
    ((LanguageConsoleViewEx)consoleView).addToHistory(text);

    doExecute(text, consoleView);
  }

  public boolean isPreserveMarkup() {
    return myPreserveMarkup;
  }

  public void setUseProcessStdIn(boolean useProcessStdIn) {
    myUseProcessStdIn = useProcessStdIn;
  }

  public abstract void doExecute(@Nonnull String text, @Nonnull LanguageConsoleView consoleView);
}
