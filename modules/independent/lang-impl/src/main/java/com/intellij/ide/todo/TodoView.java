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
package com.intellij.ide.todo;

import com.intellij.ProjectTopics;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.OptionTag;
import consulo.annotations.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.psi.PsiPackageSupportProviders;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

@State(name = "TodoView", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@Singleton
public class TodoView implements PersistentStateComponent<TodoView.State>, Disposable {
  private final Project myProject;

  private ContentManager myContentManager;
  private TodoPanel myAllTodos;
  private final List<TodoPanel> myPanels = new ArrayList<>();
  private final List<Content> myNotAddedContent = new ArrayList<>();

  private State state = new State();

  private Content myChangeListTodosContent;

  private final MyVcsListener myVcsListener = new MyVcsListener();

  @RequiredReadAction
  @Inject
  public TodoView(@Nonnull Project project) {
    myProject = project;

    state.all.isAutoScrollToSource = true;

    state.current.isAutoScrollToSource = true;

    TodoConfiguration.getInstance().addPropertyChangeListener(new MyPropertyChangeListener(), this);

    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(FileTypeManager.TOPIC, new MyFileTypeListener());
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, myVcsListener);
    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      @RequiredReadAction
      public void rootsChanged(ModuleRootEvent event) {
        validateSettings();
      }
    });
  }

  @RequiredReadAction
  private void validateSettings() {
    boolean packageSupported = PsiPackageSupportProviders.isPackageSupported(myProject);
    if (state.all.arePackagesShown && !packageSupported) {
      state.all.arePackagesShown = false;
      state.all.areModulesShown = true;
    }

    if (state.current.arePackagesShown && !packageSupported) {
      state.current.arePackagesShown = false;
    }

    if (state.changeList.arePackagesShown && !packageSupported) {
      state.changeList.arePackagesShown = false;
    }
  }

  static class State {
    @Attribute(value = "selected-index")
    public int selectedIndex;

    @OptionTag(value = "selected-file", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings current = new TodoPanelSettings();

    @OptionTag(value = "all", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings all = new TodoPanelSettings();

    @OptionTag(value = "default-changelist", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings changeList = new TodoPanelSettings();
  }

  @Override
  public void loadState(State state) {
    this.state = state;

    AccessRule.read(this::validateSettings);
  }

  @Override
  public State getState() {
    if (myContentManager != null) {
      // all panel were constructed
      Content content = myContentManager.getSelectedContent();
      state.selectedIndex = myContentManager.getIndexOfContent(content);
    }
    return state;
  }

  @Override
  public void dispose() {
  }

  public void initToolWindow(@Nonnull ToolWindow toolWindow) {
    // Create panels
    ContentFactory contentFactory = ContentFactory.getInstance();
    Content allTodosContent = contentFactory.createContent(null, IdeBundle.message("title.project"), false);
    myAllTodos = new TodoPanel(myProject, state.all, false, allTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, DefaultTreeModel treeModel, Project project) {
        AllTodosTreeBuilder builder = createAllTodoBuilder(tree, treeModel, project);
        builder.init();
        return builder;
      }
    };
    allTodosContent.setComponent(myAllTodos);
    Disposer.register(this, myAllTodos);

    Content currentFileTodosContent = contentFactory.createContent(null, IdeBundle.message("title.todo.current.file"), false);
    CurrentFileTodosPanel currentFileTodos = new CurrentFileTodosPanel(myProject, state.current, currentFileTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, DefaultTreeModel treeModel, Project project) {
        CurrentFileTodosTreeBuilder builder = new CurrentFileTodosTreeBuilder(tree, treeModel, project);
        builder.init();
        return builder;
      }
    };
    Disposer.register(this, currentFileTodos);
    currentFileTodosContent.setComponent(currentFileTodos);

    myChangeListTodosContent = contentFactory
            .createContent(null, IdeBundle.message("changelist.todo.title", ChangeListManager.getInstance(myProject).getDefaultChangeList().getName()), false);
    ChangeListTodosPanel changeListTodos = new ChangeListTodosPanel(myProject, state.current, myChangeListTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, DefaultTreeModel treeModel, Project project) {
        ChangeListTodosTreeBuilder builder = new ChangeListTodosTreeBuilder(tree, treeModel, project);
        builder.init();
        return builder;
      }
    };
    Disposer.register(this, changeListTodos);
    myChangeListTodosContent.setComponent(changeListTodos);

    Content scopeBasedTodoContent = contentFactory.createContent(null, "Scope Based", false);
    ScopeBasedTodosPanel scopeBasedTodos = new ScopeBasedTodosPanel(myProject, state.current, scopeBasedTodoContent);
    Disposer.register(this, scopeBasedTodos);
    scopeBasedTodoContent.setComponent(scopeBasedTodos);

    myContentManager = toolWindow.getContentManager();

    myContentManager.addContent(allTodosContent);
    myContentManager.addContent(currentFileTodosContent);
    myContentManager.addContent(scopeBasedTodoContent);

    if (ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss()) {
      myVcsListener.myIsVisible = true;
      myContentManager.addContent(myChangeListTodosContent);
    }
    for (Content content : myNotAddedContent) {
      myContentManager.addContent(content);
    }

    myChangeListTodosContent.setCloseable(false);
    allTodosContent.setCloseable(false);
    currentFileTodosContent.setCloseable(false);
    scopeBasedTodoContent.setCloseable(false);
    Content content = myContentManager.getContent(state.selectedIndex);
    myContentManager.setSelectedContent(content == null ? allTodosContent : content);

    myPanels.add(myAllTodos);
    myPanels.add(changeListTodos);
    myPanels.add(currentFileTodos);
    myPanels.add(scopeBasedTodos);
  }

  @Nonnull
  protected AllTodosTreeBuilder createAllTodoBuilder(JTree tree, DefaultTreeModel treeModel, Project project) {
    return new AllTodosTreeBuilder(tree, treeModel, project);
  }

  private final class MyVcsListener implements VcsListener {
    private boolean myIsVisible;

    @Override
    public void directoryMappingChanged() {
      ApplicationManager.getApplication().invokeLater(() -> {
        if (myContentManager == null || myProject.isDisposed()) {
          // was not initialized yet
          return;
        }

        boolean hasActiveVcss = ProjectLevelVcsManager.getInstance(myProject).hasActiveVcss();
        if (myIsVisible && !hasActiveVcss) {
          myContentManager.removeContent(myChangeListTodosContent, false);
          myIsVisible = false;
        }
        else if (!myIsVisible && hasActiveVcss) {
          myContentManager.addContent(myChangeListTodosContent);
          myIsVisible = true;
        }
      }, ModalityState.NON_MODAL);
    }
  }

  private final class MyPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent e) {
      if (TodoConfiguration.PROP_TODO_PATTERNS.equals(e.getPropertyName()) || TodoConfiguration.PROP_TODO_FILTERS.equals(e.getPropertyName())) {
        _updateFilters();
      }
    }

    private void _updateFilters() {
      try {
        if (!DumbService.isDumb(myProject)) {
          updateFilters();
          return;
        }
      }
      catch (ProcessCanceledException ignore) {
      }
      DumbService.getInstance(myProject).smartInvokeLater(this::_updateFilters);
    }

    private void updateFilters() {
      for (TodoPanel panel : myPanels) {
        panel.updateTodoFilter();
      }
    }
  }

  private final class MyFileTypeListener implements FileTypeListener {
    @Override
    public void fileTypesChanged(@Nonnull FileTypeEvent e) {
      // this invokeLater guaranties that this code will be invoked after
      // PSI gets the same event.
      DumbService.getInstance(myProject).smartInvokeLater(() -> ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        if (myAllTodos == null) {
          return;
        }

        ApplicationManager.getApplication().runReadAction(() -> {
          for (TodoPanel panel : myPanels) {
            panel.rebuildCache();
          }
        });
        ApplicationManager.getApplication().invokeLater(() -> {
          for (TodoPanel panel : myPanels) {
            panel.updateTree();
          }
        }, ModalityState.NON_MODAL);
      }, IdeBundle.message("progress.looking.for.todos"), false, myProject));
    }
  }

  public void refresh() {
    ApplicationManager.getApplication().runReadAction(() -> {
      for (TodoPanel panel : myPanels) {
        panel.rebuildCache();
      }
    });
    ApplicationManager.getApplication().invokeLater(() -> {
      for (TodoPanel panel : myPanels) {
        panel.updateTree();
      }
    }, ModalityState.NON_MODAL);
  }

  public void addCustomTodoView(final TodoTreeBuilderFactory factory, final String title, final TodoPanelSettings settings) {
    Content content = ContentFactory.getInstance().createContent(null, title, true);
    final ChangeListTodosPanel panel = new ChangeListTodosPanel(myProject, settings, content) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, DefaultTreeModel treeModel, Project project) {
        TodoTreeBuilder todoTreeBuilder = factory.createTreeBuilder(tree, treeModel, project);
        todoTreeBuilder.init();
        return todoTreeBuilder;
      }
    };
    content.setComponent(panel);
    Disposer.register(this, panel);

    if (myContentManager == null) {
      myNotAddedContent.add(content);
    }
    else {
      myContentManager.addContent(content);
    }
    myPanels.add(panel);
    content.setCloseable(true);
    content.setDisposer(new Disposable() {
      @Override
      public void dispose() {
        myPanels.remove(panel);
      }
    });
  }
}
