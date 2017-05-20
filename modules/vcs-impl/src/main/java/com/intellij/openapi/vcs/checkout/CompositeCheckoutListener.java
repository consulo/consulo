/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.checkout;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;

import java.io.File;

/**
 * to be called after checkout - notifiers extenders on checkout completion
 */
public class CompositeCheckoutListener implements CheckoutProvider.Listener {
  private final Project myProject;
  private boolean myFoundProject = false;
  private File myFirstDirectory;
  private VcsKey myVcsKey;

  public CompositeCheckoutListener(final Project project) {
    myProject = project;
  }

  public void directoryCheckedOut(final File directory, VcsKey vcs) {
    myVcsKey = vcs;
    if (!myFoundProject) {
      final VirtualFile virtualFile = refreshVFS(directory);
      if (virtualFile != null) {
        if (myFirstDirectory == null) {
          myFirstDirectory = directory;
        }
        notifyCheckoutListeners(directory, CheckoutListener.EP_NAME);
      }
    }
  }

  private static VirtualFile refreshVFS(final File directory) {
    final Ref<VirtualFile> result = new Ref<VirtualFile>();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        final LocalFileSystem lfs = LocalFileSystem.getInstance();
        final VirtualFile vDir = lfs.refreshAndFindFileByIoFile(directory);
        result.set(vDir);
        if (vDir != null) {
          final LocalFileSystem.WatchRequest watchRequest = lfs.addRootToWatch(vDir.getPath(), true);
          ((NewVirtualFile)vDir).markDirtyRecursively();
          vDir.refresh(false, true);
          if (watchRequest != null) {
            lfs.removeWatchedRoot(watchRequest);
          }
        }
      }
    });
    return result.get();
  }

  private void notifyCheckoutListeners(final File directory, final ExtensionPointName<CheckoutListener> epName) {
    final CheckoutListener[] listeners = Extensions.getExtensions(epName);
    for (CheckoutListener listener: listeners) {
      myFoundProject = listener.processCheckedOutDirectory(myProject, directory);
      if (myFoundProject) break;
    }
    if (!myFoundProject && !epName.equals(CheckoutListener.COMPLETED_EP_NAME)) {
      final VcsAwareCheckoutListener[] vcsAwareExtensions = Extensions.getExtensions(VcsAwareCheckoutListener.EP_NAME);
      for (VcsAwareCheckoutListener extension : vcsAwareExtensions) {
        myFoundProject = extension.processCheckedOutDirectory(myProject, directory, myVcsKey);
        if (myFoundProject) break;
      }
    }
    final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    if (openProjects.length > 0){
      final Project lastOpenedProject = openProjects[openProjects.length - 1];
      for (CheckoutListener listener: listeners) {
        listener.processOpenedProject(lastOpenedProject);
      }
    }
  }

  public void checkoutCompleted() {
    if (!myFoundProject && myFirstDirectory != null) {
      notifyCheckoutListeners(myFirstDirectory, CheckoutListener.COMPLETED_EP_NAME);
    }
  }
}
