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
package consulo.project.impl.internal;

import consulo.application.util.concurrent.PooledAsyncResult;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.application.Application;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectOpenService;

import java.nio.file.Path;
import consulo.project.internal.*;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Alert;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;

import java.util.concurrent.CompletableFuture;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Eugene Belyaev
 */
public class ProjectImplUtil {
    private static final Logger LOG = Logger.getInstance(ProjectImplUtil.class);

    private ProjectImplUtil() {
    }

    public static void updateLastProjectLocation(String projectFilePath) {
        File lastProjectLocation = new File(projectFilePath);
        if (lastProjectLocation.isFile()) {
            lastProjectLocation = lastProjectLocation.getParentFile(); // for directory-based project storage
        }
        if (lastProjectLocation == null) { // the immediate parent of the ipr file
            return;
        }
        lastProjectLocation = lastProjectLocation.getParentFile(); // the candidate directory to be saved
        if (lastProjectLocation == null) {
            return;
        }
        String path = lastProjectLocation.getPath();
        try {
            path = FileUtil.resolveShortWindowsName(path);
        }
        catch (IOException e) {
            LOG.info(e);
            return;
        }
        RecentProjectsManager.getInstance().setLastProjectCreationLocation(path.replace(File.separatorChar, '/'));
    }

    private static AsyncResult<Integer> confirmOpenNewProjectAsync(Project projectToClose, UIAccess uiAccess, boolean isNewProject) {
        ProjectOpenSetting settings = ProjectOpenSetting.getInstance();
        int confirmOpenNewProject = settings.getConfirmOpenNewProject();
        if (confirmOpenNewProject == ProjectOpenSetting.OPEN_PROJECT_ASK) {
            Alert<Integer> alert = Alert.create();
            alert.asQuestion();
            alert.remember(ProjectNewWindowDoNotAskOption.INSTANCE);
            alert.title(isNewProject ? ProjectLocalize.titleNewProject() : ProjectLocalize.titleOpenProject());
            alert.text(ProjectLocalize.promptOpenProjectInNewFrame());

            alert.button(ProjectLocalize.buttonExistingframe(), () -> ProjectOpenSetting.OPEN_PROJECT_SAME_WINDOW);
            alert.asDefaultButton();

            alert.button(ProjectLocalize.buttonNewframe(), () -> ProjectOpenSetting.OPEN_PROJECT_NEW_WINDOW);

            alert.button(Alert.CANCEL, Alert.CANCEL);
            alert.asExitButton();

            AsyncResult<Integer> result = AsyncResult.undefined();
            uiAccess.give(() -> {
                if (projectToClose != null) {
                    return alert.showAsync(projectToClose).notify(result);
                }
                else {
                    return alert.showAsync().notify(result);
                }
            });
            return result;
        }

        return AsyncResult.resolved(confirmOpenNewProject);
    }

    
    public static AsyncResult<Project> openAsync(String path,
                                                 @Nullable Project projectToCloseFinal,
                                                 boolean forceOpenInNewFrame,
                                                 UIAccess uiAccess) {
        return openAsync(path, projectToCloseFinal, forceOpenInNewFrame, uiAccess, new ProjectOpenContext());
    }

    
    public static AsyncResult<Project> openAsync(String path,
                                                 @Nullable Project projectToCloseFinal,
                                                 boolean forceOpenInNewFrame,
                                                 UIAccess uiAccess,
                                                 ProjectOpenContext context) {
        if (projectToCloseFinal != null) {
            context.putUserData(ProjectOpenContext.ACTIVE_PROJECT, projectToCloseFinal);
        }
        if (forceOpenInNewFrame) {
            context.putUserData(ProjectOpenContext.FORCE_OPEN_IN_NEW_FRAME, Boolean.TRUE);
        }

        AsyncResult<Project> result = AsyncResult.undefined();
        result.doWhenRejected(() -> WelcomeFrameManager.getInstance().showIfNoProjectOpened());

        ProjectOpenService service = Application.get().getInstance(ProjectOpenService.class);
        service.openProjectAsync(Path.of(path), uiAccess, context).whenComplete((project, throwable) -> {
            if (throwable != null) {
                result.rejectWithThrowable(throwable);
            }
            else if (project != null) {
                result.setDone(project);
            }
            else {
                result.setRejected();
            }
        });

        return result;
    }
}
