// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.FilePosition;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author Vladislav.Soroka
 */
public class FileNavigatable implements Navigatable {
  private final Project myProject;
  private final Supplier<OpenFileDescriptor> myValue;
  private final FilePosition myFilePosition;

  public FileNavigatable(@Nonnull Project project, @Nonnull FilePosition filePosition) {
    myProject = project;
    myFilePosition = filePosition;
    myValue = LazyValue.nullable(this::createDescriptor);
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
  public OpenFileDescriptor getFileDescriptor() {
    return myValue.get();
  }

  @Nonnull
  public FilePosition getFilePosition() {
    return myFilePosition;
  }

  @Nullable
  private OpenFileDescriptor createDescriptor() {
    OpenFileDescriptor descriptor = null;
    VirtualFile file = VirtualFileUtil.findFileByIoFile(myFilePosition.getFile(), false);
    if (file != null) {
      OpenFileDescriptorFactory factory = OpenFileDescriptorFactory.getInstance(myProject);
      descriptor = factory.newBuilder(file).line(myFilePosition.getStartLine()).column(myFilePosition.getStartColumn()).build();
    }
    return descriptor;
  }
}
