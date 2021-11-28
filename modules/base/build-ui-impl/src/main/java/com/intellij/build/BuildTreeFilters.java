/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.build;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import consulo.platform.base.icon.PlatformIconGroup;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * from kotlin
 */
public class BuildTreeFilters {
  private static final Predicate<ExecutionNode> SUCCESSFUL_STEPS_FILTER = (node) -> !node.isFailed() && !node.hasWarnings();
  private static final Predicate<ExecutionNode> WARNINGS_FILTER = (node) -> node.hasWarnings() || node.hasInfos();

  public static DefaultActionGroup createFilteringActionsGroup(Filterable<ExecutionNode> filterable) {
    DefaultActionGroup actionGroup = new DefaultActionGroup(LangBundle.message("action.filters.text"), true);
    actionGroup.getTemplatePresentation().setIcon(PlatformIconGroup.actionsShow());
    actionGroup.add(new WarningsToggleAction(filterable));
    actionGroup.add(new SuccessfulStepsToggleAction(filterable));
    return actionGroup;
  }

  public static void install(Filterable<ExecutionNode> filterable) {
    boolean filteringEnabled = filterable.isFilteringEnabled();
    if (!filteringEnabled) return;
    SuccessfulStepsToggleAction.install(filterable);
    WarningsToggleAction.install(filterable);
  }

  static class WarningsToggleAction extends FilterToggleAction {
    static void install(Filterable<ExecutionNode> filterable) {
      install(filterable, WARNINGS_FILTER, STATE_KEY, true);
    }

    private static final String STATE_KEY = "build.toolwindow.show.warnings.selection.state";

    WarningsToggleAction(Filterable<ExecutionNode> filterable) {
      super(LangBundle.message("build.tree.filters.show.warnings"), STATE_KEY, filterable, WARNINGS_FILTER, true);
    }
  }

  static class SuccessfulStepsToggleAction extends FilterToggleAction {
    static void install(Filterable<ExecutionNode> filterable) {
      install(filterable, SUCCESSFUL_STEPS_FILTER, STATE_KEY, false);
    }

    private static final String STATE_KEY = "build.toolwindow.show.successful.steps.selection.state";

    SuccessfulStepsToggleAction(Filterable<ExecutionNode> filterable) {
      super(LangBundle.message("build.tree.filters.show.succesful"), STATE_KEY, filterable, SUCCESSFUL_STEPS_FILTER, false);
    }
  }

  static class FilterToggleAction extends ToggleAction implements DumbAware {
    static void install(Filterable<ExecutionNode> filterable, Predicate<ExecutionNode> filter, String stateKey, boolean defaultState) {
      if (PropertiesComponent.getInstance().getBoolean(stateKey, defaultState) && !filterable.contains(filter)) {
        filterable.addFilter(filter);
      }
    }

    private final String stateKey;
    private final Filterable<ExecutionNode> filterable;
    private final Predicate<ExecutionNode> filter;
    private final boolean defaultState;

    FilterToggleAction(String text, String stateKey, Filterable<ExecutionNode> filterable, Predicate<ExecutionNode> filter, boolean defaultState) {
      super(text);
      this.stateKey = stateKey;
      this.filterable = filterable;
      this.filter = filter;
      this.defaultState = defaultState;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      boolean filteringEnabled = filterable.isFilteringEnabled();
      presentation.setEnabledAndVisible(filteringEnabled);
      if (filteringEnabled && stateKey != null && PropertiesComponent.getInstance().getBoolean(stateKey, defaultState) && !filterable.contains(filter)) {
        setSelected(e, true);
      }

      return filterable.contains(filter);
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      if (state) {
        filterable.addFilter(filter);
      }
      else {
        filterable.removeFilter(filter);
      }
      if (stateKey != null) {
        PropertiesComponent.getInstance().setValue(stateKey, state, defaultState);
      }
    }
  }
}
