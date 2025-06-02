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
package consulo.diff.impl.internal.content;

import consulo.language.psi.PsiFile;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;

import java.util.Objects;

public class DiffPsiFileSupport {
  public static final Key<Boolean> KEY = Key.create("Diff.DiffPsiFileSupport");


  public static boolean isDiffFile(@Nullable PsiFile file) {
    return file != null && isDiffFile(file.getVirtualFile());
  }

  public static boolean isDiffFile(@Nullable VirtualFile file) {
    return file != null && Objects.equals(file.getUserData(KEY), Boolean.TRUE);
  }
}
