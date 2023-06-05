// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.attach;

import consulo.execution.debug.attach.osHandler.AttachOSHandler;
import consulo.process.BaseProcessHandler;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.CapturingProcessRunner;
import consulo.process.util.ProcessOutput;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * This abstract class represent {@link XAttachHost} with extended functional, such as executing {@link GeneralCommandLine},
 * downloading files and getting OS of a host
 */
public abstract class EnvironmentAwareHost implements XAttachHost {

  private AttachOSHandler myOsHandler = null;

  /**
   * @param commandLine commandLine to execute on this host
   * @return {@link BaseProcessHandler}, with which the command is executed (for example with a timeout)
   */
  @Nonnull
  public abstract BaseProcessHandler getProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException;

  /**
   * @param commandLine commandLine to execute on this host
   * @return output of the corresponding process
   */
  @Nonnull
  public ProcessOutput getProcessOutput(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
    BaseProcessHandler handler = getProcessHandler(commandLine);
    CapturingProcessRunner runner = new CapturingProcessRunner(handler);
    return runner.runProcess();
  }

  @Nonnull
  public AttachOSHandler getOsHandler() {
    if (myOsHandler == null) {
      myOsHandler = AttachOSHandler.getAttachOsHandler(this);
    }

    return myOsHandler;
  }

  /**
   * Retrieves file contents stream. May be used to sync parts of the debugged project.
   *
   * @param filePath path of the file on host machine
   * @return stream with file contents or <code>null</code> if the specified file does not exist
   * @throws IOException on stream retrieval error
   */
  @Nullable
  public abstract InputStream getFileContent(@Nonnull String filePath) throws IOException;

  /**
   * Check if it is possible to read the file on host machine
   *
   * @param filePath path of the file on host machine
   * @throws ExecutionException on stream retrieval error
   */
  public abstract boolean canReadFile(@Nonnull String filePath) throws ExecutionException;

  /**
   * File system prefix for files from this host. It should be noted that the prefixes must be different for different hosts.
   * Path to the host file is obtained by concatenation of hostId and it's on-host path
   */
  @Nonnull
  public abstract String getFileSystemHostId();

  /**
   * @param credentialsObject is a parametrization of a host
   * @return whether the given credentials corresponds the same host
   */
  public boolean isSameHost(@Nonnull final Object credentialsObject) {
    return false;
  }
}
