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

package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileChooserKeys;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.ModulesProvider;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VirtualFileManagerListener;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author Eugene Zhuravlev
 * @since 2003-10-04
 */
public class ContentEntriesEditor extends ModuleElementsEditor {
    private static final Logger LOG = Logger.getInstance(ContentEntriesEditor.class);

    private final String myModuleName;
    private final ModulesProvider myModulesProvider;
    private final ModuleConfigurationState myState;
    private final Map<ContentEntry, ContentEntryEditor> myEntryToEditorMap = new LinkedHashMap<>();
    private final ContentEntryEditorListener myContentEntryEditorListener = new MyContentEntryEditorListener();

    private @Nullable ContentEntryTreeEditor myRootTreeEditor;
    private @Nullable DockLayout myEntriesLayout;
    private @Nullable ContentEntry mySelectedEntry;
    private @Nullable VirtualFile myLastSelectedDir;
    private boolean myRebuildScheduled;
    private int myRebuildStamp;

    public ContentEntriesEditor(String moduleName, ModuleConfigurationState state) {
        super(state);
        myState = state;
        myModuleName = moduleName;
        myModulesProvider = state.getModulesConfigurator();
    }

    @Override
    protected ModifiableRootModel getModel() {
        return myState.getRootModel();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectLocalize.modulePathsTitle();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        ContentEntryTreeEditor treeEditor = myRootTreeEditor;
        if (treeEditor != null) {
            treeEditor.setContentEntryEditor(null);
        }

        myEntryToEditorMap.clear();
        super.disposeUIResources();
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponentImpl(Disposable parentUIDisposable) {
        UIAccess uiAccess = UIAccess.current();

        ContentEntryTreeEditor treeEditor = new ContentEntryTreeEditor(myProject, myState);
        myRootTreeEditor = treeEditor;
        Component treeComponent = treeEditor.createComponent(parentUIDisposable);

        AddContentEntryAction addAction = new AddContentEntryAction();
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            ActionPlaces.UNKNOWN,
            ActionGroup.newImmutableBuilder().add(addAction).build(),
            ActionToolbar.Style.HORIZONTAL
        );

        DockLayout entriesLayout = DockLayout.create(Space.NONE);
        myEntriesLayout = entriesLayout;

        DockLayout entriesPanel = DockLayout.create(Space.NONE);
        entriesPanel.top(toolbar.getUIComponent());
        entriesPanel.center(entriesLayout);
        toolbar.setTargetUIComponent(entriesPanel);

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(treeComponent);
        splitLayout.setSecondComponent(entriesPanel);
        splitLayout.setProportion(60);

        addAction.registerCustomShortcutSet(CustomShortcutSet.fromString("alt C"), splitLayout, parentUIDisposable);

        VirtualFileManager.getInstance().addVirtualFileManagerListener(new VirtualFileManagerListener() {
            @Override
            public void afterRefreshFinish(boolean asynchronous) {
                uiAccess.give(() -> onFileSystemRefreshed());
            }
        }, parentUIDisposable);

        reloadEntries();
        return splitLayout;
    }

    @RequiredUIAccess
    private void onFileSystemRefreshed() {
        Module module = getModule();
        if (myProject.isDisposed() || module == null || module.isDisposed() || myEntriesLayout == null) {
            return;
        }

        scheduleRebuild();

        ContentEntryTreeEditor treeEditor = myRootTreeEditor;
        if (treeEditor != null) {
            treeEditor.scheduleRefresh();
        }
    }

    protected @Nullable Module getModule() {
        return myModulesProvider.getModule(myModuleName);
    }

    @Override
    @RequiredUIAccess
    public void moduleStateChanged() {
        if (myEntriesLayout == null) {
            return;
        }

        reloadEntries();
    }

