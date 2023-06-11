// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.attach;

import consulo.platform.Platform;
import consulo.platform.ProcessInfo;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.cmd.GeneralCommandLine;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class LocalAttachHost extends EnvironmentAwareHost {
  public static final LocalAttachHost INSTANCE = new LocalAttachHost();

  @Nonnull
  @Override
  public Collection<ProcessInfo> getProcessList() {
    return Platform.current().os().processes();
  }

  @Nonnull
  @Override
  public ProcessHandler getProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    return ProcessHandlerBuilder.create(commandLine).build();
  }

  @Nullable
  @Override
  public InputStream getFileContent(@Nonnull String filePath) throws IOException {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
    if (file == null) {
      return null;
    }

    return file.getInputStream();
  }

  @Override
  public boolean canReadFile(@Nonnull String filePath) {
    return new File(filePath).canRead();
  }

  @Nonnull
  @Override
  public String getFileSystemHostId() {
    return "";
  }
}
