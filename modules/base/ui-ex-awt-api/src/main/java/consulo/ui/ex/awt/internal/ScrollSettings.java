// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.internal;

import javax.swing.*;
import java.awt.*;

@Deprecated
public final class ScrollSettings {
  public static boolean isHeaderOverCorner(JViewport viewport) {
    Component view = viewport == null ? null : viewport.getView();
    return !isNotSupportedYet(view);
  }

  public static boolean isNotSupportedYet(Component view) {
    return view instanceof JTable;
  }
}
