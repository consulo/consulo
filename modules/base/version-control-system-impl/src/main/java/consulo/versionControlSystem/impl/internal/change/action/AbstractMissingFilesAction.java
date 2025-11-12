/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.progress.ProgressManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.impl.internal.change.ChangesViewManagerImpl;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesListViewImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 * @since 2006-11-02
 */
public abstract class AbstractMissingFilesAction extends AnAction implements DumbAware {
    protected AbstractMissingFilesAction() {
    }

    protected AbstractMissingFilesAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    @Override
    public void update(AnActionEvent e) {
        List<FilePath> files = e.getData(ChangesListViewImpl.MISSING_FILES_DATA_KEY);
        boolean enabled = files != null && !files.isEmpty();
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        List<FilePath> files = e.getRequiredData(ChangesListViewImpl.MISSING_FILES_DATA_KEY);
        ProgressManager progressManager = ProgressManager.getInstance();
        Runnable action = () -> {
            List<VcsException> allExceptions = new ArrayList<>();
            ChangesUtil.processFilePathsByVcs(project, files, (vcs, items) -> {
                List<VcsException> exceptions = processFiles(vcs, files);
                if (exceptions != null) {
                    allExceptions.addAll(exceptions);
                }
            });

            for (FilePath file : files) {
                VcsDirtyScopeManager.getInstance(project).fileDirty(file);
            }
            ChangesViewManagerImpl.getInstance(project).scheduleRefresh();
            if (allExceptions.size() > 0) {
                AbstractVcsHelper.getInstance(project).showErrors(allExceptions, "VCS Errors");
            }
        };
        if (synchronously()) {
            action.run();
        }
        else {
            progressManager.runProcessWithProgressSynchronously(action, getName(), true, project);
        }
    }

    protected abstract boolean synchronously();

    @Nonnull
    protected abstract LocalizeValue getName();

    protected abstract List<VcsException> processFiles(AbstractVcs vcs, List<FilePath> files);
}