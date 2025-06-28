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

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileChooserKeys;
import consulo.ide.impl.idea.openapi.roots.ui.componentsList.components.ScrollablePanel;
import consulo.ide.impl.idea.openapi.roots.ui.componentsList.layout.VerticalStackLayout;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.ui.roots.ToolbarPanel;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.language.editor.LangDataKeys;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.ModulesProvider;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VirtualFileManagerListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 * @since 2003-10-04
 */
public class ContentEntriesEditor extends ModuleElementsEditor {
    private static final Logger LOG = Logger.getInstance(ContentEntriesEditor.class);

    protected ContentEntryTreeEditor myRootTreeEditor;
    private MyContentEntryEditorListener myContentEntryEditorListener;
    protected JPanel myEditorsPanel;
    private final Map<ContentEntry, ContentEntryEditor> myEntryToEditorMap = new HashMap<>();
    private ContentEntry mySelectedEntry;

    private VirtualFile myLastSelectedDir = null;
    private final String myModuleName;
    private final ModulesProvider myModulesProvider;
    private final ModuleConfigurationState myState;
    private ActionToolbar myActionToolbar;

    public ContentEntriesEditor(String moduleName, final ModuleConfigurationState state) {
        super(state);
        myState = state;
        myModuleName = moduleName;
        myModulesProvider = state.getModulesConfigurator();
        VirtualFileManagerListener fileManagerListener = new VirtualFileManagerListener() {
            @Override
            public void afterRefreshFinish(boolean asynchronous) {
                if (state.getProject().isDisposed()) {
                    return;
                }
                Module module = getModule();
                if (module == null || module.isDisposed()) {
                    return;
                }
                for (ContentEntryEditor editor : myEntryToEditorMap.values()) {
                    editor.update();
                }
            }
        };
        VirtualFileManager fileManager = VirtualFileManager.getInstance();
        fileManager.addVirtualFileManagerListener(fileManagerListener);
        registerDisposable(() -> fileManager.removeVirtualFileManagerListener(fileManagerListener));
    }

    @Override
    protected ModifiableRootModel getModel() {
        return myState.getRootModel();
    }

