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
package consulo.ide.impl.idea.ide.todo;

import consulo.application.dumb.DumbAware;
import consulo.application.ui.util.TodoPanelSettings;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.todo.configurable.TodoConfigurable;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author irengrig
 * Date: 2/24/11
 * Time: 3:38 PM
 * moved from inner class
 */
public class SetTodoFilterAction extends ActionGroup implements DumbAware {
  private final Project myProject;
  private final TodoPanelSettings myToDoSettings;
  private final Consumer<TodoFilter> myTodoFilterConsumer;

  public SetTodoFilterAction(final Project project, final TodoPanelSettings toDoSettings, final Consumer<TodoFilter> todoFilterConsumer) {
    super(IdeBundle.message("action.filter.todo.items"), null, PlatformIconGroup.generalFilter());
    setPopup(true);
    myProject = project;
    myToDoSettings = toDoSettings;
    myTodoFilterConsumer = todoFilterConsumer;
  }

  @Nonnull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    TodoFilter[] filters = TodoConfiguration.getInstance().getTodoFilters();
    List<AnAction> group = new ArrayList<>();
    group.add(new TodoFilterApplier(IdeBundle.message("action.todo.show.all"),
                                    IdeBundle.message("action.description.todo.show.all"), null, myToDoSettings, myTodoFilterConsumer));
    for (TodoFilter filter : filters) {
      group.add(new TodoFilterApplier(filter.getName(), null, filter, myToDoSettings, myTodoFilterConsumer));
    }
    group.add(AnSeparator.create());
    group.add(new DumbAwareAction(IdeBundle.message("action.todo.edit.filters"),
                                  IdeBundle.message("action.todo.edit.filters"),
                                  PlatformIconGroup.generalSettings()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                  ShowSettingsUtil.getInstance().showAndSelect(myProject, TodoConfigurable.class);
                }
              }
    );
    return group.toArray(AnAction[]::new);
  }

  private static class TodoFilterApplier extends ToggleAction implements DumbAware {
    private final TodoFilter myFilter;
    private final TodoPanelSettings mySettings;
    private final Consumer<TodoFilter> myTodoFilterConsumer;

    /**
     * @param text               action's text.
     * @param description        action's description.
     * @param filter             filter to be applied. {@code null} value means "empty" filter.
     * @param settings
     * @param todoFilterConsumer
     */
    TodoFilterApplier(String text,
                      String description,
                      TodoFilter filter,
                      TodoPanelSettings settings,
                      Consumer<TodoFilter> todoFilterConsumer) {
      super(null, description, null);
      mySettings = settings;
      myTodoFilterConsumer = todoFilterConsumer;
      getTemplatePresentation().setText(text, false);
      myFilter = filter;
    }

    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      if (myFilter != null) {
        e.getPresentation().setEnabled(!myFilter.isEmpty());
      }
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return Comparing.equal(myFilter != null ? myFilter.getName() : null, mySettings.todoFilterName);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      if (state) {
        myTodoFilterConsumer.accept(myFilter);
        //setTodoFilter(myFilter);
      }
    }
  }
}
