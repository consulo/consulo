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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowserBase;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.Streams;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.impl.internal.change.ui.awt.IgnoreUnversionedDialog;
import consulo.versionControlSystem.change.ChangesBrowserApi;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.stream.Collectors;

import static consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesListViewImpl.UNVERSIONED_FILES_DATA_KEY;

/**
 * @author yole
 * @since 2006-12-20
 */
@ActionImpl(id = "ChangesView.Ignore")
public class IgnoreUnversionedAction extends AnAction {
    public IgnoreUnversionedAction() {
        super(
            ActionLocalize.actionChangesviewIgnoreText(),
            ActionLocalize.actionChangesviewIgnoreDescription(),
            PlatformIconGroup.filetypesIgnored()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);

        if (!ChangeListManager.getInstance(project).isFreezedWithNotification(null)) {
            List<VirtualFile> files = e.getRequiredData(UNVERSIONED_FILES_DATA_KEY).collect(Collectors.toList());
            ChangesBrowserBase<?> browser = (ChangesBrowserBase<?>) e.getData(ChangesBrowserApi.DATA_KEY);
            Runnable callback = browser == null ? null : () -> {
                browser.rebuildList();
                //noinspection unchecked
                browser.getViewer().excludeChanges((List) files);
            };

            IgnoreUnversionedDialog.ignoreSelectedFiles(project, files, callback);
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY) && !Streams.isEmpty(e.getData(UNVERSIONED_FILES_DATA_KEY)));
    }
}