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
package consulo.versionControlSystem.log.impl.internal;

import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogTabsProperties;
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.editor.DefaultVcsLogFile;
import consulo.versionControlSystem.log.impl.internal.ui.editor.VcsLogVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Opens the additional log tabs, in the tool window or in editors, and reopens them after a restart.
 */
public class VcsLogTabsManager implements Disposable {
    private static final Logger LOG = Logger.getInstance(VcsLogTabsManager.class);

    public static final Key<String> TAB_ID_KEY = Key.create("VcsLogTabsManager.TabId");

    private final Project myProject;
    private final VcsLogTabsProperties myUiProperties;
    private final VcsLogManager myLogManager;
    private final CompletableFuture<ToolWindow> myFutureToolWindow = new CompletableFuture<>();
    private boolean myTabsCreated;
    private boolean myFilesListenerInstalled;
    private boolean myToolWindowListenerInstalled;
    private boolean myDisposed;

    public VcsLogTabsManager(Project project, VcsLogTabsProperties uiProperties, VcsLogManager logManager) {
        myProject = project;
        myUiProperties = uiProperties;
        myLogManager = logManager;
    }

    @RequiredUIAccess
    public void createTabs() {
        if (myTabsCreated) {
            return;
        }
        myTabsCreated = true;

        Map<String, VcsLogTabLocation> savedTabs = myUiProperties.getTabs();
        if (savedTabs.isEmpty()) {
            return;
        }

        List<String> editorTabs = new ArrayList<>();
        List<String> toolWindowTabs = new ArrayList<>();
        for (Map.Entry<String, VcsLogTabLocation> entry : savedTabs.entrySet()) {
            switch (entry.getValue()) {
                case EDITOR -> editorTabs.add(entry.getKey());
                case TOOL_WINDOW -> toolWindowTabs.add(entry.getKey());
                default -> LOG.warn("Reopening standalone tabs is not supported");
            }
        }
        closeTabsWithoutFilters(editorTabs);
        closeTabsWithoutFilters(toolWindowTabs);

        if (!editorTabs.isEmpty()) {
            myProject.getApplication().invokeLater(() -> {
                if (myDisposed) {
                    return;
                }
                for (String tabId : editorTabs) {
                    openEditorLogTab(tabId, false, null);
                }
            }, myProject.getDisposed());
        }

        if (!toolWindowTabs.isEmpty()) {
            myFutureToolWindow.whenComplete((toolWindow, error) -> {
                if (error != null || myDisposed) {
                    return;
                }
                for (String tabId : toolWindowTabs) {
                    openToolWindowLogTab(toolWindow, tabId, false, null);
                }
            });
        }

        myProject.getApplication().invokeLater(() -> {
            if (myDisposed) {
                return;
            }

            ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(VcsToolWindow.ID);
            if (toolWindow == null) {
                LOG.error("Could not find tool window by id " + VcsToolWindow.ID);
                return;
            }

            if (toolWindow.isVisible()) {
                myFutureToolWindow.complete(toolWindow);
            }
        }, myProject.getDisposed());
    }

