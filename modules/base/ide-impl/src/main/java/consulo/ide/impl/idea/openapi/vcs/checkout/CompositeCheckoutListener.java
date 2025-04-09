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
package consulo.ide.impl.idea.openapi.vcs.checkout;

import consulo.application.Application;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.checkout.*;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.List;

/**
 * to be called after checkout - notifiers extenders on checkout completion
 */
public class CompositeCheckoutListener implements CheckoutProvider.Listener {
    private final Project myProject;
    private boolean myFoundProject = false;
    private File myFirstDirectory;
    private VcsKey myVcsKey;

    public CompositeCheckoutListener(Project project) {
        myProject = project;
    }

    @Override
    @RequiredUIAccess
    public void directoryCheckedOut(File directory, VcsKey vcs) {
        myVcsKey = vcs;
        if (!myFoundProject) {
            VirtualFile virtualFile = refreshVFS(directory);
            if (virtualFile != null) {
                if (myFirstDirectory == null) {
                    myFirstDirectory = directory;
                }
                notifyCheckoutListeners(directory, PreCheckoutListener.class);
            }
        }
    }

    @RequiredUIAccess
    private static VirtualFile refreshVFS(File directory) {
        SimpleReference<VirtualFile> result = new SimpleReference<>();
        Application.get().runWriteAction(() -> {
            LocalFileSystem lfs = LocalFileSystem.getInstance();
            VirtualFile vDir = lfs.refreshAndFindFileByIoFile(directory);
            result.set(vDir);
            if (vDir != null) {
                LocalFileSystem.WatchRequest watchRequest = lfs.addRootToWatch(vDir.getPath(), true);
                ((NewVirtualFile)vDir).markDirtyRecursively();
                vDir.refresh(false, true);
                if (watchRequest != null) {
                    lfs.removeWatchedRoot(watchRequest);
                }
            }
        });
        return result.get();
    }

    private void notifyCheckoutListeners(File directory, Class<? extends CheckoutListener> checkoutListenerEP) {
        List<? extends CheckoutListener> listeners = Application.get().getExtensionList(checkoutListenerEP);
        for (CheckoutListener listener : listeners) {
            myFoundProject = listener.processCheckedOutDirectory(myProject, directory);
            if (myFoundProject) {
                break;
            }
        }
        if (!myFoundProject && checkoutListenerEP != CompletedCheckoutListener.class) {
            List<VcsAwareCheckoutListener> vcsAwareExtensions = VcsAwareCheckoutListener.EP_NAME.getExtensionList();
            for (VcsAwareCheckoutListener extension : vcsAwareExtensions) {
                myFoundProject = extension.processCheckedOutDirectory(myProject, directory, myVcsKey);
                if (myFoundProject) {
                    break;
                }
            }
        }
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            Project lastOpenedProject = openProjects[openProjects.length - 1];
            for (CheckoutListener listener : listeners) {
                listener.processOpenedProject(lastOpenedProject);
            }
        }
    }

    @Override
    public void checkoutCompleted() {
        if (!myFoundProject && myFirstDirectory != null) {
            notifyCheckoutListeners(myFirstDirectory, CompletedCheckoutListener.class);
        }
    }

    @Nullable
    static Project findProjectByBaseDirLocation(@Nonnull File directory) {
        return ContainerUtil.find(ProjectManager.getInstance().getOpenProjects(), project -> {
            VirtualFile baseDir = project.getBaseDir();
            return baseDir != null && FileUtil.filesEqual(VfsUtilCore.virtualToIoFile(baseDir), directory);
        });
    }
}
