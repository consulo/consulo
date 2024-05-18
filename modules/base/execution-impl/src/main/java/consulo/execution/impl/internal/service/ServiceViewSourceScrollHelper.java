// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.application.AllIcons;
import consulo.execution.ExecutionBundle;
import consulo.execution.service.ServiceViewManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.ui.view.ProjectViewAutoScrollFromSourceHandler;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

final class ServiceViewSourceScrollHelper {
  private static final String AUTO_SCROLL_TO_SOURCE_PROPERTY = "service.view.auto.scroll.to.source";
  private static final String AUTO_SCROLL_FROM_SOURCE_PROPERTY = "service.view.auto.scroll.from.source";

  @Nonnull
  static AutoScrollToSourceHandler createAutoScrollToSourceHandler(@Nonnull Project project) {
    return new ServiceViewAutoScrollToSourceHandler(project);
  }

  static void installAutoScrollSupport(@Nonnull Project project, @Nonnull ToolWindow toolWindow,
                                       @Nonnull AutoScrollToSourceHandler toSourceHandler) {
    ServiceViewAutoScrollFromSourceHandler fromSourceHandler = new ServiceViewAutoScrollFromSourceHandler(project, toolWindow);
    fromSourceHandler.install();
    DefaultActionGroup additionalGearActions = new DefaultActionGroup(toSourceHandler.createToggleAction(),
                                                                      fromSourceHandler.createToggleAction());
    List<AnAction> additionalProviderActions = ServiceViewActionProvider.getInstance().getAdditionalGearActions();
    for (AnAction action : additionalProviderActions) {
      additionalGearActions.add(action);
    }
    toolWindow.setAdditionalGearActions(additionalGearActions);
    toolWindow.setTitleActions(new ScrollFromEditorAction(fromSourceHandler));
  }

  private static boolean isAutoScrollFromSourceEnabled(@Nonnull Project project) {
    return ProjectPropertiesComponent.getInstance(project).getBoolean(AUTO_SCROLL_FROM_SOURCE_PROPERTY, false);
  }

  private static final class ServiceViewAutoScrollToSourceHandler extends AutoScrollToSourceHandler {
    private final Project myProject;

    ServiceViewAutoScrollToSourceHandler(@Nonnull Project project) {
      myProject = project;
    }

    @Override
    protected boolean isAutoScrollMode() {
      return ProjectPropertiesComponent.getInstance(myProject).getBoolean(AUTO_SCROLL_TO_SOURCE_PROPERTY);
    }

    @Override
    protected void setAutoScrollMode(boolean state) {
      ProjectPropertiesComponent.getInstance(myProject).setValue(AUTO_SCROLL_TO_SOURCE_PROPERTY, state);
    }
  }

  private static final class ServiceViewAutoScrollFromSourceHandler extends ProjectViewAutoScrollFromSourceHandler {
    ServiceViewAutoScrollFromSourceHandler(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
      super(project, toolWindow.getComponent(), toolWindow.getContentManager());
    }

    @Override
    protected boolean isAutoScrollEnabled() {
      return isAutoScrollFromSourceEnabled(myProject);
    }

    @Override
    protected void setAutoScrollEnabled(boolean enabled) {
      ProjectPropertiesComponent.getInstance(myProject).setValue(AUTO_SCROLL_FROM_SOURCE_PROPERTY, enabled, false);
    }

    @Override
    protected void selectElementFromEditor(@Nonnull FileEditor editor) {
      select(editor);
    }

    private Promise<Void> select(@Nonnull FileEditor editor) {
      VirtualFile virtualFile = editor.getFile();
      if (virtualFile == null) {
        return Promises.rejectedPromise("Virtual file is null");
      }
      return ((ServiceViewManagerImpl)ServiceViewManager.getInstance(myProject)).select(virtualFile);
    }
  }

  private static final class ScrollFromEditorAction extends DumbAwareAction {
    private final ServiceViewAutoScrollFromSourceHandler myScrollFromHandler;

    ScrollFromEditorAction(ServiceViewAutoScrollFromSourceHandler scrollFromHandler) {
      super(ExecutionBundle.message("service.view.scroll.from.editor.action.name"),
            ExecutionBundle.message("service.view.scroll.from.editor.action.description"),
            AllIcons.General.Locate);
      myScrollFromHandler = scrollFromHandler;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      Project project = e.getData(Project.KEY);
      if (project == null) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }
      e.getPresentation().setEnabledAndVisible(!isAutoScrollFromSourceEnabled(project));
    }

//    @Override
//    public @NotNull ActionUpdateThread getActionUpdateThread() {
//      return ActionUpdateThread.BGT;
//    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      Project project = e.getData(Project.KEY);
      if (project == null) return;

      FileEditorManager manager = FileEditorManager.getInstance(project);
      FileEditor[] editors = manager.getSelectedEditors();
      select(Arrays.asList(editors).iterator());
    }

    private void select(Iterator<? extends FileEditor> editors) {
      if (!editors.hasNext()) return;

      FileEditor editor = editors.next();
      myScrollFromHandler.select(editor).onError(r -> select(editors));
    }
  }
}
