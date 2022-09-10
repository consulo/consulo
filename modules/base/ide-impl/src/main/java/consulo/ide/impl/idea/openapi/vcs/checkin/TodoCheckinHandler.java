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
package consulo.ide.impl.idea.openapi.vcs.checkin;

import consulo.application.ui.util.TodoPanelSettings;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.todo.*;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ide.ServiceManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.ref.Ref;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.LinkListener;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import java.util.function.Consumer;
import consulo.util.lang.function.PairConsumer;
import consulo.application.util.DateFormatUtil;
import consulo.versionControlSystem.checkin.CheckinHandler;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collection;

import static consulo.application.CommonBundle.getCancelButtonText;
import static consulo.ui.ex.awt.Messages.*;
import static consulo.ui.ex.awt.UIUtil.getWarningIcon;

/**
 * @author irengrig
 *         Date: 2/17/11
 *         Time: 5:54 PM
 */
public class TodoCheckinHandler extends CheckinHandler {
  private final Project myProject;
  private final CheckinProjectPanel myCheckinProjectPanel;
  private final VcsConfiguration myConfiguration;
  private TodoFilter myTodoFilter;

  public TodoCheckinHandler(CheckinProjectPanel checkinProjectPanel) {
    myProject = checkinProjectPanel.getProject();
    myCheckinProjectPanel = checkinProjectPanel;
    myConfiguration = VcsConfiguration.getInstance(myProject);
  }

  @Override
  public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
    JCheckBox checkBox = new JCheckBox(VcsBundle.message("before.checkin.new.todo.check", ""));
    return new RefreshableOnComponent() {
      @Override
      public JComponent getComponent() {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.add(checkBox, BorderLayout.WEST);
        setFilterText(myConfiguration.myTodoPanelSettings.todoFilterName);
        if (myConfiguration.myTodoPanelSettings.todoFilterName != null) {
          myTodoFilter = TodoConfiguration.getInstance().getTodoFilter(myConfiguration.myTodoPanelSettings.todoFilterName);
        }

        Consumer<TodoFilter> consumer = todoFilter -> {
          myTodoFilter = todoFilter;
          String name = todoFilter == null ? null : todoFilter.getName();
          myConfiguration.myTodoPanelSettings.todoFilterName = name;
          setFilterText(name);
        };
        LinkLabel linkLabel = new LinkLabel("Configure", null);
        linkLabel.setListener(new LinkListener() {
          @Override
          public void linkSelected(LinkLabel aSource, Object aLinkData) {
            DefaultActionGroup group = SetTodoFilterAction.createPopupActionGroup(myProject, myConfiguration.myTodoPanelSettings, consumer);
            ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TODO_VIEW_TOOLBAR, group);
            popupMenu.getComponent().show(linkLabel, 0, linkLabel.getHeight());
          }
        }, null);
        panel.add(linkLabel, BorderLayout.CENTER);

        CheckinHandlerUtil.disableWhenDumb(myProject, checkBox, "TODO check is impossible until indices are up-to-date");
        return panel;
      }

      private void setFilterText(String filterName) {
        if (filterName == null) {
          checkBox.setText(VcsBundle.message("before.checkin.new.todo.check", IdeBundle.message("action.todo.show.all")));
        } else {
          checkBox.setText(VcsBundle.message("before.checkin.new.todo.check", "Filter: " + filterName));
        }
      }

      @Override
      public void refresh() {
      }

      @Override
      public void saveState() {
        myConfiguration.CHECK_NEW_TODO = checkBox.isSelected();
      }

