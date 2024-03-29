/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileTypes.ex;

import consulo.language.file.FileTypeManager;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author max
 */
public abstract class FileTypeManagerEx extends FileTypeManager {
  public static FileTypeManagerEx getInstanceEx() {
    return (FileTypeManagerEx)getInstance();
  }

  @Nonnull
  public abstract String getExtension(@Nonnull String fileName);

  public abstract void fireFileTypesChanged();

  public abstract void fireBeforeFileTypesChanged();
}
