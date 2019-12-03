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
package com.intellij.ide;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author kir
 */
public interface SelectInContext {
  Key<SelectInContext> DATA_KEY = Key.create("SelectInContext");

  @Nonnull
  Project getProject();

  @Nonnull
  VirtualFile getVirtualFile();

  @Nullable
  Object getSelectorInFile();

  @Nullable
  default Supplier<FileEditor> getFileEditorProvider() {
    return null;
  }

  /**
   * @param target       an object that supports a context selection
   * @param requestFocus specifies whether a focus request is needed or not
   * @return {@code true} if a selection request is approved and executed by the given target
   */
  default boolean selectIn(@Nonnull SelectInTarget target, boolean requestFocus) {
    if (!target.canSelect(this)) return false;
    target.selectIn(this, requestFocus);
    return true;
  }
}
