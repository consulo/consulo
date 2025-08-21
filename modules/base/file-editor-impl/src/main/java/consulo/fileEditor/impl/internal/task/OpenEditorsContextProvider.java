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

package consulo.fileEditor.impl.internal.task;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.DockableEditorTabbedContainer;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.project.ui.wm.dock.BaseDockManager;
import consulo.project.ui.wm.dock.DockContainer;
import consulo.project.ui.wm.dock.DockManager;
import consulo.task.context.WorkingContextProvider;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class OpenEditorsContextProvider extends WorkingContextProvider {
  private final FileEditorManagerImpl myFileEditorManager;
  private final BaseDockManager myDockManager;

  @Inject
  public OpenEditorsContextProvider(FileEditorManager fileEditorManager, DockManager dockManager) {
    myDockManager = (BaseDockManager)dockManager;
    myFileEditorManager = fileEditorManager instanceof FileEditorManagerImpl ? (FileEditorManagerImpl)fileEditorManager : null;
  }

  @Nonnull
  @Override
  public String getId() {
    return "editors";
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Open editors and positions";
  }

  @Override
  public void saveContext(Element element) {
    if (myFileEditorManager != null) {
      myFileEditorManager.getMainSplitters().writeExternal(element);
    }
    element.addContent(myDockManager.getState());
  }

  @Override
  public void loadContext(Element element) {
    CompletableFuture<?> future = CompletableFuture.completedFuture(null);
    if (myFileEditorManager != null) {
      myFileEditorManager.loadState(element);
      future = myFileEditorManager.getMainSplitters().openFilesAsync(UIAccess.current());
    }

    future.whenComplete((o, throwable) -> {

      Element dockState = element.getChild("DockManager");
      if (dockState != null) {
        myDockManager.loadState(dockState);
        myDockManager.readState();
      }
    });
  }

  @Override
  public void clearContext() {
    if (myFileEditorManager != null) {
      myFileEditorManager.closeAllFiles();
      myFileEditorManager.getMainSplitters().clear();
    }
    for (DockContainer container : myDockManager.getContainers()) {
      if (container instanceof DockableEditorTabbedContainer) {
        container.closeAll();
      }
    }
  }
}