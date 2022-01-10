// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.components.fields;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

import static com.intellij.util.ui.JBUI.scale;

/**
 * @author Konstantin Bulenkov
 * @author Sergey Malenkov
 */
public interface ExtendableTextComponent {
  String VARIANT = "extendable";

  List<Extension> getExtensions();

  void setExtensions(Extension... extensions);

  void setExtensions(Collection<? extends Extension> extensions);

  void addExtension(@Nonnull Extension extension);

  void removeExtension(@Nonnull Extension extension);

  interface Extension {
    Image getIcon(boolean hovered);

    default int getIconGap() {
      return scale(5);
    }

    default int getPreferredSpace() {
      Image icon1 = getIcon(true);
      Image icon2 = getIcon(false);
      if (icon1 == null && icon2 == null) return 0;
      if (icon1 == null) return getIconGap() + icon2.getWidth();
      if (icon2 == null) return getIconGap() + icon1.getWidth();
      return getIconGap() + Math.max(icon1.getWidth(), icon2.getWidth());
    }

    default int getAfterIconOffset() {
      return 0;
    }

    default boolean isIconBeforeText() {
      return false;
    }

    default Runnable getActionOnClick() {
      return null;
    }

    default String getTooltip() {
      return null;
    }

    static Extension create(@Nonnull Image icon, String tooltip, Runnable action) {
      return create(icon, icon, tooltip, action);
    }

    static Extension create(@Nonnull Image defaultIcon, @Nonnull Image hoveredIcon, String tooltip, Runnable action) {
      return new Extension() {
        @Override
        public Image getIcon(boolean hovered) {
          return hovered ? hoveredIcon : defaultIcon;
        }

        @Override
        public String getTooltip() {
          return tooltip;
        }

        @Override
        public Runnable getActionOnClick() {
          return action;
        }
      };
    }
  }
}
