// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.todo;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.util.TodoPanelSettings;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.PluginVcsMappingListener;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsListener;
import consulo.versionControlSystem.VcsMappingListener;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.ide.impl.idea.openapi.wm.ex.ToolWindowEx;
import consulo.language.file.event.FileTypeEvent;
import consulo.language.file.event.FileTypeListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.OptionTag;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.*;

@State(name = "TodoView", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class TodoView implements PersistentStateComponent<TodoView.State>, Disposable {
  private final Project myProject;

  private ContentManager myContentManager;
  private TodoPanel myAllTodos;
  private final List<TodoPanel> myPanels = new ArrayList<>();
  private final List<Content> myNotAddedContent = new ArrayList<>();

  private State state = new State();

  private Content myChangeListTodosContent;

  private final MyVcsListener myVcsListener = new MyVcsListener();

  @Inject
  public TodoView(@Nonnull Project project) {
    myProject = project;

    state.all.arePackagesShown = true;
    state.all.isAutoScrollToSource = true;

    state.current.isAutoScrollToSource = true;

    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(TodoConfigurationListener.class, new MyTodoConfigurationListener());
    connection.subscribe(FileTypeListener.class, new MyFileTypeListener());
    connection.subscribe(VcsMappingListener.class, myVcsListener);
    connection.subscribe(PluginVcsMappingListener.class, myVcsListener);
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
  public void loadState(@Nonnull State state) {
    this.state = state;
  }

  @Override
  public State getState() {
    if (myContentManager != null) {
      // all panel were constructed
      Content content = myContentManager.getSelectedContent();
      state.selectedIndex = content == null ? -1 : myContentManager.getIndexOfContent(content);
    }
    return state;
  }

  @Override
  public void dispose() {
  }

  public void initToolWindow(@Nonnull ToolWindow toolWindow) {
    // Create panels
    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    Content allTodosContent = contentFactory.createContent(null, IdeBundle.message("title.project"), false);
    myAllTodos = new TodoPanel(myProject, state.all, false, allTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        AllTodosTreeBuilder builder = createAllTodoBuilder(tree, project);
        builder.init();
        return builder;
      }
    };
    allTodosContent.setComponent(myAllTodos);
    Disposer.register(this, myAllTodos);
    if (toolWindow instanceof ToolWindowEx) {
      DefaultActionGroup group = new DefaultActionGroup() {
        {
          getTemplatePresentation().setText(IdeBundle.message("group.view.options"));
          setPopup(true);
          add(myAllTodos.createAutoScrollToSourceAction());
          addSeparator();
          addAll(myAllTodos.createGroupByActionGroup());
        }
      };
      ((ToolWindowEx)toolWindow).setAdditionalGearActions(group);
    }

    Content currentFileTodosContent = contentFactory.createContent(null, IdeBundle.message("title.todo.current.file"), false);
    CurrentFileTodosPanel currentFileTodos = new CurrentFileTodosPanel(myProject, state.current, currentFileTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        CurrentFileTodosTreeBuilder builder = new CurrentFileTodosTreeBuilder(tree, project);
        builder.init();
        return builder;
      }
    };
    Disposer.register(this, currentFileTodos);
    currentFileTodosContent.setComponent(currentFileTodos);

    String tabName = getTabNameForChangeList(ChangeListManager.getInstance(myProject).getDefaultChangeList().getName());
    myChangeListTodosContent = contentFactory.createContent(null, tabName, false);
    ChangeListTodosPanel changeListTodos = new ChangeListTodosPanel(myProject, state.current, myChangeListTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        ChangeListTodosTreeBuilder builder = new ChangeListTodosTreeBuilder(tree, project);
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
  static String getTabNameForChangeList(@Nonnull String changelistName) {
    changelistName = changelistName.trim();
    String suffix = "Changelist";
    return StringUtil.endsWithIgnoreCase(changelistName, suffix) ? changelistName : changelistName + " " + suffix;
  }

  @Nonnull
  protected AllTodosTreeBuilder createAllTodoBuilder(JTree tree, Project project) {
    return new AllTodosTreeBuilder(tree, project);
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
      }, IdeaModalityState.NON_MODAL);
    }
  }

  private final class MyTodoConfigurationListener implements TodoConfigurationListener {
    @Override
    public void propertyChanged(String propertyName, Object oldValue, Object newValue) {
      if (TodoConfiguration.PROP_TODO_PATTERNS.equals(propertyName) || TodoConfiguration.PROP_TODO_FILTERS.equals(propertyName)) {
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
      refresh();
    }
  }

  public void refresh() {
    Map<TodoPanel, Set<VirtualFile>> files = new HashMap<>();

    AccessRule.readAsync(() -> {
      if (myAllTodos == null) {
        return;
      }
      for (TodoPanel panel : myPanels) {
        panel.myTodoTreeBuilder.collectFiles(virtualFile -> {
          files.computeIfAbsent(panel, p -> new HashSet<>()).add(virtualFile);
          return true;
        });
      }
    }).doWhenDone(() -> {
      Application.get().invokeLater(() -> {
        for (TodoPanel panel : myPanels) {
          panel.rebuildCache(ObjectUtil.notNull(files.get(panel), new HashSet<>()));
          panel.updateTree();
        }
      }, IdeaModalityState.NON_MODAL);
    });
  }

  public void addCustomTodoView(final TodoTreeBuilderFactory factory, final String title, final TodoPanelSettings settings) {
    Content content = ContentFactory.SERVICE.getInstance().createContent(null, title, true);
    final ChangeListTodosPanel panel = new ChangeListTodosPanel(myProject, settings, content) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        TodoTreeBuilder todoTreeBuilder = factory.createTreeBuilder(tree, project);
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
