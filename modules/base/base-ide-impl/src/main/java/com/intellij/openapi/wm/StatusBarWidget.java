/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.wm;

import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.border.Border;
import java.awt.event.MouseEvent;

/**
 * User: spLeaner
 */
public interface StatusBarWidget extends Disposable {
  @Nonnull
  String ID();

  @Nullable
  WidgetPresentation getPresentation();

  void install(@Nonnull final StatusBar statusBar);

  interface Multiframe extends StatusBarWidget {
    StatusBarWidget copy();
  }

  interface WidgetPresentation {
    @Nullable
    String getTooltipText();

    @Nullable
    default String getShortcutText() {
      return null;
    }

    @Nullable
    Consumer<MouseEvent> getClickConsumer();
  }

  interface IconPresentation extends WidgetPresentation {
    @Nullable
    Image getIcon();
  }

  interface TextPresentation extends WidgetPresentation {
    @Nonnull
    String getText();

    float getAlignment();
  }

  interface MultipleTextValuesPresentation extends WidgetPresentation {
    /**
     * @return null means the widget is unable to show the popup
     */
    @Nullable
    ListPopup getPopupStep();

    @Nullable
    @RequiredUIAccess
    String getSelectedValue();

    @Nonnull
    @Deprecated
    default String getMaxValue() {
      return "";
    }

    @Nullable
    default Image getIcon() {
      return null;
    }
  }

  class WidgetBorder {
    public static final Border ICON = JBUI.Borders.empty(0, 4);
    public static final Border INSTANCE = JBUI.Borders.empty(0, 2);
    public static final Border WIDE = JBUI.Borders.empty(0, 4);
  }
}
