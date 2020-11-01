// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.status;

import com.intellij.openapi.project.Project;
import consulo.disposer.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import com.intellij.ui.UIBundle;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

public class ReadOnlyAttributeWidgetFactory extends StatusBarEditorBasedWidgetFactory {
  @Override
  public
  @Nonnull
  String getId() {
    return StatusBar.StandardWidgets.READONLY_ATTRIBUTE_PANEL;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.read.only.widget.name");
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new ToggleReadOnlyAttributePanel();
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }
}
