/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test;

import consulo.application.ui.action.DumbAwareToggleBooleanProperty;
import consulo.application.ui.action.ToggleBooleanProperty;
import consulo.component.util.config.AbstractProperty;
import consulo.component.util.config.BooleanProperty;
import consulo.component.util.config.Storage;
import consulo.component.util.config.StoringPropertyContainer;
import consulo.disposer.Disposable;
import consulo.execution.DefaultExecutionTarget;
import consulo.execution.ExecutionTarget;
import consulo.execution.configuration.ModuleRunProfile;
import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.executor.Executor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.console.ConsoleView;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author anna
 * @since 25-May-2007
 */
public abstract class TestConsoleProperties extends StoringPropertyContainer implements Disposable {
  public static final BooleanProperty SCROLL_TO_STACK_TRACE = new BooleanProperty("scrollToStackTrace", false);
  public static final BooleanProperty SORT_ALPHABETICALLY = new BooleanProperty("sortTestsAlphabetically", false);
  public static final BooleanProperty SORT_BY_DURATION = new BooleanProperty("sortTestsByDuration", false);
  public static final BooleanProperty SELECT_FIRST_DEFECT = new BooleanProperty("selectFirtsDefect", false);
  public static final BooleanProperty TRACK_RUNNING_TEST = new BooleanProperty("trackRunningTest", true);
  public static final BooleanProperty HIDE_IGNORED_TEST = new BooleanProperty("hideIgnoredTests", false);
  public static final BooleanProperty HIDE_PASSED_TESTS = new BooleanProperty("hidePassedTests", true);
  public static final BooleanProperty SCROLL_TO_SOURCE = new BooleanProperty("scrollToSource", false);
  public static final BooleanProperty OPEN_FAILURE_LINE = new BooleanProperty("openFailureLine", true);
  public static final BooleanProperty TRACK_CODE_COVERAGE = new BooleanProperty("trackCodeCoverage", false);
  public static final BooleanProperty SHOW_STATISTICS = new BooleanProperty("showStatistics", false);
  public static final BooleanProperty SHOW_INLINE_STATISTICS = new BooleanProperty("showInlineStatistics", true);
  public static final BooleanProperty INCLUDE_NON_STARTED_IN_RERUN_FAILED = new BooleanProperty("includeNonStarted", true);
  public static final BooleanProperty HIDE_SUCCESSFUL_CONFIG = new BooleanProperty("hideConfig", false);

  private final Project myProject;
  private final Executor myExecutor;
  private ConsoleView myConsole;
  private boolean myUsePredefinedMessageFilter = true;
  private GlobalSearchScope myScope;

  protected final Map<AbstractProperty, List<TestFrameworkPropertyListener>> myListeners = new HashMap<>();

  public TestConsoleProperties(@Nonnull Storage storage, Project project, Executor executor) {
    super(storage);
    myProject = project;
    myExecutor = executor;
  }

  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public GlobalSearchScope getScope() {
    if (myScope == null) {
      myScope = initScope();
    }
    return myScope;
  }

  @Nonnull
  protected GlobalSearchScope initScope() {
    RunProfile configuration = getConfiguration();
    if (!(configuration instanceof ModuleRunProfile)) {
      return GlobalSearchScope.allScope(myProject);
    }

    Module[] modules = ((ModuleRunProfile)configuration).getModules();
    if (modules.length == 0) {
      return GlobalSearchScope.allScope(myProject);
    }

    GlobalSearchScope scope = GlobalSearchScope.EMPTY_SCOPE;
    for (Module each : modules) {
      scope = scope.uniteWith(GlobalSearchScope.moduleRuntimeScope(each, true));
    }
    return scope;
  }

  public <T> void addListener(@Nonnull AbstractProperty<T> property, @Nonnull TestFrameworkPropertyListener<T> listener) {
    List<TestFrameworkPropertyListener> listeners = myListeners.get(property);
    if (listeners == null) {
      myListeners.put(property, (listeners = new ArrayList<>()));
    }
    listeners.add(listener);
  }

