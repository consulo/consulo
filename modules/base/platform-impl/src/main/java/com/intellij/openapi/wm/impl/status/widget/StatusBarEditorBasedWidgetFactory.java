// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.status.widget;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class StatusBarEditorBasedWidgetFactory implements StatusBarWidgetFactory {
  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return true;
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return getTextEditor(statusBar) != null;
  }

  @Nullable
  protected FileEditor getFileEditor(@Nonnull StatusBar statusBar) {
    return StatusBarUtil.getCurrentFileEditor(statusBar);
  }

  @Nullable
  protected Editor getTextEditor(@Nonnull StatusBar statusBar) {
    return StatusBarUtil.getCurrentTextEditor(statusBar);
  }
}
