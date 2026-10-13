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
package consulo.ide.ui;

import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.Table;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilder;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.image.Image;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PathEditor {
    private record RootPresentation(String text, Image icon, boolean valid) {
    }

    private final List<VirtualFile> myItems = new ArrayList<>();
    private final Map<VirtualFile, RootPresentation> myPresentations = new HashMap<>();
    private boolean myModified = false;
    protected boolean myEnabled = false;
    private final FileChooserDescriptor myDescriptor;
    private @Nullable VirtualFile myAddBaseDir;

    private @Nullable MutableFlatDataModel<VirtualFile> myModel;
    private @Nullable Table<VirtualFile> myTable;

    public PathEditor(FileChooserDescriptor descriptor) {
        myDescriptor = descriptor;
        myDescriptor.putUserData(FileChooserDialog.PREFER_LAST_OVER_TO_SELECT, Boolean.TRUE);
    }

    public void setAddBaseDir(@Nullable VirtualFile addBaseDir) {
        myAddBaseDir = addBaseDir;
    }

    protected void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    protected void setModified(boolean modified) {
        myModified = modified;
    }

    public boolean isModified() {
        return myModified;
    }

    public VirtualFile[] getRoots() {
        return myItems.toArray(VirtualFile.EMPTY_ARRAY);
    }

    @RequiredUIAccess
    public void resetPath(List<VirtualFile> paths) {
        myItems.clear();
        myPresentations.clear();
        setEnabled(true);
        for (VirtualFile file : paths) {
            addElement(file);
        }
        syncModel();
        setModified(false);
    }

    protected boolean isImmutable() {
        return false;
    }

    @RequiredUIAccess
    public Component createUIComponent() {
        MutableFlatDataModel<VirtualFile> model = FlatDataModel.of(myItems);
        myModel = model;

        Table<VirtualFile> table = Table.create(model);
        table.setShowHeader(false);
        table.setAllowMultipleSelect(true);
        table.addColumn(LocalizeValue.empty(), file -> file).setRender((presentation, item) -> {
            VirtualFile file = item.getValue();
            if (file != null) {
                render(presentation, file);
            }
        });
        myTable = table;

        if (isImmutable()) {
            return ScrollableLayout.create(table);
        }

        ToolbarDecoratorBuilder<VirtualFile> builder = ToolbarDecoratorBuilder.newBuilder(table)
            .addOrReplaceAction(new PathAddAction())
            .addOrReplaceAction(new PathRemoveAction())
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class);

        addToolbarActions(builder);

        return builder.build();
    }

    protected void addToolbarActions(ToolbarDecoratorBuilder<VirtualFile> builder) {
    }

    @RequiredUIAccess
    protected void doRemoveItems(List<VirtualFile> items) {
        List<VirtualFile> removed = new ArrayList<>();
        for (VirtualFile item : items) {
            if (myItems.remove(item)) {
                myPresentations.remove(item);
                removed.add(item);
            }
        }
        syncModel();
        itemsRemoved(removed);
    }

    protected void itemsRemoved(List<VirtualFile> removedItems) {
        if (!removedItems.isEmpty()) {
            setModified(true);
        }
    }

    /**
     * The machine whose files are added, reached first when it is a remote one.
     */
    @RequiredUIAccess
    protected CompletableFuture<Platform> connectPlatform() {
        return CompletableFuture.completedFuture(myDescriptor.getPlatform());
    }

    @RequiredUIAccess
    private void doAdd() {
        UIAccess uiAccess = UIAccess.current();
        connectPlatform().whenCompleteAsync((platform, error) -> {
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                MessageBoxes.okError(cause).showAsync();
                return;
            }
            myDescriptor.withPlatform(platform);
            choose(uiAccess);
        }, uiAccess);
    }

    @RequiredUIAccess
    private void choose(UIAccess uiAccess) {
        if (!myDescriptor.getPlatform().equals(Platform.current())) {
            FileChooser.choosePaths(myDescriptor, null, null).whenCompleteAsync((paths, error) -> {
                if (error != null) {
                    return;
                }
                List<VirtualFile> files = new ArrayList<>();
                for (Path path : paths) {
                    VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(path);
                    if (file != null) {
                        files.add(file);
                    }
                }
                addChosen(files.toArray(VirtualFile.EMPTY_ARRAY));
            }, uiAccess);
            return;
        }
        FileChooser.chooseFiles(myDescriptor, null, myAddBaseDir).whenCompleteAsync((files, error) -> {
            if (error != null) {
                return;
            }
            addChosen(files);
        }, uiAccess);
    }

    @RequiredUIAccess
    private void addChosen(VirtualFile[] files) {
        List<VirtualFile> added = new ArrayList<>();
        for (VirtualFile file : adjustAddedFileSet(files)) {
            if (addElement(file)) {
                added.add(file);
            }
        }

        if (!added.isEmpty()) {
            syncModel();
            setModified(true);
            setSelectedRoots(added);
        }
    }

    protected VirtualFile[] adjustAddedFileSet(VirtualFile[] files) {
        return files;
    }

    protected boolean isUrlInserted() {
        return !myItems.isEmpty() && myItems.get(myItems.size() - 1) instanceof HttpVirtualFile;
    }

    @RequiredUIAccess
    public void addPaths(VirtualFile... paths) {
        boolean added = false;
        for (VirtualFile path : paths) {
            if (addElement(path)) {
                added = true;
            }
        }
        if (added) {
            syncModel();
            setModified(true);
        }
    }

    @RequiredUIAccess
    public void removePaths(VirtualFile... paths) {
        Set<VirtualFile> pathsSet = new HashSet<>(Arrays.asList(paths));
        List<VirtualFile> toRemove = new ArrayList<>();
        for (VirtualFile item : myItems) {
            if (pathsSet.contains(item)) {
                toRemove.add(item);
            }
        }
        doRemoveItems(toRemove);
    }

    protected boolean addElement(@Nullable VirtualFile item) {
        if (item == null || myPresentations.containsKey(item)) {
            return false;
        }

        if (isUrlInserted()) {
            myItems.add(myItems.size() - 1, item);
        }
        else {
            myItems.add(item);
        }
        myPresentations.put(item, createPresentation(item));
        return true;
    }

    @RequiredUIAccess
    protected void setSelectedRoots(List<VirtualFile> roots) {
        Table<VirtualFile> table = myTable;
        if (table == null || roots.isEmpty()) {
            return;
        }

        VirtualFile last = roots.get(roots.size() - 1);
        if (myPresentations.containsKey(last)) {
            table.select(last);
            table.scrollTo(last);
        }
    }

    @RequiredUIAccess
    protected List<VirtualFile> getSelectedRoots() {
        Table<VirtualFile> table = myTable;
        return table == null ? List.of() : table.getSelectedItems();
    }

    protected int getRowCount() {
        return myItems.size();
    }

    protected VirtualFile getValueAt(int row) {
        return myItems.get(row);
    }

    @RequiredUIAccess
    public void clearList() {
        myItems.clear();
        myPresentations.clear();
        syncModel();
        setModified(true);
    }

    @RequiredUIAccess
    private void syncModel() {
        MutableFlatDataModel<VirtualFile> model = myModel;
        if (model != null) {
            model.replaceAll(new ArrayList<>(myItems));
        }
    }

    private void render(TextItemPresentation presentation, VirtualFile file) {
        RootPresentation rootPresentation = myPresentations.get(file);
        if (rootPresentation == null) {
            return;
        }

        presentation.withIcon(rootPresentation.icon());
        presentation.append(rootPresentation.text(), rootPresentation.valid() ? TextAttribute.REGULAR : TextAttribute.ERROR);
    }

    private static RootPresentation createPresentation(VirtualFile file) {
        boolean valid = file.isValid();
        return new RootPresentation(file.getPresentableUrl(), getIconForRoot(file, valid), valid);
    }

    private static Image getIconForRoot(VirtualFile file, boolean valid) {
        if (!valid) {
            return PlatformIconGroup.nodesPpinvalid();
        }

        if (file instanceof HttpVirtualFile) {
            return PlatformIconGroup.nodesPpweb();
        }

        FileType fileType = findFileType(file);
        if (fileType instanceof ArchiveFileType) {
            return fileType.getIcon();
        }

        if (file.isDirectory()) {
            return PlatformIconGroup.nodesFolder();
        }

        return fileType != null ? fileType.getIcon() : FileTypeRegistry.getInstance().getFileTypeByFileName(file.getName()).getIcon();
    }

    private static @Nullable FileType findFileType(VirtualFile file) {
        String name = file.getName();
        if (file.getFileSystem() instanceof ArchiveFileSystem && file.getParent() == null) {
            String path = file.getPath();
            String localPath = path.endsWith(URLUtil.ARCHIVE_SEPARATOR)
                ? path.substring(0, path.length() - URLUtil.ARCHIVE_SEPARATOR.length())
                : path;
            name = localPath.substring(localPath.lastIndexOf('/') + 1);
        }
        else if (file.isDirectory()) {
            return null;
        }
        return FileTypeRegistry.getInstance().getFileTypeByFileName(name);
    }

    private class PathAddAction extends AddAction<VirtualFile> {
        @Override
        @RequiredUIAccess
        protected void doAdd(AnActionEvent e) {
            if (myEnabled) {
                PathEditor.this.doAdd();
            }
        }
    }

    private class PathRemoveAction extends RemoveAction<VirtualFile> {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            if (myEnabled) {
                doRemoveItems(getSelectedRoots());
            }
        }
    }
}
