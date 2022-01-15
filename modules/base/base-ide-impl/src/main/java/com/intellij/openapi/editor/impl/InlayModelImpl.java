// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import consulo.editor.impl.CodeEditorInlayModelBase;

import javax.annotation.Nonnull;

public class InlayModelImpl extends CodeEditorInlayModelBase {
  InlayModelImpl(@Nonnull DesktopEditorImpl editor) {
    super(editor);
  }
}
