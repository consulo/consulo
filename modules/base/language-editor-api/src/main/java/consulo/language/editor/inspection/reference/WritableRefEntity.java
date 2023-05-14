// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inspection.reference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface WritableRefEntity extends RefEntity {
  void setOwner(@Nullable WritableRefEntity owner);

  void add(@Nonnull RefEntity child);

  void removeChild(@Nonnull RefEntity child);
}
