// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.FilePosition;
import consulo.navigation.NavigateOptions;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author Vladislav.Soroka
 */
public class FileNavigatable implements Navigatable {
  private final Project myProject;
  private final Supplier<OpenFileDescriptor> myValue;
  private final FilePosition myFilePosition;

  public FileNavigatable(Project project, FilePosition filePosition) {
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
  public NavigateOptions getNavigateOptions() {
    Navigatable descriptor = getFileDescriptor();
    return descriptor != null ? descriptor.getNavigateOptions() : NavigateOptions.CANT_NAVIGATE;
  }

  public @Nullable OpenFileDescriptor getFileDescriptor() {
    return myValue.get();
  }

  
  public FilePosition getFilePosition() {
    return myFilePosition;
  }

  private @Nullable OpenFileDescriptor createDescriptor() {
    OpenFileDescriptor descriptor = null;
    VirtualFile file = VirtualFileUtil.findFileByIoFile(myFilePosition.getFile(), false);
    if (file != null) {
      OpenFileDescriptorFactory factory = OpenFileDescriptorFactory.getInstance(myProject);
      descriptor = factory.newBuilder(file).line(myFilePosition.getStartLine()).column(myFilePosition.getStartColumn()).build();
    }
    return descriptor;
  }
}
