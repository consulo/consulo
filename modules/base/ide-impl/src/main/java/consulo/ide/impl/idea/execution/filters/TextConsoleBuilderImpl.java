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

package consulo.ide.impl.idea.execution.filters;

import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.content.scope.SearchScope;
import consulo.execution.ui.console.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.process.ProcessHandler;
import consulo.project.Project;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dyoma
 */
public class TextConsoleBuilderImpl extends TextConsoleBuilder {
  private final Project myProject;
  private final SearchScope myScope;
  private final List<Filter> myFilters = new ArrayList<>();

  private ConsoleState myState = new ConsoleState.NotStartedStated() {
    @Nonnull
    @Override
    public ConsoleState attachTo(@Nonnull ConsoleView console, ProcessHandler processHandler) {
      return new ConsoleViewRunningState(console, processHandler, this, true, true);
    }
  };
  
  private boolean myViewer;
  private boolean myUsePredefinedMessageFilter = true;

  public TextConsoleBuilderImpl(final Project project) {
    this(project, GlobalSearchScope.allScope(project));
  }

  public TextConsoleBuilderImpl(@Nonnull final Project project, @Nonnull SearchScope scope) {
    myProject = project;
    myScope = scope;
  }

  @Override
  public ConsoleView getConsole() {
    final ConsoleView consoleView = createConsole();
    for (final Filter filter : myFilters) {
      consoleView.addMessageFilter(filter);
    }
    return consoleView;
  }

  protected ConsoleView createConsole() {
    return new ConsoleViewImpl(myProject, myScope, myViewer, myState, myUsePredefinedMessageFilter);
  }

  @Override
  public void addFilter(final Filter filter) {
    myFilters.add(filter);
  }

  @Override
  public TextConsoleBuilder filters(List<Filter> filters) {
    myFilters.addAll(filters);
    return this;
  }

  @Override
  public void setViewer(boolean isViewer) {
    myViewer = isViewer;
  }

  @Override
  public void setState(@Nonnull ConsoleState state) {
    myState = state;
  }

  protected Project getProject() {
    return myProject;
  }

  protected SearchScope getScope() {
    return myScope;
  }

  protected List<Filter> getFilters() {
    return myFilters;
  }

  protected boolean isViewer() {
    return myViewer;
  }

  public void setUsePredefinedMessageFilter(boolean usePredefinedMessageFilter) {
    myUsePredefinedMessageFilter = usePredefinedMessageFilter;
  }

  public boolean isUsePredefinedMessageFilter() {
    return myUsePredefinedMessageFilter;
  }
}