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
import consulo.ide.impl.roots.ui.configuration.ContentFolderPropertiesDialogDescriptor;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.localize.ProjectLocalize;
import consulo.proxy.EventDispatcher;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ContentEntryEditor implements ContentRootPanelCallback {
    private final EventDispatcher<ContentEntryEditorListener> myEventDispatcher = EventDispatcher.create(ContentEntryEditorListener.class);
    private final ContentEntry myContentEntry;
    private final boolean myOnlySingleFile;
    private final Supplier<ModifiableRootModel> myModelSupplier;

    public ContentEntryEditor(ContentEntry contentEntry, boolean onlySingleFile, Supplier<ModifiableRootModel> modelSupplier) {
        myContentEntry = contentEntry;
        myOnlySingleFile = onlySingleFile;
        myModelSupplier = modelSupplier;
    }

    public ContentEntry getContentEntry() {
        return myContentEntry;
    }

    public ModifiableRootModel getModel() {
        return myModelSupplier.get();
    }

    public void addContentEntryEditorListener(ContentEntryEditorListener listener) {
        myEventDispatcher.addListener(listener);
    }

    public void removeContentEntryEditorListener(ContentEntryEditorListener listener) {
        myEventDispatcher.removeListener(listener);
    }

    public List<String> collectUrls() {
        List<String> urls = new ArrayList<>();
        urls.add(myContentEntry.getUrl());
        for (ContentFolder folder : myContentEntry.getFolders(LanguageContentFolderScopes.all())) {
            if (!folder.isSynthetic()) {
                urls.add(folder.getUrl());
            }
        }
        return urls;
    }

    public ContentEntrySnapshot createSnapshot(boolean packageSupport) {
        return ContentEntrySnapshot.of(myContentEntry, packageSupport);
    }

    @RequiredUIAccess
    public Component createComponent(Set<String> missingUrls) {
        ContentRootPanel panel = myOnlySingleFile ? new ContentRootPanel(this, myContentEntry) : new FolderContentRootPanel(this, myContentEntry);
        Component component = panel.createComponent(missingUrls);
        component.addClickListener(event -> myEventDispatcher.getMulticaster().editingStarted(this));
        return component;
    }

    @Override
    @RequiredUIAccess
    public void deleteContentEntry(Component source) {
        String path = FileUtil.toSystemDependentName(VirtualFileUtil.urlToPath(myContentEntry.getUrl()));
        UIAccess uiAccess = UIAccess.current();
        MessageBoxes.yesNo()
            .title(ProjectLocalize.modulePathsRemoveContentTitle())
            .text(ProjectLocalize.modulePathsRemoveContentPrompt(path))
            .showAsync(source)
            .whenCompleteAsync((answer, error) -> {
                if (Boolean.TRUE.equals(answer)) {
                    myEventDispatcher.getMulticaster().beforeEntryDeleted(this);
                    getModel().removeContentEntry(myContentEntry);
                }
            }, uiAccess);
    }

    @Override
    @RequiredUIAccess
    public void deleteContentFolder(ContentFolder contentFolder) {
        if (!contentFolder.isSynthetic()) {
            myContentEntry.removeFolder(contentFolder);
        }
        fireFoldersChanged();
    }

    @Override
    @RequiredUIAccess
    public void showChangeOptionsDialog(ContentFolder contentFolder, Component source) {
        UIAccess uiAccess = UIAccess.current();
        ContentFolderPropertiesDialogDescriptor.showAsync(source, contentFolder).whenCompleteAsync((changed, error) -> {
            if (Boolean.TRUE.equals(changed)) {
                fireFoldersChanged();
            }
        }, uiAccess);
    }

    @Override
    @RequiredUIAccess
    public void navigateFolder(ContentFolder contentFolder) {
        myEventDispatcher.getMulticaster().navigationRequested(this, contentFolder.getUrl());
    }

    @RequiredUIAccess
    public void setFolderType(List<VirtualFile> files, @Nullable ContentFolderTypeProvider folderType) {
        for (VirtualFile file : files) {
            ContentFolder folder = getFolder(file);
            if (folder != null && folder.isSynthetic()) {
                continue;
            }

            if (folderType == null) {
                if (folder != null) {
                    myContentEntry.removeFolder(folder);
                }
            }
            else if (folder == null) {
                myContentEntry.addFolder(file, folderType);
            }
            else if (!folderType.equals(folder.getType())) {
                myContentEntry.removeFolder(folder);
                myContentEntry.addFolder(file, folderType);
            }
        }
        fireFoldersChanged();
    }

    public @Nullable ContentFolder getFolder(VirtualFile file) {
        String url = file.getUrl();
        for (ContentFolder contentFolder : myContentEntry.getFolders(LanguageContentFolderScopes.all())) {
            if (url.equals(contentFolder.getUrl())) {
                return contentFolder;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void fireFoldersChanged() {
        myEventDispatcher.getMulticaster().foldersChanged(this);
    }
}
