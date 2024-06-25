/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.process.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.execution.process.RunnerWinProcess;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.internal.OSProcessManager;
import consulo.process.internal.UnixProcessManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import org.jvnet.winp.WinProcess;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class OSProcessManagerImpl extends OSProcessManager {
  private static final Logger LOG = Logger.getInstance(OSProcessManagerImpl.class);

  @Override
  public boolean killProcessTree(@Nonnull Process process) {
    if (Platform.current().os().isWindows()) {
      try {
        WinProcess winProcess = createWinProcess(process);
        winProcess.killRecursively();
        return true;
      }
      catch (Throwable e) {
        LOG.info("Cannot kill process tree", e);
      }
    }
    else if (Platform.current().os().isUnix()) {
      return UnixProcessManager.sendSigKillToProcessTree(process);
    }
    return false;
  }

  @Nonnull
  private static WinProcess createWinProcess(@Nonnull Process process) {
    if (process instanceof RunnerWinProcess) {
      RunnerWinProcess runnerWinProcess = (RunnerWinProcess)process;
      return new WinProcess((int)runnerWinProcess.getOriginalProcess().pid());
    }
    return new WinProcess((int)process.pid());
  }
}
