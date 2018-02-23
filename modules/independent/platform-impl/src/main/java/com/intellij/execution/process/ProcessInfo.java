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
package com.intellij.execution.process;

import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Optional;


public class ProcessInfo {
  public static ProcessInfo[] EMPTY_ARRAY = new ProcessInfo[0];

  private final int myPid;
  @Nonnull
  private final String myCommandLine;
  @Nonnull
  private final Optional<String> myExecutablePath;
  @Nonnull
  private final String myExecutableName;
  @Nonnull
  private final String myArgs;

  public ProcessInfo(int pid,
                     @Nonnull String commandLine,
                     @Nonnull String executableName,
                     @Nonnull String args) {
    myPid = pid;
    myCommandLine = commandLine;
    myExecutablePath = Optional.empty();
    myExecutableName = executableName;
    myArgs = args;
  }

  public ProcessInfo(int pid,
                     @Nonnull String commandLine,
                     @Nonnull String executableName,
                     @Nonnull String args,
                     @Nullable String executablePath) {
    myPid = pid;
    myCommandLine = commandLine;
    myExecutableName = executableName;
    myExecutablePath = StringUtil.isNotEmpty(executablePath) ? Optional.of(executablePath) : Optional.empty();
    myArgs = args;
  }

  public int getPid() {
    return myPid;
  }

  @Nonnull
  public String getCommandLine() {
    return myCommandLine;
  }

  @Nonnull
  public String getExecutableName() {
    return myExecutableName;
  }

  @Nonnull
  public Optional<String> getExecutableCannonicalPath() {
    return myExecutablePath.map(s -> {
      try {
        return new File(s).getCanonicalPath();
      }
      catch (IOException e) {
        return s;
      }
    });
  }

  @Nonnull
  public String getExecutableDisplayName() {
    return StringUtil.trimEnd(myExecutableName, ".exe", true);
  }

  @Nonnull
  public String getArgs() {
    return myArgs;
  }

  @Override
  public String toString() {
    return myPid + " '" + myCommandLine + "' '" + myExecutableName + "' '" + myArgs + "'" +
           (myExecutablePath.isPresent() ? " " + myExecutablePath.get() : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProcessInfo info = (ProcessInfo)o;

    if (myPid != info.myPid) return false;
    if (!myExecutableName.equals(info.myExecutableName)) return false;
    if (!myArgs.equals(info.myArgs)) return false;
    if (!myCommandLine.equals(info.myCommandLine)) return false;
    if (!myExecutablePath.equals(info.myExecutablePath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myPid;
    result = 31 * result + myExecutableName.hashCode();
    result = 31 * result + myArgs.hashCode();
    result = 31 * result + myCommandLine.hashCode();
    return result;
  }
}