      @Override
      public void restoreState() {
        checkBox.setSelected(myConfiguration.CHECK_NEW_TODO);
      }
    };
  }

  @Override
  public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
    if (! myConfiguration.CHECK_NEW_TODO) return ReturnResult.COMMIT;
    if (DumbService.getInstance(myProject).isDumb()) {
      String todoName = VcsBundle.message("before.checkin.new.todo.check.title");
      if (Messages.showOkCancelDialog(myProject,
                                      todoName +
                                      " can't be performed while " + ApplicationNamesInfo.getInstance().getFullProductName() + " updates the indices in background.\n" +
                                      "You can commit the changes without running checks, or you can wait until indices are built.",
                                      todoName + " is not possible right now",
                                      "&Wait", "&Commit", null) == Messages.OK) {
        return ReturnResult.CANCEL;
      }
      return ReturnResult.COMMIT;
    }
    Collection<Change> changes = myCheckinProjectPanel.getSelectedChanges();
    TodoCheckinHandlerWorker worker = new TodoCheckinHandlerWorker(myProject, changes, myTodoFilter, true);

    Ref<Boolean> completed = Ref.create(Boolean.FALSE);
    ProgressManager.getInstance().run(new Task.Modal(myProject, "Looking for New and Edited TODO Items...", true) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        worker.execute();
      }

      @Override
      public void onSuccess() {
        completed.set(Boolean.TRUE);
      }
    });
    if (completed.get() && (worker.getAddedOrEditedTodos().isEmpty() && worker.getInChangedTodos().isEmpty() &&
                            worker.getSkipped().isEmpty())) return ReturnResult.COMMIT;
    if (!completed.get()) return ReturnResult.CANCEL;
    return showResults(worker, executor);
  }

  private ReturnResult showResults(TodoCheckinHandlerWorker worker, CommitExecutor executor) {
    String commitButtonText = executor != null ? executor.getActionText() : myCheckinProjectPanel.getCommitActionName();
    commitButtonText = StringUtil.trimEnd(commitButtonText, "...");

    String text = createMessage(worker);
    boolean thereAreTodoFound = worker.getAddedOrEditedTodos().size() + worker.getInChangedTodos().size() > 0;
    String title = "TODO";
    if (thereAreTodoFound) {
      return askReviewOrCommit(worker, commitButtonText, text, title);
    }
    else if (YES == showYesNoDialog(myProject, text, title, commitButtonText, getCancelButtonText(), getWarningIcon())) {
      return ReturnResult.COMMIT;
    }
    return ReturnResult.CANCEL;
  }

  @Nonnull
  private ReturnResult askReviewOrCommit(@Nonnull TodoCheckinHandlerWorker worker,
                                         @Nonnull String commitButton,
                                         @Nonnull String text,
                                         @Nonnull String title) {
    String yesButton = VcsBundle.message("todo.in.new.review.button");
    switch (showYesNoCancelDialog(myProject, text, title, yesButton, commitButton, getCancelButtonText(), getWarningIcon())) {
      case YES:
        showTodo(worker);
        return ReturnResult.CLOSE_WINDOW;
      case NO:
        return ReturnResult.COMMIT;
    }
    return ReturnResult.CANCEL;
  }

  private void showTodo(TodoCheckinHandlerWorker worker) {
    String title = "For commit (" + DateFormatUtil.formatDateTime(System.currentTimeMillis()) + ")";
    ServiceManager.getService(myProject, TodoView.class).addCustomTodoView(new TodoTreeBuilderFactory() {
      @Override
      public TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        return new CustomChangelistTodosTreeBuilder(tree, myProject, title, worker.inOneList());
      }
    }, title, new TodoPanelSettings(myConfiguration.myTodoPanelSettings));

    ApplicationManager.getApplication().invokeLater(() -> {
      ToolWindowManager manager = ToolWindowManager.getInstance(myProject);
      if (manager != null) {
        ToolWindow window = manager.getToolWindow("TODO");
        if (window != null) {
          window.show(() -> {
            ContentManager cm = window.getContentManager();
            Content[] contents = cm.getContents();
            if (contents.length > 0) {
              cm.setSelectedContent(contents[contents.length - 1], true);
            }
          });
        }
      }
    }, IdeaModalityState.NON_MODAL, myProject.getDisposed());
  }

  private static String createMessage(TodoCheckinHandlerWorker worker) {
    int added = worker.getAddedOrEditedTodos().size();
    int changed = worker.getInChangedTodos().size();
    int skipped = worker.getSkipped().size();
    if (added == 0 && changed == 0) {
      return VcsBundle.message("todo.handler.only.skipped", skipped);
    } else if (changed == 0) {
      return VcsBundle.message("todo.handler.only.added", added, skipped);
    } else if (added == 0) {
      return VcsBundle.message("todo.handler.only.in.changed", changed, skipped);
    } else {
      return VcsBundle.message("todo.handler.only.both", added, changed, skipped);
    }
  }
}
