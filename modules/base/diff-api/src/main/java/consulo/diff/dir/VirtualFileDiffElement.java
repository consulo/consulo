/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.dir;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.navigation.Navigable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Konstantin Bulenkov
 */
public class VirtualFileDiffElement extends DiffElement<VirtualFile> {
    private final VirtualFile myFile;

    public VirtualFileDiffElement(VirtualFile file) {
        myFile = file;
    }

    @Override
    public String getPath() {
        return myFile.getPresentableUrl();
    }

    @Override
    public String getName() {
        return myFile.getName();
    }

    @Override
    public String getPresentablePath() {
        return getPath();
    }

    @Override
    public long getSize() {
        return myFile.getLength();
    }

    @Override
    public long getTimeStamp() {
        return myFile.getTimeStamp();
    }

    @Override
    public boolean isContainer() {
        return myFile.isDirectory();
    }

    @Override
    public @Nullable Navigable getNavigable(@Nullable Project project) {
        if (project == null || project.isDefault() || !myFile.isValid()) {
            return null;
        }
        return OpenFileDescriptorFactory.getInstance(project).newBuilder(myFile).build();
    }

    @Override
    public VirtualFileDiffElement[] getChildren() {
        if (myFile.is(VFileProperty.SYMLINK)) {
            return new VirtualFileDiffElement[0];
        }
        List<VirtualFileDiffElement> elements = new ArrayList<>();
        for (VirtualFile file : myFile.getRequiredChildren()) {
            if (!FileTypeRegistry.getInstance().isFileIgnored(file) && file.isValid()) {
                elements.add(new VirtualFileDiffElement(file));
            }
        }
        return elements.toArray(new VirtualFileDiffElement[elements.size()]);
    }

    @Override
    public byte @Nullable [] getContent() throws IOException {
        return Application.get().runReadAction((ThrowableSupplier<byte[], IOException>) () -> myFile.contentsToByteArray());
    }

    @Override
    public VirtualFile getValue() {
        return myFile;
    }

    @Override
    public Image getIcon() {
        return isContainer() ? PlatformIconGroup.nodesFolder() : VirtualFilePresentation.getIcon(myFile);
    }

    @Override
    public Callable<DiffElement<VirtualFile>> getElementChooser(Project project) {
        return () -> {
            FileChooserDescriptor descriptor = getChooserDescriptor();
            VirtualFile[] result = IdeaFileChooser.chooseFiles(descriptor, project, getValue());
            return result.length == 1 ? createElement(result[0]) : null;
        };
    }

    protected @Nullable VirtualFileDiffElement createElement(VirtualFile file) {
        return new VirtualFileDiffElement(file);
    }

    protected FileChooserDescriptor getChooserDescriptor() {
        return new FileChooserDescriptor(false, true, false, false, false, false);
    }

    @Override
    public boolean isOperationsEnabled() {
        return myFile.getFileSystem() instanceof LocalFileSystem;
    }

    @Override
    public VirtualFileDiffElement copyTo(DiffElement<VirtualFile> container, String relativePath) {
        try {
            File src = new File(myFile.getPath());
            File trg = new File(container.getValue().getPath() + relativePath + src.getName());
            FileUtil.copy(src, trg, FilePermissionCopier.BY_NIO2);
            VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(trg);
            if (virtualFile != null) {
                return new VirtualFileDiffElement(virtualFile);
            }
        }
        catch (IOException e) {//
        }
        return null;
    }

    @Override
    @RequiredWriteAction
    public boolean delete() {
        try {
            myFile.delete(this);
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    @RequiredUIAccess
    public void refresh(boolean userInitiated) {
        refreshFile(userInitiated, myFile);
    }

    @RequiredUIAccess
    public static void refreshFile(boolean userInitiated, VirtualFile virtualFile) {
        if (userInitiated) {
            List<Document> docsToSave = new ArrayList<>();
            FileDocumentManager manager = FileDocumentManager.getInstance();
            for (Document document : manager.getUnsavedDocuments()) {
                VirtualFile file = manager.getFile(document);
                if (file != null && VirtualFileUtil.isAncestor(virtualFile, file, false)) {
                    docsToSave.add(document);
                }
            }

            if (!docsToSave.isEmpty()) {
                WriteAction.run(() -> {
                    for (Document document : docsToSave) {
                        manager.saveDocument(document);
                    }
                });
            }

            ModalityState modalityState = ProgressManager.getInstance().getProgressIndicator().getModalityState();

            VirtualFileUtil.markDirty(true, true, virtualFile);
            RefreshQueue.getInstance().refresh(false, true, null, modalityState, virtualFile);
        }
    }
}
