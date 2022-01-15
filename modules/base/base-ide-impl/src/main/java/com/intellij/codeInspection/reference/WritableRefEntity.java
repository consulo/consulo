// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface WritableRefEntity extends RefEntity {
  void setOwner(@Nullable WritableRefEntity owner);

  void add(@Nonnull RefEntity child);

  void removeChild(@Nonnull RefEntity child);
}
