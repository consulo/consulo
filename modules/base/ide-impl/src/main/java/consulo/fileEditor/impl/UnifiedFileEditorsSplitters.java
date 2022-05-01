/*
 * Copyright 2013-2018 consulo.io
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
package consulo.fileEditor.impl;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileEditorManagerImpl;
import consulo.fileEditor.FileEditorWindow;
import consulo.project.Project;
import consulo.project.ui.wm.dock.DockManager;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.docking.impl.UnifiedDockableEditorTabbedContainer;
import consulo.ui.layout.WrappedLayout;
import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedFileEditorsSplitters extends FileEditorsSplittersBase<UnifiedFileEditorWindow> {
  private static final Logger LOG = Logger.getInstance(UnifiedFileEditorsSplitters.class);
  
  private final Project myProject;

  private WrappedLayout myLayout;

  public UnifiedFileEditorsSplitters(Project project, FileEditorManagerImpl editorManager, DockManager dockManager, boolean createOwnDockableContainer) {
    super(project, editorManager);
    myProject = project;

    myLayout = WrappedLayout.create();

    if (createOwnDockableContainer) {
      UnifiedDockableEditorTabbedContainer dockable = new UnifiedDockableEditorTabbedContainer(myManager.getProject(), this, false);
      Disposer.register(editorManager.getProject(), dockable);
      dockManager.register(dockable);
    }
  }

  @Nonnull
  @Override
  protected UnifiedFileEditorWindow[] createArray(int size) {
    return new UnifiedFileEditorWindow[size];
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    return myLayout;
  }

  @Override
  public void writeExternal(Element element) {

  }

  @Override
  public void openFiles(@Nonnull UIAccess uiAccess) {

  }

  @Override
  public int getSplitCount() {
    return 0;
  }

  @Override
  public void startListeningFocus() {

  }

  @Override
  public void clear() {
    for (UnifiedFileEditorWindow window : myWindows) {
      window.dispose();
    }
    //todo myComponent.removeAll();
    myWindows.clear();
    setCurrentWindow(null);
    //todo myComponent.repaint(); // revalidate doesn't repaint correctly after "Close All"
  }

  @RequiredUIAccess
  @Override
  protected void createCurrentWindow() {
    LOG.assertTrue(myCurrentWindow == null);
    setCurrentWindow(new UnifiedFileEditorWindow(myProject, myManager, this));
    myLayout.set(myCurrentWindow.getUIComponent());
  }

  @Nonnull
  @Override
  protected IdeaModalityState getComponentModality() {
    return IdeaModalityState.any();
  }

  @Override
  public FileEditorWindow[] getOrderedWindows() {
    if (myCurrentWindow != null) {
      return new FileEditorWindow[]{myCurrentWindow};
    }
    return FileEditorWindow.EMPTY_ARRAY;
  }

  @Override
  public boolean isShowing() {
    return true;
  }
}
