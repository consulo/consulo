/*
 * Copyright 2013-2025 consulo.io
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

import consulo.content.ContentFolderTypeProvider;
import consulo.ide.impl.roots.ui.configuration.ContentFolderPropertiesDialog;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.localize.ProjectLocalize;
import consulo.proxy.EventDispatcher;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.Objects;
import java.util.function.Supplier;

public class ContentEntryEditor implements ContentRootPanel.ActionCallback {
    private boolean myIsSelected;
    private ContentRootPanel myContentRootPanel;
    private JPanel myMainPanel;
    protected EventDispatcher<ContentEntryEditorListener> myEventDispatcher;
    private final ContentEntry myContentEntry;
    private final boolean myOnlySingleFile;
    private final Supplier<ModifiableRootModel> myModelSupplier;

    public interface ContentEntryEditorListener extends EventListener {
        default void editingStarted(@Nonnull ContentEntryEditor editor) {
        }

        default void beforeEntryDeleted(@Nonnull ContentEntryEditor editor) {
        }

        default void folderAdded(@Nonnull ContentEntryEditor editor, ContentFolder contentFolder) {
        }

        default void folderRemoved(@Nonnull ContentEntryEditor editor, ContentFolder contentFolder) {
        }

        default void navigationRequested(@Nonnull ContentEntryEditor editor, VirtualFile file) {
        }
    }

    public ContentEntryEditor(ContentEntry contentEntry,
                              boolean onlySingleFile,
                              Supplier<ModifiableRootModel> modelSupplier) {
        myContentEntry = contentEntry;
        myOnlySingleFile = onlySingleFile;
        myModelSupplier = modelSupplier;
    }

    public void initUI() {
        myMainPanel = new JPanel(new BorderLayout());
        myMainPanel.setOpaque(false);
        myMainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                myEventDispatcher.getMulticaster().editingStarted(ContentEntryEditor.this);
            }
        });
        myEventDispatcher = EventDispatcher.create(ContentEntryEditorListener.class);
        setSelected(false);
        update();
    }

    @Nonnull
    protected ContentEntry getContentEntry() {
        return myContentEntry;
    }

    @Nonnull
    protected ModifiableRootModel getModel() {
        return myModelSupplier.get();
    }

    @Override
    @RequiredUIAccess
    public void deleteContentEntry() {
        String path = FileUtil.toSystemDependentName(VirtualFileUtil.urlToPath(myContentEntry.getUrl()));
        int answer = Messages.showYesNoDialog(
            ProjectLocalize.modulePathsRemoveContentPrompt(path).get(),
            ProjectLocalize.modulePathsRemoveContentTitle().get(),
            UIUtil.getQuestionIcon()
        );
        if (answer != 0) { // no
            return;
        }
        myEventDispatcher.getMulticaster().beforeEntryDeleted(this);
        ContentEntry entry = getContentEntry();
        getModel().removeContentEntry(entry);
    }

    @Override
    public void deleteContentFolder(ContentEntry contentEntry, ContentFolder folder) {
        removeFolder(folder);
        update();
    }

    @Override
    public void showChangeOptionsDialog(ContentEntry contentEntry, ContentFolder contentFolder) {
        ContentFolderPropertiesDialog c = new ContentFolderPropertiesDialog(getModel().getProject(), contentFolder);
        AsyncResult<Boolean> booleanAsyncResult = c.showAndGetOk();
        if (Objects.equals(booleanAsyncResult.getResult(), Boolean.TRUE)) {
            update();
        }
    }

    @Override
    public void navigateFolder(ContentEntry contentEntry, ContentFolder contentFolder) {
        VirtualFile file = contentFolder.getFile();
        if (file != null) { // file can be deleted externally
            myEventDispatcher.getMulticaster().navigationRequested(this, file);
        }
    }

    public void addContentEntryEditorListener(ContentEntryEditorListener listener) {
        myEventDispatcher.addListener(listener);
    }

    public void removeContentEntryEditorListener(ContentEntryEditorListener listener) {
        myEventDispatcher.removeListener(listener);
    }

    public void setSelected(boolean isSelected) {
        if (myIsSelected != isSelected) {
            myIsSelected = isSelected;
        }
    }

    public JComponent getComponent() {
        return myMainPanel;
    }

    public void update() {
        if (myContentRootPanel != null) {
            myMainPanel.remove(myContentRootPanel);
        }
        myContentRootPanel = createContentRootPane();
        myContentRootPanel.initUI();
        myMainPanel.add(myContentRootPanel, BorderLayout.CENTER);
        myMainPanel.revalidate();
    }

    protected ContentRootPanel createContentRootPane() {
        if (myOnlySingleFile) {
            return new ContentRootPanel(this, getContentEntry());
        }
        return new FolderContentRootPanel(this, getContentEntry());
    }

    @Nullable
    public ContentFolder addFolder(@Nonnull VirtualFile file, ContentFolderTypeProvider contentFolderType) {
        ContentEntry contentEntry = getContentEntry();
        ContentFolder contentFolder = contentEntry.addFolder(file, contentFolderType);
        try {
            return contentFolder;
        }
        finally {
            myEventDispatcher.getMulticaster().folderAdded(this, contentFolder);
            update();
        }
    }

    public void removeFolder(@Nonnull ContentFolder contentFolder) {
        try {
            if (contentFolder.isSynthetic()) {
                return;
            }
            ContentEntry contentEntry = getContentEntry();
            contentEntry.removeFolder(contentFolder);
        }
        finally {
            myEventDispatcher.getMulticaster().folderRemoved(this, contentFolder);
            update();
        }
    }

    @Nullable
    public ContentFolder getFolder(@Nonnull VirtualFile file) {
        ContentEntry contentEntry = getContentEntry();
        for (ContentFolder contentFolder : contentEntry.getFolders(LanguageContentFolderScopes.all())) {
            VirtualFile f = contentFolder.getFile();
            if (f != null && f.equals(file)) {
                return contentFolder;
            }
        }
        return null;
    }
}
