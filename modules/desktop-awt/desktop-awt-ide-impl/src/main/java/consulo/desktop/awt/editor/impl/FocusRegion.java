// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl;

import consulo.document.impl.RangeMarkerImpl;
import consulo.codeEditor.Editor;
import consulo.document.internal.DocumentEx;
import jakarta.annotation.Nonnull;

public class FocusRegion extends RangeMarkerImpl {
  private final Editor myEditor;

  FocusRegion(@Nonnull Editor editor, int start, int end) {
    super((DocumentEx)editor.getDocument(), start, end, false, false);
    this.myEditor = editor;
  }

  public Editor getEditor() {
    return myEditor;
  }
}
