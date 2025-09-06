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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.util.io.PathUtil;
import consulo.versionControlSystem.change.patch.FilePatch;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ShelvedBinaryFilePatch extends FilePatch {
  private final ShelvedBinaryFileImpl myShelvedBinaryFile;

  public ShelvedBinaryFilePatch(@Nonnull ShelvedBinaryFileImpl shelvedBinaryFile) {
    myShelvedBinaryFile = shelvedBinaryFile;
    setBeforeName(myShelvedBinaryFile.BEFORE_PATH);
    setAfterName(myShelvedBinaryFile.AFTER_PATH);
  }

  public static ShelvedBinaryFilePatch patchCopy(@Nonnull ShelvedBinaryFilePatch patch) {
    return new ShelvedBinaryFilePatch(patch.getShelvedBinaryFile());
  }

  @Override
  @Nullable
  public String getBeforeFileName() {
    return getFileName(myShelvedBinaryFile.BEFORE_PATH);
  }

  @Override
  @Nullable
  public String getAfterFileName() {
    return getFileName(myShelvedBinaryFile.AFTER_PATH);
  }

  @Nullable
  private static String getFileName(String filePath) {
    return filePath != null ? PathUtil.getFileName(filePath) : null;
  }

  @Override
  public boolean isNewFile() {
    return myShelvedBinaryFile.BEFORE_PATH == null;
  }

  @Override
  public boolean isDeletedFile() {
    return myShelvedBinaryFile.AFTER_PATH == null;
  }

  @Nonnull
  public ShelvedBinaryFileImpl getShelvedBinaryFile() {
    return myShelvedBinaryFile;
  }
}
