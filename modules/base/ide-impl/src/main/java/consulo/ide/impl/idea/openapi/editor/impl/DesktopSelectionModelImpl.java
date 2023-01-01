// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.codeEditor.SelectionModel;
import consulo.annotation.DeprecationInfo;
import consulo.codeEditor.impl.CodeEditorSelectionModelBase;

@Deprecated
@DeprecationInfo("Desktop implementation")
public class DesktopSelectionModelImpl extends CodeEditorSelectionModelBase implements SelectionModel {
  public DesktopSelectionModelImpl(DesktopEditorImpl editor) {
    super(editor);
  }
}
