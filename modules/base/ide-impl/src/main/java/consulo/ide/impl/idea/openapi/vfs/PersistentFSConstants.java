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
package consulo.ide.impl.idea.openapi.vfs;

import consulo.annotation.DeprecationInfo;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.impl.internal.RawFileLoaderImpl;

@Deprecated
public class PersistentFSConstants {
  @Deprecated
  @DeprecationInfo("Use consulo.virtualFileSystem.RawFileLoader.getFileLengthToCacheThreshold")
  public static final long FILE_LENGTH_TO_CACHE_THRESHOLD = RawFileLoaderImpl.LARGE_FOR_CONTENT_LOADING;

  public static int getMaxIntellisenseFileSize() {
    return RawFileLoader.getInstance().getMaxIntellisenseFileSize();
  }

  private PersistentFSConstants() {
  }
}
