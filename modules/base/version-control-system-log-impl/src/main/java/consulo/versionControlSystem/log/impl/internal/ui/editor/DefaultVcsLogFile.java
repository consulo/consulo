/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.log.impl.internal.ui.editor;

import consulo.application.Application;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.impl.internal.VcsLogTabsUtil;
import consulo.versionControlSystem.log.impl.internal.VcsProjectLog;
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.virtualFileSystem.VirtualFileSystem;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The file of a log tab opened in an editor.
 */
public class DefaultVcsLogFile extends VcsLogFile {
    private final VcsLogVirtualFileSystem myFileSystem;
    private final String myPath;
    private final String myTabId;
    private @Nullable VcsLogFilterCollection myFilters;
    private @Nullable AbstractVcsLogUi myUi;

    DefaultVcsLogFile(VcsLogVirtualFileSystem fileSystem, String path, String tabId, @Nullable VcsLogFilterCollection filters) {
        super(VcsLogTabsUtil.getFullName(tabId));
        myFileSystem = fileSystem;
        myPath = path;
        myTabId = tabId;
        myFilters = filters;
    }

    public String getTabId() {
        return myTabId;
    }

    public String getTabName() {
        String tabName = Application.get().getInstance(VcsLogEditorTabNameCache.class).getTabName(getPath());
        return tabName != null ? tabName : getName();
    }

    private void setTabName(String tabName) {
        Application.get().getInstance(VcsLogEditorTabNameCache.class).putTabName(getPath(), tabName);
    }

    @RequiredUIAccess
    @Override
    public Component createMainComponent(Project project) {
        DockLayout panel = DockLayout.create();
        panel.center(Label.create(VersionControlSystemLogLocalize.logIsLoading()));

        VcsProjectLog.getInstance(project).runWhenLogIsReady(logManager -> {
            if (logManager.hasLogUi(myTabId)) {
                panel.center(Label.create(VersionControlSystemLogLocalize.duplicatedTabIdError()));
                return;
            }

            AbstractVcsLogUi ui = logManager.createLogUi(myTabId, null, null);
            if (myFilters != null) {
                ui.getFilterUi().setFilters(myFilters);
                myFilters = null;
            }
            myUi = ui;

            Disposer.register(ui, () -> {
                myUi = null;
                setValid(false);
                project.getApplication().invokeLater(
                    () -> FileEditorManager.getInstance(project).closeFile(this),
                    project.getDisposed()
                );
            });

            setTabName(VcsLogTabsUtil.generateDisplayName(ui));
            VcsLogTabsUtil.onDisplayNameChange(ui, () -> {
                setTabName(VcsLogTabsUtil.generateDisplayName(ui));
                FileEditorManager.getInstance(project).updateFilePresentation(this);
            });

            panel.center(ui.createPanel(logManager));
        });
        return panel;
    }

    @Override
    public List<AbstractVcsLogUi> getLogUis() {
        AbstractVcsLogUi ui = myUi;
        return ui == null ? List.of() : List.of(ui);
    }

    @RequiredUIAccess
    @Override
    public void disposeLogUis() {
        AbstractVcsLogUi ui = myUi;
        if (ui != null) {
            Disposer.dispose(ui);
        }
    }

    @Override
    public VirtualFileSystem getFileSystem() {
        return myFileSystem;
    }

    @Override
    public String getPath() {
        return myPath;
    }

    @Override
    public String getPresentableName() {
        return getTabName();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DefaultVcsLogFile that && myTabId.equals(that.myTabId);
    }

    @Override
    public int hashCode() {
        return myTabId.hashCode();
    }
}