    @RequiredUIAccess
    private void reloadEntries() {
        ContentEntryTreeEditor treeEditor = myRootTreeEditor;
        if (treeEditor == null) {
            return;
        }

        myEntryToEditorMap.clear();

        ModifiableRootModel model = getModel();
        boolean onlySingleFile = model.getModule().getModuleDirPath() == null;
        for (ContentEntry contentEntry : model.getContentEntries()) {
            ContentEntryEditor editor = new ContentEntryEditor(contentEntry, onlySingleFile, this::getModel);
            editor.addContentEntryEditorListener(myContentEntryEditorListener);
            myEntryToEditorMap.put(contentEntry, editor);
        }

        treeEditor.updateMarkActions();

        ContentEntry selectedEntry = mySelectedEntry;
        if (selectedEntry == null || !myEntryToEditorMap.containsKey(selectedEntry)) {
            selectedEntry = myEntryToEditorMap.isEmpty() ? null : myEntryToEditorMap.keySet().iterator().next();
        }

        mySelectedEntry = selectedEntry;
        treeEditor.setContentEntryEditor(selectedEntry == null ? null : myEntryToEditorMap.get(selectedEntry));

        scheduleRebuild();
    }

    @RequiredUIAccess
    void selectContentEntry(@Nullable ContentEntry contentEntry) {
        mySelectedEntry = contentEntry;

        ContentEntryTreeEditor treeEditor = myRootTreeEditor;
        if (treeEditor != null) {
            treeEditor.setContentEntryEditor(contentEntry == null ? null : myEntryToEditorMap.get(contentEntry));
        }
    }

    @RequiredUIAccess
    private void scheduleRebuild() {
        if (myRebuildScheduled || myEntriesLayout == null) {
            return;
        }

        myRebuildScheduled = true;
        UIAccess.current().give(() -> {
            myRebuildScheduled = false;
            rebuildEntriesLayout();
        });
    }

    @RequiredUIAccess
    private void rebuildEntriesLayout() {
        DockLayout entriesLayout = myEntriesLayout;
        if (entriesLayout == null) {
            return;
        }

        List<ContentEntryEditor> editors = new ArrayList<>(myEntryToEditorMap.values());
        List<String> urls = new ArrayList<>();
        for (ContentEntryEditor editor : editors) {
            urls.addAll(editor.collectUrls());
        }

        int stamp = ++myRebuildStamp;
        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.supplyAsync(() -> ReadAction.compute(() -> findMissingUrls(urls)), AppExecutorUtil.getAppExecutorService())
            .whenCompleteAsync((missingUrls, error) -> {
                if (stamp != myRebuildStamp || myEntriesLayout != entriesLayout) {
                    return;
                }

                if (error != null) {
                    LOG.error(error);
                }

                Set<String> missing = missingUrls == null ? Set.of() : missingUrls;
                VerticalLayout column = VerticalLayout.create(Space.NONE);
                for (ContentEntryEditor editor : editors) {
                    column.add(editor.createComponent(missing));
                }
                entriesLayout.center(ScrollableLayout.create(
                    column,
                    ScrollableLayoutOptions.builder().horizontalScrollPolicy(ScrollableLayoutOptions.ScrollPolicy.NEVER).build()
                ));
            }, uiAccess);
    }

    private static Set<String> findMissingUrls(List<String> urls) {
        VirtualFileManager fileManager = VirtualFileManager.getInstance();
        Set<String> missing = new HashSet<>();
        for (String url : urls) {
            if (fileManager.findFileByUrl(url) == null) {
                missing.add(url);
            }
        }
        return missing;
    }

    private @Nullable ContentEntry getNextContentEntry(ContentEntry contentEntry) {
        ContentEntry[] contentEntries = getModel().getContentEntries();
        for (int i = 0; i < contentEntries.length; i++) {
            if (contentEntry.equals(contentEntries[i])) {
                int next = (i + 1) % contentEntries.length;
                return next == i ? null : contentEntries[next];
            }
        }
        return null;
    }

    protected List<ContentEntry> addContentEntries(VirtualFile[] files) {
        List<ContentEntry> contentEntries = new ArrayList<>();
        for (VirtualFile file : files) {
            if (isAlreadyAdded(file)) {
                continue;
            }
            contentEntries.add(getModel().addContentEntry(file));
        }
        return contentEntries;
    }

