/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.application.AllIcons;
import consulo.codeEditor.EditorEx;
import consulo.document.util.TextRange;
import consulo.execution.internal.LanguageConsoleViewEx;
import consulo.execution.ui.console.language.BaseConsoleExecuteActionHandler;
import consulo.execution.ui.console.language.ConsoleExecuteActionHandler;
import consulo.execution.ui.console.language.LanguageConsoleView;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupFocusDegree;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.EmptyAction;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Conditions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Predicate;

public class ConsoleExecuteAction extends DumbAwareAction {
  public static final String CONSOLE_EXECUTE_ACTION_ID = "Console.Execute";

  private final LanguageConsoleView myConsoleView;
  private final ConsoleExecuteActionHandler myExecuteActionHandler;
  private final Predicate<LanguageConsoleView> myEnabledCondition;

  @SuppressWarnings("UnusedDeclaration")
  public ConsoleExecuteAction(@Nonnull LanguageConsoleView console, @Nonnull BaseConsoleExecuteActionHandler executeActionHandler) {
    this(console, executeActionHandler, CONSOLE_EXECUTE_ACTION_ID, Conditions.<LanguageConsoleView>alwaysTrue());
  }

  public ConsoleExecuteAction(@Nonnull LanguageConsoleView console, final @Nonnull ConsoleExecuteActionHandler executeActionHandler, @Nullable Predicate<LanguageConsoleView> enabledCondition) {
    this(console, executeActionHandler, CONSOLE_EXECUTE_ACTION_ID, enabledCondition);
  }

  public ConsoleExecuteAction(@Nonnull LanguageConsoleView console, @Nonnull BaseConsoleExecuteActionHandler executeActionHandler, @Nullable Predicate<LanguageConsoleView> enabledCondition) {
    this(console, executeActionHandler, CONSOLE_EXECUTE_ACTION_ID, enabledCondition);
  }

  public ConsoleExecuteAction(@Nonnull LanguageConsoleView consoleView,
                              @Nonnull ConsoleExecuteActionHandler executeActionHandler,
                              @Nonnull String emptyExecuteActionId,
                              @Nullable Predicate<LanguageConsoleView> enabledCondition) {
    super(LocalizeValue.empty(), LocalizeValue.empty(), AllIcons.Actions.Execute);

    myConsoleView = consoleView;
    myExecuteActionHandler = executeActionHandler;
    myEnabledCondition = enabledCondition == null ? Conditions.<LanguageConsoleView>alwaysTrue() : enabledCondition;

    EmptyAction.setupAction(this, emptyExecuteActionId, null);
  }

  @RequiredUIAccess
  @Override
  public final void update(@Nonnull AnActionEvent e) {
    EditorEx editor = myConsoleView.getConsoleEditor();
    boolean enabled = !editor.isRendererMode() && isEnabled() && (myExecuteActionHandler.isEmptyCommandExecutionAllowed() || !StringUtil.isEmptyOrSpaces(editor.getDocument().getCharsSequence()));
    if (enabled) {
      Lookup lookup = LookupManager.getActiveLookup(editor);
      // we should check getCurrentItem() also - fast typing could produce outdated lookup, such lookup reports isCompletion() true
      enabled = lookup == null || !lookup.isCompletion() || lookup.getCurrentItem() == null || (((LookupEx)lookup).getLookupFocusDegree() == LookupFocusDegree.UNFOCUSED);
    }

    e.getPresentation().setEnabled(enabled);
  }

  @RequiredUIAccess
  @Override
  public final void actionPerformed(@Nonnull AnActionEvent e) {
    myExecuteActionHandler.runExecuteAction(myConsoleView);
  }

  public boolean isEnabled() {
    return myEnabledCondition.test(myConsoleView);
  }

  public void execute(@Nullable TextRange range, @Nonnull String text, @Nullable EditorEx editor) {
    if (range == null) {
      ((LanguageConsoleViewEx)myConsoleView).doAddPromptToHistory();
      myConsoleView.print(text, ConsoleViewContentType.USER_INPUT);
      if (!text.endsWith("\n")) {
        myConsoleView.print("\n", ConsoleViewContentType.USER_INPUT);
      }
    }
    else {
      assert editor != null;
      ((LanguageConsoleViewEx)myConsoleView).addTextRangeToHistory(range, editor, myExecuteActionHandler.isPreserveMarkup());
    }
    myExecuteActionHandler.addToCommandHistoryAndExecute(myConsoleView, text);
  }

  public ConsoleExecuteActionHandler getExecuteActionHandler() {
    return myExecuteActionHandler;
  }
}