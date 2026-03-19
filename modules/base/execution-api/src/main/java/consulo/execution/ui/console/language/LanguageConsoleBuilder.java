/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.annotation.UsedInPlugin;
import consulo.codeEditor.EditorEx;
import consulo.execution.internal.LanguageConsoleViewEx;
import consulo.execution.ui.console.ConsoleExecuteAction;
import consulo.execution.ui.console.ConsoleRootType;
import consulo.language.psi.PsiCodeFragment;
import consulo.language.psi.PsiFile;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

@UsedInPlugin
@SuppressWarnings("unused")
public abstract class LanguageConsoleBuilder {
  protected static class MyConsoleRootType extends ConsoleRootType {
    public MyConsoleRootType(String historyType) {
      super(historyType, null);
    }
  }

  protected @Nullable Predicate<LanguageConsoleView> executionEnabled = languageConsoleView -> true;
  protected @Nullable BiFunction<VirtualFile, Project, PsiFile> psiFileFactory;
  protected @Nullable BaseConsoleExecuteActionHandler executeActionHandler;
  protected @Nullable String historyType;

  protected @Nullable GutterContentProvider gutterContentProvider;

  protected boolean oneLineInput;

  protected String processInputStateKey;

  public LanguageConsoleBuilder processHandler(ProcessHandler processHandler) {
    executionEnabled = console -> !processHandler.isProcessTerminated();
    return this;
  }

  public LanguageConsoleBuilder executionEnabled(Predicate<LanguageConsoleView> condition) {
    executionEnabled = condition;
    return this;
  }

  /**
   * @see {@link PsiCodeFragment}
   */
  public LanguageConsoleBuilder psiFileFactory(BiFunction<VirtualFile, Project, PsiFile> value) {
    psiFileFactory = value;
    return this;
  }

  
  public LanguageConsoleBuilder initActions(BaseConsoleExecuteActionHandler executeActionHandler, String historyType) {
    this.executeActionHandler = executeActionHandler;
    this.historyType = historyType;
    return this;
  }


  /**
   * todo This API doesn't look good, but it is much better than force client to know low-level details
   */
  public static AnAction registerExecuteAction(LanguageConsoleView console,
                                               final Consumer<String> executeActionHandler,
                                               String historyType,
                                               @Nullable String historyPersistenceId,
                                               @Nullable Predicate<LanguageConsoleView> enabledCondition) {
    ConsoleExecuteActionHandler handler = new ConsoleExecuteActionHandler(true) {
      @Override
      public void doExecute(String text, LanguageConsoleView consoleView) {
        executeActionHandler.accept(text);
      }
    };

    ConsoleExecuteAction action = new ConsoleExecuteAction(console, handler, enabledCondition);
    action.registerCustomShortcutSet(action.getShortcutSet(), console.getConsoleEditor().getComponent());

    ((LanguageConsoleViewEx)console).installConsoleHistory(new MyConsoleRootType(historyType), historyPersistenceId);
    return action;
  }

  public LanguageConsoleBuilder gutterContentProvider(@Nullable GutterContentProvider value) {
    gutterContentProvider = value;
    return this;
  }

  /**
   * @see {@link EditorEx#setOneLineMode(boolean)}
   */
  public LanguageConsoleBuilder oneLineInput() {
    oneLineInput(true);
    return this;
  }

  /**
   * @see {@link EditorEx#setOneLineMode(boolean)}
   */
  public LanguageConsoleBuilder oneLineInput(boolean value) {
    oneLineInput = value;
    return this;
  }

  
  public LanguageConsoleBuilder processInputStateKey(@Nullable String value) {
    processInputStateKey = value;
    return this;
  }

  
  public abstract LanguageConsoleView build();
}
