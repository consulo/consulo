// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.build.ui.FilePosition;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.NullableLazyValue;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.navigation.Navigatable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class FileNavigatable implements Navigatable {
  private final Project myProject;
  private final NullableLazyValue<OpenFileDescriptorImpl> myValue;
  private final FilePosition myFilePosition;

  public FileNavigatable(@Nonnull Project project, @Nonnull FilePosition filePosition) {
    myProject = project;
    myFilePosition = filePosition;
    myValue = new NullableLazyValue<OpenFileDescriptorImpl>() {
      @Nullable
      @Override
      protected OpenFileDescriptorImpl compute() {
        return createDescriptor();
      }
    };
  }

  @Override
  public void navigate(boolean requestFocus) {
    Navigatable descriptor = getFileDescriptor();
    if (descriptor != null) {
      descriptor.navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    Navigatable descriptor = getFileDescriptor();
    if (descriptor != null) {
      return descriptor.canNavigate();
    }
    return false;
  }

  @Override
  public boolean canNavigateToSource() {
    Navigatable descriptor = getFileDescriptor();
    if (descriptor != null) {
      return descriptor.canNavigateToSource();
    }
    return false;
  }

  @Nullable
  public OpenFileDescriptorImpl getFileDescriptor() {
    return myValue.getValue();
  }

  @Nonnull
  public FilePosition getFilePosition() {
    return myFilePosition;
  }

  @Nullable
  private OpenFileDescriptorImpl createDescriptor() {
    OpenFileDescriptorImpl descriptor = null;
    VirtualFile file = VfsUtil.findFileByIoFile(myFilePosition.getFile(), false);
    if (file != null) {
      descriptor = new OpenFileDescriptorImpl(myProject, file, myFilePosition.getStartLine(), myFilePosition.getStartColumn());
    }
    return descriptor;
  }
}
