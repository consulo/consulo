// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.desktop.awt.editor.impl;

import consulo.annotation.DeprecationInfo;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorCaretModelBase;

import jakarta.annotation.Nonnull;

public class DesktopCaretModelImpl extends CodeEditorCaretModelBase<DesktopCaretImpl> {
  public DesktopCaretModelImpl(@Nonnull DesktopEditorImpl editor) {
    super(editor);
  }

  @Nonnull
  @Override
  protected DesktopCaretImpl createCaret(CodeEditorBase editor, CodeEditorCaretModelBase<DesktopCaretImpl> model) {
    return new DesktopCaretImpl((DesktopEditorImpl)editor, (DesktopCaretModelImpl)model);
  }
}
