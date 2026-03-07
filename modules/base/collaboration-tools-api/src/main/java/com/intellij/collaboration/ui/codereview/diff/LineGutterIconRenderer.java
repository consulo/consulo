// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.diff;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.awt.EmptyIcon;
import jakarta.annotation.Nonnull;

import javax.swing.*;

@ApiStatus.ScheduledForRemoval
@Deprecated
@ApiStatus.Internal
public abstract class LineGutterIconRenderer extends GutterIconRenderer implements DumbAware, Disposable {

  public abstract int getLine();

  public abstract @Nonnull Icon getVisibleIcon();

  public abstract void setVisibleIcon(@Nonnull Icon icon);

  private boolean iconVisible = false;

  public boolean isIconVisible() {
    return iconVisible;
  }

  public void setIconVisible(boolean iconVisible) {
    this.iconVisible = iconVisible;
  }

  @Override
  public @Nonnull Icon getIcon() {
    return iconVisible ? getVisibleIcon() : EmptyIcon.ICON_16;
  }

  protected @Nonnull ShortcutSet getShortcut() {
    return CustomShortcutSet.EMPTY;
  }

  @Override
  public boolean isNavigateAction() {
    return true;
  }

  @Override
  public @Nonnull Alignment getAlignment() {
    return Alignment.RIGHT;
  }

  public abstract void disposeInlay();

  @Override
  public void dispose() {
    disposeInlay();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof AddCommentGutterIconRenderer otherRenderer)) return false;
    return getLine() == otherRenderer.getLine();
  }

  @Override
  public int hashCode() {
    return getLine();
  }
}
