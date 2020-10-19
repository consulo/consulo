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
package com.intellij.execution.process.impl;

import com.intellij.execution.process.OSProcessManager;
import com.intellij.execution.process.RunnerWinProcess;
import com.intellij.execution.process.UnixProcessManager;
import com.intellij.openapi.util.SystemInfo;
import consulo.logging.Logger;
import jakarta.inject.Singleton;
import org.jvnet.winp.WinProcess;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
@Singleton
public class OSProcessManagerImpl extends OSProcessManager {
  private static final Logger LOG = Logger.getInstance(OSProcessManagerImpl.class);

  @Override
  public boolean killProcessTree(@Nonnull Process process) {
    if (SystemInfo.isWindows) {
      try {
        WinProcess winProcess = createWinProcess(process);
        winProcess.killRecursively();
        return true;
      }
      catch (Throwable e) {
        LOG.info("Cannot kill process tree", e);
      }
    }
    else if (SystemInfo.isUnix) {
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
