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

import consulo.application.Application;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectOpenService;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

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
