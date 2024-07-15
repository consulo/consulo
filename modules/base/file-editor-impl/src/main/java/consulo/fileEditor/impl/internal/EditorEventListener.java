// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal;

import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.SelectionListener;
import consulo.document.Document;
import consulo.document.event.DocumentListener;
import jakarta.annotation.Nonnull;

public interface EditorEventListener extends DocumentListener, CaretListener, SelectionListener {
  default void readOnlyModificationAttempt(@Nonnull Document document) {
  }
}
