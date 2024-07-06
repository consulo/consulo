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

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.ThrowableComputable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.image.Image;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

  public VirtualFileDiffElement(@Nonnull VirtualFile file) {
    myFile = file;
  }

  @Override
  public String getPath() {
    return myFile.getPresentableUrl();
  }

  @Nonnull
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
  @Nullable
  public Navigatable getNavigatable(@Nullable Project project) {
    if (project == null || project.isDefault() || !myFile.isValid()) return null;
    return OpenFileDescriptorFactory.getInstance(project).newBuilder(myFile).build();
  }

  @Override
  public VirtualFileDiffElement[] getChildren() {
    if (myFile.is(VFileProperty.SYMLINK)) {
      return new VirtualFileDiffElement[0];
    }
    final VirtualFile[] files = myFile.getChildren();
    final ArrayList<VirtualFileDiffElement> elements = new ArrayList<>();
    for (VirtualFile file : files) {
      if (!FileTypeRegistry.getInstance().isFileIgnored(file) && file.isValid()) {
        elements.add(new VirtualFileDiffElement(file));
      }
    }
    return elements.toArray(new VirtualFileDiffElement[elements.size()]);
  }

  @Nullable
  @Override
  public byte[] getContent() throws IOException {
    return ApplicationManager.getApplication().runReadAction(new ThrowableComputable<byte[], IOException>() {
      @Override
      public byte[] compute() throws IOException {
        return myFile.contentsToByteArray();
      }
    });
  }

  @Override
  public VirtualFile getValue() {
    return myFile;
  }

  @Override
  public Image getIcon() {
    return isContainer() ? AllIcons.Nodes.Folder : VirtualFilePresentation.getIcon(myFile);
  }

  @Override
  public Callable<DiffElement<VirtualFile>> getElementChooser(final Project project) {
    return () -> {
      final FileChooserDescriptor descriptor = getChooserDescriptor();
      final VirtualFile[] result = IdeaFileChooser.chooseFiles(descriptor, project, getValue());
      return result.length == 1 ? createElement(result[0]) : null;
    };
  }

  @Nullable
  protected VirtualFileDiffElement createElement(VirtualFile file) {
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
      final File src = new File(myFile.getPath());
      final File trg = new File(container.getValue().getPath() + relativePath + src.getName());
      FileUtil.copy(src, trg, FilePermissionCopier.BY_NIO2);
      final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(trg);
      if (virtualFile != null) {
        return new VirtualFileDiffElement(virtualFile);
      }
    }
    catch (IOException e) {//
    }
    return null;
  }

  @Override
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
  public void refresh(boolean userInitiated) {
    refreshFile(userInitiated, myFile);
  }

  public static void refreshFile(boolean userInitiated, VirtualFile virtualFile) {
    if (userInitiated) {
      final List<Document> docsToSave = new ArrayList<>();
      final FileDocumentManager manager = FileDocumentManager.getInstance();
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