  public <T> void addListenerAndSendValue(@Nonnull AbstractProperty<T> property, @Nonnull TestFrameworkPropertyListener<T> listener) {
    addListener(property, listener);
    listener.onChanged(property.get(this));
  }

  public <T> void removeListener(@Nonnull AbstractProperty<T> property, @Nonnull TestFrameworkPropertyListener listener) {
    List<TestFrameworkPropertyListener> listeners = myListeners.get(property);
    if (listeners != null) {
      listeners.remove(listener);
    }
  }

  public Executor getExecutor() {
    return myExecutor;
  }

  public boolean isDebug() {
    return myExecutor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID);
  }

  public boolean isPaused() {
    XDebugSession debuggerSession = XDebuggerManager.getInstance(myProject).getDebugSession(getConsole());
    return debuggerSession != null && debuggerSession.isPaused();
  }

  @Override
  protected <T> void onPropertyChanged(@Nonnull AbstractProperty<T> property, T value) {
    List<TestFrameworkPropertyListener> listeners = myListeners.get(property);
    if (listeners != null) {
      for (Object o : listeners.toArray()) {
        @SuppressWarnings("unchecked") TestFrameworkPropertyListener<T> listener = (TestFrameworkPropertyListener<T>)o;
        listener.onChanged(value);
      }
    }
  }

  public void setConsole(ConsoleView console) {
    myConsole = console;
  }

  @Override
  public void dispose() {
    myListeners.clear();
  }

  public abstract RunProfile getConfiguration();

  /**
   * Allows to make console editable and disable/enable input sending in process stdin stream.
   * Normally tests shouldn't ask anything in stdin so console is view only by default.
   * <p/>
   * NB1: Process input support feature isn't fully implemented. Input text will be lost after
   * switching to any other test/suite in tests results view. It's highly not recommended to change
   * default behaviour. Please do it only in critical cases and only if you are sure that you need this feature.
   * <p/>
   * NB2: If you are using Service Messages based test runner please ensure that before each service message
   * (e.g. #teamcity[...]) you always send "\n" to the output stream.
   *
   * @return False for view-only mode and true for stdin support.
   */
  public boolean isEditable() {
    return false;
  }

  protected ExecutionConsole getConsole() {
    return myConsole;
  }

  public boolean isUsePredefinedMessageFilter() {
    return myUsePredefinedMessageFilter;
  }

  public void setUsePredefinedMessageFilter(boolean usePredefinedMessageFilter) {
    myUsePredefinedMessageFilter = usePredefinedMessageFilter;
  }

  public void appendAdditionalActions(DefaultActionGroup actionGroup, JComponent parent, TestConsoleProperties target) { }

  @Nullable
  public AnAction createImportAction() {
    return null;
  }

  @Nonnull
  protected ToggleBooleanProperty createIncludeNonStartedInRerun(TestConsoleProperties target) {
    LocalizeValue text = ExecutionLocalize.junitRuningInfoIncludeNonStartedInRerunFailedActionName();
    return new DumbAwareToggleBooleanProperty(text, LocalizeValue.empty(), null, target, INCLUDE_NON_STARTED_IN_RERUN_FAILED);
  }

  @Nonnull
  protected ToggleBooleanProperty createHideSuccessfulConfig(TestConsoleProperties target) {
    LocalizeValue text = ExecutionLocalize.junitRuningInfoHideSuccessfulConfigActionName();
    setIfUndefined(HIDE_SUCCESSFUL_CONFIG, true);
    return new DumbAwareToggleBooleanProperty(text, LocalizeValue.empty(), null, target, HIDE_SUCCESSFUL_CONFIG);
  }

  @JdkConstants.TreeSelectionMode
  public int getSelectionMode() {
    return TreeSelectionModel.SINGLE_TREE_SELECTION;
  }

  @Nonnull
  public ExecutionTarget getExecutionTarget() {
    return DefaultExecutionTarget.INSTANCE;
  }
}
