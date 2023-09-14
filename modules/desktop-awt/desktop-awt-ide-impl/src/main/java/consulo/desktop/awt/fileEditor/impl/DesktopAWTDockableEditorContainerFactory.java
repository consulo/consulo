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
package consulo.desktop.awt.fileEditor.impl;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.fileEditor.impl.DockableEditorContainerFactory;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileEditorManagerImpl;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.dock.DockContainer;
import consulo.project.ui.wm.dock.DockManager;
import consulo.project.ui.wm.dock.DockableContent;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;

public class DesktopAWTDockableEditorContainerFactory implements DockableEditorContainerFactory {
  private final ApplicationConcurrency myApplicationConcurrency;
  private final Project myProject;
  private final FileEditorManagerImpl myFileEditorManager;
  private final DockManager myDockManager;

  public DesktopAWTDockableEditorContainerFactory(ApplicationConcurrency applicationConcurrency,
                                                  Project project,
                                                  FileEditorManagerImpl fileEditorManager,
                                                  DockManager dockManager) {
    myApplicationConcurrency = applicationConcurrency;
    this.myProject = project;
    myFileEditorManager = fileEditorManager;
    myDockManager = dockManager;
  }

  @Override
  public DockContainer createContainer(DockableContent content) {
    return createContainer(false);
  }

  private DockContainer createContainer(boolean loadingState) {
    final SimpleReference<DesktopDockableEditorTabbedContainer> containerRef = SimpleReference.create();
    DesktopFileEditorsSplitters splitters =
      new DesktopFileEditorsSplitters(myProject, myApplicationConcurrency, myFileEditorManager, myDockManager, false) {
        @Override
        public void afterFileClosed(VirtualFile file) {
          containerRef.get().fireContentClosed(file);
        }

        @Override
        public void afterFileOpen(VirtualFile file) {
          containerRef.get().fireContentOpen(file);
        }

        @Override
        protected IdeFrame getFrame(Project project) {
          return DockManager.getInstance(project).getIdeFrame(containerRef.get());
        }

        @Override
        public boolean isFloating() {
          return true;
        }
      };
    if (!loadingState) {
      splitters.createCurrentWindow();
    }
    final DesktopDockableEditorTabbedContainer container = new DesktopDockableEditorTabbedContainer(myProject, splitters, true);
    Disposer.register(container, splitters);
    containerRef.set(container);
    container.getSplitters().startListeningFocus();
    return container;
  }

  @Override
  public DockContainer loadContainerFrom(Element element) {
    DesktopDockableEditorTabbedContainer container = (DesktopDockableEditorTabbedContainer)createContainer(true);
    container.getSplitters().readExternal(element.getChild("state"));
    return container;
  }

  @Override
  public void dispose() {
  }
}
