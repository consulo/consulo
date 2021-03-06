// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.injected.editor;

import com.intellij.openapi.editor.Editor;
import consulo.util.dataholder.UserDataHolderBase;
import javax.annotation.Nonnull;

/**
 * @deprecated Use {@link EditorWindow} instead. To be removed in IDEA 2018.1
 */
@Deprecated
//@ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
public abstract class EditorWindowImpl extends UserDataHolderBase implements EditorWindow {
  /**
   * @deprecated Use {@link EditorWindow#getDelegate()} instead. To be removed in IDEA 2018.1
   */
  //@ApiStatus.ScheduledForRemoval(inVersion = "2018.1")
  @Deprecated
  @Nonnull
  @Override
  public Editor getDelegate() {
    throw new IllegalStateException();
  }
}