    private void closeTabsWithoutFilters(List<String> tabs) {
        if (Registry.is("vcs.log.tabs.restore.without.filters", false)) {
            return;
        }

        Iterator<String> iterator = tabs.iterator();
        boolean oneTabPreserved = false;
        while (iterator.hasNext()) {
            String tabId = iterator.next();
            if (myUiProperties.checkTabHasFilters(tabId)) {
                continue;
            }

            if (!oneTabPreserved) {
                oneTabPreserved = true;
            }
            else {
                iterator.remove();
                myUiProperties.removeTab(tabId);
            }
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    @RequiredUIAccess
    public void toolWindowShown(ToolWindow toolWindow) {
        myFutureToolWindow.complete(toolWindow);
    }

    @RequiredUIAccess
    public @Nullable AbstractVcsLogUi openAnotherLogTab(VcsLogFilterCollection filters, VcsLogTabLocation location) {
        if (myDisposed) {
            throw new IllegalStateException("Already disposed");
        }

        String tabId = myLogManager.generateNewLogId();
        myUiProperties.resetState(tabId);
        if (location == VcsLogTabLocation.EDITOR) {
            FileEditor[] editors = openEditorLogTab(tabId, true, filters);
            return VcsLogEditorUtil.findVcsLogUi(editors, AbstractVcsLogUi.class);
        }
        else if (location == VcsLogTabLocation.TOOL_WINDOW) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(VcsToolWindow.ID);
            if (toolWindow == null) {
                throw new IllegalStateException("Could not find tool window for id " + VcsToolWindow.ID);
            }
            myFutureToolWindow.complete(toolWindow);
            return openToolWindowLogTab(toolWindow, tabId, true, filters);
        }
        throw new UnsupportedOperationException("Only log in editor or tool window is supported");
    }

    @RequiredUIAccess
    private FileEditor[] openEditorLogTab(String tabId, boolean focus, @Nullable VcsLogFilterCollection filters) {
        VirtualFile file = VcsLogVirtualFileSystem.getInstance().createVcsLogFile(myProject, tabId, filters);
        installFilesListener();
        myUiProperties.addTab(tabId, VcsLogTabLocation.EDITOR);
        return FileEditorManager.getInstance(myProject).openFile(file, focus, true);
    }

    private void installFilesListener() {
        if (myFilesListenerInstalled) {
            return;
        }
        myProject.getMessageBus().connect(this).subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
            @Override
            public void fileClosed(FileEditorManager source, VirtualFile file) {
                if (!(file instanceof DefaultVcsLogFile logFile)) {
                    return;
                }
                if (myProject.isDisposed()) {
                    return;
                }
                myUiProperties.removeTab(logFile.getTabId());
            }
        });
        myFilesListenerInstalled = true;
    }

    @RequiredUIAccess
    private AbstractVcsLogUi openToolWindowLogTab(ToolWindow toolWindow,
                                                 String tabId,
                                                 boolean focus,
                                                 @Nullable VcsLogFilterCollection filters) {
        AbstractVcsLogUi ui = myLogManager.createLogUi(tabId, tabId, null);
        if (filters != null) {
            ui.getFilterUi().setFilters(filters);
        }

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = ContentFactory.getInstance()
            .createUIContent(ui.createPanel(myLogManager), VcsLogTabsUtil.generateDisplayName(ui), false);
        content.putUserData(Content.TAB_GROUP_NAME_KEY, VcsLogContentProvider.TAB_NAME);
        content.putUserData(TAB_ID_KEY, tabId);
        content.setDisposer(ui);
        content.setCloseable(true);
        contentManager.addContent(content);
        if (focus) {
            contentManager.setSelectedContent(content);
            toolWindow.activate(null);
        }

        VcsLogTabsUtil.onDisplayNameChange(ui, () -> content.setDisplayName(VcsLogTabsUtil.generateDisplayName(ui)));

        installContentListener(toolWindow);
        myUiProperties.addTab(tabId, VcsLogTabLocation.TOOL_WINDOW);

        myLogManager.scheduleInitialization();
        return ui;
    }

    private void installContentListener(ToolWindow toolWindow) {
        if (myToolWindowListenerInstalled) {
            return;
        }
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(ContentManagerEvent event) {
                if (myProject.isDisposed()) {
                    return;
                }
                String id = event.getContent().getUserData(TAB_ID_KEY);
                if (id != null) {
                    myUiProperties.removeTab(id);
                }
            }
        }, this);
        myToolWindowListenerInstalled = true;
    }

    public Set<String> getTabs() {
        return myUiProperties.getTabs().keySet();
    }

    public Set<String> getTabs(VcsLogTabLocation location) {
        Set<String> tabs = new LinkedHashSet<>();
        for (Map.Entry<String, VcsLogTabLocation> entry : myUiProperties.getTabs().entrySet()) {
            if (entry.getValue() == location) {
                tabs.add(entry.getKey());
            }
        }
        return tabs;
    }
}
