// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui;

import java.awt.*;

/**
 * This interface is supposed to be implemented by Swing components, for which preferred width and height are not independent. Example is a
 * text component where text is broken into lines according to available width. Laying out such a component usually implies calculating its
 * preferred width, then calculating actual width to use (e.g. by fitting it to horizontal space, available to the parent component), and
 * finally calculating the component preferred height, based on the chosen width. *
 */
public interface WidthBasedLayout {
  int getPreferredWidth();

  int getPreferredHeight(int width);

  static int getPreferredWidth(Component component) {
    return component == null ? 0 : component instanceof WidthBasedLayout ? ((WidthBasedLayout)component).getPreferredWidth() : component.getPreferredSize().width;
  }

  static int getPreferredHeight(Component component, int width) {
    return component == null ? 0 : component instanceof WidthBasedLayout ? ((WidthBasedLayout)component).getPreferredHeight(width) : component.getPreferredSize().height;
  }
}
