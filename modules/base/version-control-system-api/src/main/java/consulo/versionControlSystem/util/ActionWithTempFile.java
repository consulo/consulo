/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.util;

import consulo.annotation.UsedInPlugin;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.VcsException;

import java.io.File;
import java.io.IOException;

@UsedInPlugin
public abstract class ActionWithTempFile {
  private File myTempFile;
  private final File mySourceFile;
  private static final String TMP_PREFIX = "vcs";
  private static final String TMP_SUFFIX = "tmp";

  public ActionWithTempFile(File sourceFile){
    mySourceFile = sourceFile;
  }

  public void execute () throws VcsException {
    try {
      try {
        init();
        executeInternal();
      }
      finally {
        rollbackChanges();
      }
    }
    catch (IOException e) {
      throw new VcsException(e);
    }
  }

  private void init() throws IOException {
    myTempFile = FileUtil.createTempFile(TMP_PREFIX, TMP_SUFFIX);
    FileUtil.delete(myTempFile);
    FileUtil.rename(mySourceFile, myTempFile, FilePermissionCopier.BY_NIO2);
  }

  protected abstract void executeInternal() throws VcsException;

  private void rollbackChanges() throws IOException {
    try {
      FileUtil.delete(mySourceFile);
    }
    finally {
      try {
        FileUtil.rename(myTempFile, mySourceFile, FilePermissionCopier.BY_NIO2);
      }
      finally {
        FileUtil.delete(myTempFile);
      }
    }
  }
}
