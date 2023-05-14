// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl;

import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorInlayModelBase;

import jakarta.annotation.Nonnull;

public class InlayModelImpl extends CodeEditorInlayModelBase {
  InlayModelImpl(@Nonnull CodeEditorBase editor) {
    super(editor);
  }
}
