// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.editor.impl;

import consulo.annotation.DeprecationInfo;
import consulo.editor.impl.CodeEditorBase;
import consulo.editor.impl.CodeEditorCaretModelBase;

import javax.annotation.Nonnull;

@Deprecated
@DeprecationInfo("Desktop only")
@SuppressWarnings("deprecation")
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