    @Override
    public String getDisplayName() {
        return ProjectLocalize.modulePathsTitle().get();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        if (myRootTreeEditor != null) {
            myRootTreeEditor.setContentEntryEditor(null);
        }

        myEntryToEditorMap.clear();
        super.disposeUIResources();
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public JPanel createComponentImpl(@Nonnull Disposable parentUIDisposable) {
        Module module = getModule();
        Project project = module.getProject();

        myContentEntryEditorListener = new MyContentEntryEditorListener();

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel entriesPanel = new JPanel(new BorderLayout());

        AddContentEntryAction action = new AddContentEntryAction();
        action.registerCustomShortcutSet(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK, mainPanel);

        myEditorsPanel = new ScrollablePanel(new VerticalStackLayout());
        myEditorsPanel.setBackground(UIUtil.getListBackground());

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myEditorsPanel, true);
        entriesPanel.add(new ToolbarPanel(scrollPane, ActionGroup.newImmutableBuilder().add(action).build()), BorderLayout.CENTER);

        JBSplitter splitter = new OnePixelSplitter(false);
        splitter.setProportion(0.6f);
        splitter.setHonorComponentsMinimumSize(true);

        myRootTreeEditor = new ContentEntryTreeEditor(project, myState);
        JComponent component = myRootTreeEditor.createComponent();
        component.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));

        splitter.setFirstComponent(component);
        splitter.setSecondComponent(entriesPanel);
        JPanel contentPanel = new JPanel(new BorderLayout());

        myActionToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, myRootTreeEditor.getEditingActionsGroup(), true);
        myActionToolbar.setTargetComponent(contentPanel);
        myActionToolbar.updateActionsAsync();

        contentPanel.add(myActionToolbar.getComponent(), BorderLayout.NORTH);
        contentPanel.add(splitter, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        ModifiableRootModel model = getModel();
        if (model != null) {
            boolean onlySingleFile = model.getModule().getModuleDirPath() == null;
            ContentEntry[] contentEntries = model.getContentEntries();
            if (contentEntries.length > 0) {
                for (ContentEntry contentEntry : contentEntries) {
                    addContentEntryPanel(contentEntry, onlySingleFile);
                }
                selectContentEntry(contentEntries[0]);
            }
        }

        return mainPanel;
    }

    protected Module getModule() {
        return myModulesProvider.getModule(myModuleName);
    }

    public ActionToolbar getActionToolbar() {
        return myActionToolbar;
    }

    protected void addContentEntryPanel(ContentEntry contentEntry, boolean onlySingleFile) {
        ContentEntryEditor contentEntryEditor = createContentEntryEditor(contentEntry, onlySingleFile);
        contentEntryEditor.initUI();
        contentEntryEditor.addContentEntryEditorListener(myContentEntryEditorListener);
        registerDisposable(() -> contentEntryEditor.removeContentEntryEditorListener(myContentEntryEditorListener));
        myEntryToEditorMap.put(contentEntry, contentEntryEditor);
        JComponent component = contentEntryEditor.getComponent();

        component.setBorder(JBUI.Borders.empty());
        myEditorsPanel.add(component);
    }

    private ContentEntryEditor createContentEntryEditor(ContentEntry contentEntry, boolean onlySingleFile) {
        return new ContentEntryEditor(contentEntry, onlySingleFile, this::getModel);
    }

    void selectContentEntry(@Nullable ContentEntry contentEntryUrl) {
        if (mySelectedEntry != null && mySelectedEntry.equals(contentEntryUrl)) {
            return;
        }
        try {
            if (mySelectedEntry != null) {
                ContentEntryEditor editor = myEntryToEditorMap.get(mySelectedEntry);
                if (editor != null) {
                    editor.setSelected(false);
                }
            }

            if (contentEntryUrl != null) {
                ContentEntryEditor editor = myEntryToEditorMap.get(contentEntryUrl);
                if (editor != null) {
                    editor.setSelected(true);
                    JComponent component = editor.getComponent();
                    JComponent scroller = (JComponent) component.getParent();
                    SwingUtilities.invokeLater(() -> scroller.scrollRectToVisible(component.getBounds()));
                    myRootTreeEditor.setContentEntryEditor(editor);
                    myRootTreeEditor.requestFocus();
                }
            }
        }
        finally {
            mySelectedEntry = contentEntryUrl;
        }
    }

    @Override
    public void moduleStateChanged() {
        myEntryToEditorMap.clear();
        myEditorsPanel.removeAll();

        ModifiableRootModel model = getModel();
        if (model != null) {
            ContentEntry[] contentEntries = model.getContentEntries();
            if (contentEntries.length > 0) {
                boolean onlySingleFile = model.getModule().getModuleDirPath() == null;
                for (ContentEntry contentEntry : contentEntries) {
                    addContentEntryPanel(contentEntry, onlySingleFile);
                }
                selectContentEntry(contentEntries[0]);
            }
            else {
                selectContentEntry(null);
                myRootTreeEditor.setContentEntryEditor(null);
            }
        }

        if (myRootTreeEditor != null) {
            myRootTreeEditor.update();
        }
    }

    @Nullable
    private ContentEntry getNextContentEntry(ContentEntry contentEntryUrl) {
        return getAdjacentContentEntry(contentEntryUrl, 1);
    }

    @Nullable
    private ContentEntry getAdjacentContentEntry(ContentEntry contentEntryUrl, int delta) {
        ContentEntry[] contentEntries = getModel().getContentEntries();
        for (int idx = 0; idx < contentEntries.length; idx++) {
            ContentEntry entry = contentEntries[idx];
            if (contentEntryUrl.equals(entry)) {
                int nextEntryIndex = (idx + delta) % contentEntries.length;
                if (nextEntryIndex < 0) {
                    nextEntryIndex += contentEntries.length;
                }
                return nextEntryIndex == idx ? null : contentEntries[nextEntryIndex];
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
            ContentEntry contentEntry = getModel().addContentEntry(file);
            contentEntries.add(contentEntry);
        }
        return contentEntries;
    }

    private boolean isAlreadyAdded(VirtualFile file) {
        VirtualFile[] contentRoots = getModel().getContentRoots();
        for (VirtualFile contentRoot : contentRoots) {
            if (contentRoot.equals(file)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveData() {
    }

    private final class MyContentEntryEditorListener implements ContentEntryEditor.ContentEntryEditorListener {
        @Override
        public void editingStarted(@Nonnull ContentEntryEditor editor) {
            selectContentEntry(editor.getContentEntry());
        }

        @Override
        public void beforeEntryDeleted(@Nonnull ContentEntryEditor editor) {
            ContentEntry entryUrl = editor.getContentEntry();
            if (mySelectedEntry != null && mySelectedEntry.equals(entryUrl)) {
                myRootTreeEditor.setContentEntryEditor(null);
            }
            ContentEntry nextContentEntryUrl = getNextContentEntry(entryUrl);
            removeContentEntryPanel(entryUrl);
            selectContentEntry(nextContentEntryUrl);
            editor.removeContentEntryEditorListener(this);
        }

        @Override
        public void navigationRequested(@Nonnull ContentEntryEditor editor, VirtualFile file) {
            if (mySelectedEntry != null && mySelectedEntry.equals(editor.getContentEntry())) {
                myRootTreeEditor.requestFocus();
                myRootTreeEditor.select(file);
            }
            else {
                selectContentEntry(editor.getContentEntry());
                myRootTreeEditor.requestFocus();
                myRootTreeEditor.select(file);
            }
        }

        private void removeContentEntryPanel(ContentEntry contentEntry) {
            ContentEntryEditor editor = myEntryToEditorMap.get(contentEntry);
            if (editor != null) {
                myEditorsPanel.remove(editor.getComponent());
                myEntryToEditorMap.remove(contentEntry);
                myEditorsPanel.revalidate();
                myEditorsPanel.repaint();
            }
        }
    }

    private class AddContentEntryAction extends DumbAwareAction {
        private final FileChooserDescriptor myDescriptor;

        public AddContentEntryAction() {
            super(ProjectLocalize.modulePathsAddContentAction(), ProjectLocalize.modulePathsAddContentActionDescription(), PlatformIconGroup.generalAdd());
            myDescriptor = new FileChooserDescriptor(false, true, true, false, true, true) {
                @Override
                public void validateSelectedFiles(VirtualFile[] files) throws Exception {
                    validateContentEntriesCandidates(files);
                }
            };
            myDescriptor.putUserData(LangDataKeys.MODULE_CONTEXT, getModule());
            myDescriptor.withTitleValue(ProjectLocalize.modulePathsAddContentTitle());
            myDescriptor.withDescriptionValue(ProjectLocalize.modulePathsAddContentPrompt());
            myDescriptor.putUserData(FileChooserKeys.DELETE_ACTION_AVAILABLE, false);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            FileChooser.chooseFiles(myDescriptor, myProject, myLastSelectedDir).doWhenDone(virtualFiles -> {
                myLastSelectedDir = virtualFiles[0];
                addContentEntries(virtualFiles);
            });
        }

        private void validateContentEntriesCandidates(VirtualFile[] files) throws Exception {
            for (VirtualFile file : files) {
                // check for collisions with already existing entries
                for (ContentEntry contentEntry : myEntryToEditorMap.keySet()) {
                    VirtualFile contentEntryFile = contentEntry.getFile();
                    if (contentEntryFile == null) {
                        continue;  // skip invalid entry
                    }
                    if (contentEntryFile.equals(file)) {
                        throw new Exception(ProjectLocalize.modulePathsAddContentAlreadyExistsError(file.getPresentableUrl()).get());
                    }
                    if (VfsUtilCore.isAncestor(contentEntryFile, file, true)) {
                        // intersection not allowed
                        throw new Exception(
                            ProjectLocalize.modulePathsAddContentIntersectError(file.getPresentableUrl(), contentEntryFile.getPresentableUrl()).get());
                    }
                    if (VfsUtilCore.isAncestor(file, contentEntryFile, true)) {
                        // intersection not allowed
                        throw new Exception(
                            ProjectLocalize.modulePathsAddContentDominateError(file.getPresentableUrl(), contentEntryFile.getPresentableUrl()).get());
                    }
                }
                // check if the same root is configured for another module
                Module[] modules = myModulesProvider.getModules();
                for (Module module : modules) {
                    if (myModuleName.equals(module.getName())) {
                        continue;
                    }
                    ModuleRootModel rootModel = myModulesProvider.getRootModel(module);
                    LOG.assertTrue(rootModel != null);
                    VirtualFile[] moduleContentRoots = rootModel.getContentRoots();
                    for (VirtualFile moduleContentRoot : moduleContentRoots) {
                        if (file.equals(moduleContentRoot)) {
                            throw new Exception(
                                ProjectLocalize.modulePathsAddContentDuplicateError(file.getPresentableUrl(), module.getName()).get());
                        }
                    }
                }
            }
        }

    }

}
