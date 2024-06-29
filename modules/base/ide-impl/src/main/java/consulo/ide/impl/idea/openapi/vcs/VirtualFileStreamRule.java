/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataProvider;
import consulo.dataContext.GetDataRule;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.stream.Stream;

@ExtensionImpl
public class VirtualFileStreamRule implements GetDataRule<Stream<VirtualFile>> {
  @Nonnull
  @Override
  public Key<Stream<VirtualFile>> getKey() {
    return VcsDataKeys.VIRTUAL_FILE_STREAM;
  }

  @Nullable
  @Override
  public Stream<VirtualFile> getData(@Nonnull DataProvider dataProvider) {
    VirtualFile[] files = dataProvider.getDataUnchecked(VirtualFile.KEY_OF_ARRAY);
    if (files != null) {
      return Stream.of(files);
    }

    VirtualFile file = dataProvider.getDataUnchecked(VirtualFile.KEY);
    if (file != null) {
      return Stream.of(file);
    }

    return null;
  }
}