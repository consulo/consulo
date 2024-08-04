/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.openapi.project;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.ide.impl.idea.openapi.vfs.impl.local.FileWatcher;
import consulo.ide.impl.idea.openapi.vfs.impl.local.LocalFileSystemImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.ui.UIAccess;
import consulo.util.collection.SmartList;
import consulo.util.io.FileUtil;
import consulo.util.lang.TimeoutUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ExtensionImpl
public class ProjectNetworkRootWatcherChecker implements BackgroundStartupActivity {
    private static final Logger LOG = Logger.getInstance(ProjectNetworkRootWatcherChecker.class);

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
        if (roots.length == 0) {
            return;
        }

        LocalFileSystem fs = LocalFileSystem.getInstance();
        if (!(fs instanceof LocalFileSystemImpl)) {
            return;
        }
        FileWatcher watcher = ((LocalFileSystemImpl) fs).getFileWatcher();
        if (!watcher.isOperational()) {
            //ProjectFsStatsCollector.watchedRoots(myProject, -1);
            return;
        }

        project.getApplication().executeOnPooledThread(() -> {
            LOG.debug("FW/roots waiting started");
            while (true) {
                if (project.isDisposed()) {
                    return;
                }
                if (!watcher.isSettingRoots()) {
                    break;
                }
                TimeoutUtil.sleep(10);
            }
            LOG.debug("FW/roots waiting finished");

            Collection<String> manualWatchRoots = watcher.getManualWatchRoots();
            int pctNonWatched = 0;
            if (!manualWatchRoots.isEmpty()) {
                List<String> nonWatched = new SmartList<>();
                for (VirtualFile root : roots) {
                    if (!(root.getFileSystem() instanceof LocalFileSystem)) {
                        continue;
                    }
                    String rootPath = root.getPath();
                    for (String manualWatchRoot : manualWatchRoots) {
                        if (FileUtil.isAncestor(manualWatchRoot, rootPath, false)) {
                            nonWatched.add(rootPath);
                        }
                    }
                }
                if (!nonWatched.isEmpty()) {
                    LocalizeValue message = ApplicationLocalize.watcherNonWatchableProject();
                    watcher.notifyOnFailure(message.get());
                    LOG.info("unwatched roots: " + nonWatched);
                    LOG.info("manual watches: " + manualWatchRoots);
                    pctNonWatched = (int) (100.0 * nonWatched.size() / roots.length);
                }
            }
            //ProjectFsStatsCollector.watchedRoots(myProject, pctNonWatched);
        });
    }
}
