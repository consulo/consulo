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
package consulo.ide.impl.idea.designer;

import consulo.application.ApplicationManager;
import consulo.component.PropertiesComponent;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * @author Alexander Lobas
 */
public abstract class LightToolWindowManager implements Disposable {
  public static final String EDITOR_MODE = "UI_DESIGNER_EDITOR_MODE.";

  private final MergingUpdateQueue myWindowQueue = new MergingUpdateQueue(getClientPropertyName(), 200, true, null);
  protected final Project myProject;
  protected final FileEditorManager myFileEditorManager;
  protected volatile ToolWindow myToolWindow;
  private volatile boolean myToolWindowDisposed;

  private final PropertiesComponent myPropertiesComponent;
  public final String myEditorModeKey;
  private ToggleEditorModeAction myLeftEditorModeAction;
  private ToggleEditorModeAction myRightEditorModeAction;

  private MessageBusConnection myConnection;
  private final FileEditorManagerListener myListener = new FileEditorManagerListener() {
    @Override
    public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
      bindToDesigner(getActiveDesigner());
    }

    @Override
    public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          bindToDesigner(getActiveDesigner());
        }
      });
    }

    @Override
    public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
      bindToDesigner(getDesigner(event.getNewEditor()));
    }
  };

  //////////////////////////////////////////////////////////////////////////////////////////
  //
  // ToolWindow
  //
  //////////////////////////////////////////////////////////////////////////////////////////

  protected LightToolWindowManager(Project project, FileEditorManager fileEditorManager) {
    myProject = project;
    myFileEditorManager = fileEditorManager;
    myPropertiesComponent = ProjectPropertiesComponent.getInstance(myProject);
    myEditorModeKey = EDITOR_MODE + getClass().getSimpleName() + ".STATE";

    initListeners();
  }

  @Override
  public void dispose() {
    if (!myToolWindowDisposed) {
      disposeComponent();
      myToolWindowDisposed = true;
      myToolWindow = null;
    }
  }

  public void disposeComponent() {
  }

  private void initListeners() {
    myConnection = myProject.getMessageBus().connect(myProject);
    myConnection.subscribe(FileEditorManagerListener.class, myListener);
  }

  private void removeListeners() {
    myConnection.disconnect();
    myConnection = null;
  }

  @Nullable
  protected abstract DesignerEditorPanelFacade getDesigner(FileEditor editor);

  @Nullable
  public DesignerEditorPanelFacade getActiveDesigner() {
    for (FileEditor editor : myFileEditorManager.getSelectedEditors()) {
      DesignerEditorPanelFacade designer = getDesigner(editor);
      if (designer != null) {
        return designer;
      }
    }

    return null;
  }

  // obsolete?
  public void bindToActiveDesigner() {
    bindToDesigner(getActiveDesigner());
  }

  private void bindToDesigner(final DesignerEditorPanelFacade designer) {
    myWindowQueue.cancelAllUpdates();
    myWindowQueue.queue(new Update("update") {
      @Override
      public void run() {
        if (myToolWindowDisposed) {
          return;
        }
        if (myToolWindow == null && designer == null) {
          return;
        }

        updateToolWindow(designer);
      }
    });
  }

  protected abstract void updateToolWindow(@Nullable DesignerEditorPanelFacade designer);

  protected final void initGearActions() {
    ToolWindowEx toolWindow = (ToolWindowEx)myToolWindow;
    toolWindow.setAdditionalGearActions(new DefaultActionGroup(createGearActions()));
  }

  protected abstract ToolWindowAnchor getAnchor();

  //////////////////////////////////////////////////////////////////////////////////////////
  //
  // LightToolWindow
  //
  //////////////////////////////////////////////////////////////////////////////////////////

  public final ActionGroup createGearActions() {
    DefaultActionGroup group = new DefaultActionGroup("In Editor Mode", true);

    if (myLeftEditorModeAction == null) {
      myLeftEditorModeAction = createToggleAction(ToolWindowAnchor.LEFT);
    }
    group.add(myLeftEditorModeAction);

    if (myRightEditorModeAction == null) {
      myRightEditorModeAction = createToggleAction(ToolWindowAnchor.RIGHT);
    }
    group.add(myRightEditorModeAction);

    return group;
  }

  protected abstract ToggleEditorModeAction createToggleAction(ToolWindowAnchor anchor);

  public final void bind(@Nonnull DesignerEditorPanelFacade designer) {
    if (isEditorMode()) {
      myCreateAction.accept(designer);
    }
  }

  public final void dispose(@Nonnull DesignerEditorPanelFacade designer) {
    if (isEditorMode()) {
      disposeContent(designer);
    }
  }

  protected final Object getContent(@Nonnull DesignerEditorPanelFacade designer) {
    LightToolWindow toolWindow = (LightToolWindow)designer.getClientProperty(getClientPropertyName());
    return toolWindow.getContent();
  }

  protected abstract LightToolWindow createContent(@Nonnull DesignerEditorPanelFacade designer);

  protected final LightToolWindow createContent(@Nonnull DesignerEditorPanelFacade designer,
                                                @Nonnull LightToolWindowContent content,
                                                @Nonnull String title,
                                                @Nonnull Icon icon,
                                                @Nonnull JComponent component,
                                                @Nonnull JComponent focusedComponent,
                                                int defaultWidth,
                                                @Nullable AnAction[] actions) {
    return new LightToolWindow(content, title, icon, component, focusedComponent, designer.getContentSplitter(), getEditorMode(), this,
                               myProject, myPropertiesComponent, getClientPropertyName(), defaultWidth, actions);
  }

  protected final void disposeContent(DesignerEditorPanelFacade designer) {
    String key = getClientPropertyName();
    LightToolWindow toolWindow = (LightToolWindow)designer.getClientProperty(key);
    designer.putClientProperty(key, null);
    toolWindow.dispose();
  }

  private final Consumer<DesignerEditorPanelFacade> myCreateAction =
    designer -> designer.putClientProperty(getClientPropertyName(), createContent(designer));

  private final Consumer<DesignerEditorPanelFacade> myUpdateAnchorAction = designer -> {
    LightToolWindow toolWindow = (LightToolWindow)designer.getClientProperty(getClientPropertyName());
    toolWindow.updateAnchor(getEditorMode());
  };

  private final Consumer<DesignerEditorPanelFacade> myDisposeAction = designer -> disposeContent(designer);

  private void runUpdateContent(Consumer<DesignerEditorPanelFacade> action) {
    for (FileEditor editor : myFileEditorManager.getAllEditors()) {
      DesignerEditorPanelFacade designer = getDesigner(editor);
      if (designer != null) {
        action.accept(designer);
      }
    }
  }

  protected final boolean isEditorMode() {
    return getEditorMode() != null;
  }

  @Nullable
  final ToolWindowAnchor getEditorMode() {
    String value = myPropertiesComponent.getValue(myEditorModeKey);
    if (value == null) {
      return getAnchor();
    }
    return value.equals("ToolWindow") ? null : ToolWindowAnchor.fromText(value);
  }

  @Nonnull
  protected String getClientPropertyName() {
    return getClass().getSimpleName();
  }

  final void setEditorMode(@Nullable ToolWindowAnchor newState) {
    ToolWindowAnchor oldState = getEditorMode();
    myPropertiesComponent.setValue(myEditorModeKey, newState == null ? "ToolWindow" : newState.toString());

    if (oldState != null && newState != null) {
      runUpdateContent(myUpdateAnchorAction);
    }
    else if (newState != null) {
      removeListeners();
      updateToolWindow(null);
      runUpdateContent(myCreateAction);
    }
    else {
      runUpdateContent(myDisposeAction);
      initListeners();
      bindToDesigner(getActiveDesigner());
    }
  }

  final ToolWindow getToolWindow() {
    return myToolWindow;
  }
}