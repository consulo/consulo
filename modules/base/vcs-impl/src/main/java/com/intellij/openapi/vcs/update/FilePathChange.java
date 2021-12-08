/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.openapi.vcs.update;

import com.intellij.openapi.vcs.FilePath;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 08/12/2021
 *
 * from kolin
 */
public interface FilePathChange {
  public static class Simple implements FilePathChange {
    private final FilePath myBeforePath;
    private final FilePath myAfterPath;

    public Simple(FilePath beforePath, FilePath afterPath) {
      myBeforePath = beforePath;
      myAfterPath = afterPath;
    }

    @Nullable
    @Override
    public FilePath getBeforePath() {
      return myBeforePath;
    }

    @Nullable
    @Override
    public FilePath getAfterPath() {
      return myAfterPath;
    }
  }

  @Nullable
  FilePath getBeforePath();

  @Nullable
  FilePath getAfterPath();
}
