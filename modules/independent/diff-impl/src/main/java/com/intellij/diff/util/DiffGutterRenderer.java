/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.diff.util;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAwareAction;
import javax.annotation.Nonnull;

import javax.swing.*;

public abstract class DiffGutterRenderer extends GutterIconRenderer {
  @Nonnull
  private final Icon myIcon;
  @javax.annotation.Nullable
  private final String myTooltip;

  public DiffGutterRenderer(@Nonnull Icon icon, @javax.annotation.Nullable String tooltip) {
    myIcon = icon;
    myTooltip = tooltip;
  }

  @Nonnull
  @Override
  public Icon getIcon() {
    return myIcon;
  }

  @javax.annotation.Nullable
  @Override
  public String getTooltipText() {
    return myTooltip;
  }

  public boolean isNavigateAction() {
    return true;
  }

  @Override
  public boolean isDumbAware() {
    return true;
  }

  @Nonnull
  @Override
  public Alignment getAlignment() {
    return Alignment.LEFT;
  }

  @javax.annotation.Nullable
  @Override
  public AnAction getClickAction() {
    return new DumbAwareAction() {
      @Override
      public void actionPerformed(AnActionEvent e) {
        performAction(e);
      }
    };
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  protected abstract void performAction(AnActionEvent e);
}
