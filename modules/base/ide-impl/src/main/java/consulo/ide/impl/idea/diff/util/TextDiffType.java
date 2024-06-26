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
package consulo.ide.impl.idea.diff.util;

import consulo.codeEditor.Editor;
import consulo.diff.localize.DiffLocalize;
import consulo.ide.impl.idea.openapi.diff.DiffColors;
import consulo.ui.color.ColorValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface TextDiffType {
  @Nonnull
  TextDiffType INSERTED = TextDiffTypeFactory.getInstance()
    .createTextDiffType(DiffColors.DIFF_INSERTED, DiffLocalize.diffTypeInsertedName().get());
  @Nonnull
  TextDiffType DELETED = TextDiffTypeFactory.getInstance()
    .createTextDiffType(DiffColors.DIFF_DELETED, DiffLocalize.diffTypeDeletedName().get());
  @Nonnull
  TextDiffType MODIFIED = TextDiffTypeFactory.getInstance()
    .createTextDiffType(DiffColors.DIFF_MODIFIED, DiffLocalize.diffTypeChangedName().get());
  @Nonnull
  TextDiffType CONFLICT = TextDiffTypeFactory.getInstance()
    .createTextDiffType(DiffColors.DIFF_CONFLICT, DiffLocalize.diffTypeConflictName().get());

  @Nonnull
  String getName();

  @Nonnull
  ColorValue getColor(@Nullable Editor editor);

  @Nonnull
  ColorValue getIgnoredColor(@Nullable Editor editor);

  @Nullable
  ColorValue getMarkerColor(@Nullable Editor editor);
}
