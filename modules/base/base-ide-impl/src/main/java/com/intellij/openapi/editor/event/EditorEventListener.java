// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.event;

import com.intellij.openapi.editor.Document;
import javax.annotation.Nonnull;

public interface EditorEventListener extends DocumentListener, CaretListener, SelectionListener {
  default void readOnlyModificationAttempt(@Nonnull Document document) {
  }
}
