// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.desktop.awt.editor.impl;

import consulo.annotation.DeprecationInfo;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorCaretModelBase;

public class DesktopCaretModelImpl extends CodeEditorCaretModelBase<DesktopCaretImpl> {
  public DesktopCaretModelImpl(DesktopEditorImpl editor) {
    super(editor);
  }

  
  @Override
  protected DesktopCaretImpl createCaret(CodeEditorBase editor, CodeEditorCaretModelBase<DesktopCaretImpl> model) {
    return new DesktopCaretImpl((DesktopEditorImpl)editor, (DesktopCaretModelImpl)model);
  }
}