    private boolean isAlreadyAdded(VirtualFile file) {
        for (VirtualFile contentRoot : getModel().getContentRoots()) {
            if (contentRoot.equals(file)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveData() {
    }

    private final class MyContentEntryEditorListener implements ContentEntryEditorListener {
        @Override
        @RequiredUIAccess
        public void editingStarted(ContentEntryEditor editor) {
            selectContentEntry(editor.getContentEntry());
        }

        @Override
        @RequiredUIAccess
        public void beforeEntryDeleted(ContentEntryEditor editor) {
            ContentEntry contentEntry = editor.getContentEntry();
            if (contentEntry.equals(mySelectedEntry)) {
                mySelectedEntry = getNextContentEntry(contentEntry);
            }
        }

        @Override
        @RequiredUIAccess
        public void foldersChanged(ContentEntryEditor editor) {
            scheduleRebuild();
        }

        @Override
        @RequiredUIAccess
        public void navigationRequested(ContentEntryEditor editor, String folderUrl) {
            selectContentEntry(editor.getContentEntry());

            ContentEntryTreeEditor treeEditor = myRootTreeEditor;
            if (treeEditor != null) {
                treeEditor.select(folderUrl);
            }
        }
    }

    private class AddContentEntryAction extends DumbAwareAction {
        private final FileChooserDescriptor myDescriptor;

        public AddContentEntryAction() {
            super(
                ProjectLocalize.modulePathsAddContentAction(),
                ProjectLocalize.modulePathsAddContentActionDescription(),
                PlatformIconGroup.generalAdd()
            );
            // compares with the content roots of the project model
            myDescriptor = new FileChooserDescriptor(false, true, true, false, true, true)
                .withVirtualFilesValidator(AddContentEntryAction.this::validateContentEntriesCandidates);
            myDescriptor.putUserData(LangDataKeys.MODULE_CONTEXT, getModule());
            myDescriptor.withTitle(ProjectLocalize.modulePathsAddContentTitle());
            myDescriptor.withDescription(ProjectLocalize.modulePathsAddContentPrompt());
            myDescriptor.putUserData(FileChooserKeys.DELETE_ACTION_AVAILABLE, false);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            UIAccess uiAccess = UIAccess.current();
            FileChooser.chooseFiles(myDescriptor, myProject, myLastSelectedDir).whenCompleteAsync((virtualFiles, error) -> {
                if (error != null || virtualFiles == null || virtualFiles.length == 0) {
                    return;
                }

                myLastSelectedDir = virtualFiles[0];
                List<ContentEntry> added = addContentEntries(virtualFiles);
                if (!added.isEmpty()) {
                    selectContentEntry(added.get(0));
                    scheduleRebuild();
                }
            }, uiAccess);
        }

        private void validateContentEntriesCandidates(List<VirtualFile> files) throws Exception {
            for (VirtualFile file : files) {
                for (ContentEntry contentEntry : myEntryToEditorMap.keySet()) {
                    VirtualFile contentEntryFile = contentEntry.getFile();
                    if (contentEntryFile == null) {
                        continue;
                    }
                    if (contentEntryFile.equals(file)) {
                        throw new Exception(ProjectLocalize.modulePathsAddContentAlreadyExistsError(file.getPresentableUrl()).get());
                    }
                    if (VirtualFileUtil.isAncestor(contentEntryFile, file, true)) {
                        throw new Exception(
                            ProjectLocalize.modulePathsAddContentIntersectError(file.getPresentableUrl(), contentEntryFile.getPresentableUrl()).get()
                        );
                    }
                    if (VirtualFileUtil.isAncestor(file, contentEntryFile, true)) {
                        throw new Exception(
                            ProjectLocalize.modulePathsAddContentDominateError(file.getPresentableUrl(), contentEntryFile.getPresentableUrl()).get()
                        );
                    }
                }

                for (Module module : myModulesProvider.getModules()) {
                    if (myModuleName.equals(module.getName())) {
                        continue;
                    }
                    ModuleRootModel rootModel = myModulesProvider.getRootModel(module);
                    LOG.assertTrue(rootModel != null);
                    for (VirtualFile moduleContentRoot : rootModel.getContentRoots()) {
                        if (file.equals(moduleContentRoot)) {
                            throw new Exception(
                                ProjectLocalize.modulePathsAddContentDuplicateError(file.getPresentableUrl(), module.getName()).get()
                            );
                        }
                    }
                }
            }
        }
    }
}
