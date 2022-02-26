// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.internal;

import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.ui.RemoteDesktopService;
import consulo.application.ui.UISettings;

import javax.swing.*;
import java.awt.*;

import static consulo.application.ApplicationManager.getApplication;

public final class ScrollSettings {
  public static boolean isEligibleFor(Component component) {
    if (component == null || !component.isShowing()) return false;

    Application application = getApplication();
    if (application == null) return false;
    if (PowerSaveMode.isEnabled()) return false;
    if (RemoteDesktopService.isRemoteSession()) return false;

    UISettings settings = UISettings.getInstanceOrNull();
    return settings != null && settings.SMOOTH_SCROLLING;
  }

  public static boolean isHighPrecisionEnabled() {
    return true;
  }

  public static boolean isPixelPerfectEnabled() {
    return true;
  }

  public static boolean isDebugEnabled() {
    return false;
  }

  public static boolean isBackgroundFromView() {
    return true;
  }

  public static boolean isHeaderOverCorner(JViewport viewport) {
    Component view = viewport == null ? null : viewport.getView();
    return !isNotSupportedYet(view);
  }

  public static boolean isNotSupportedYet(Component view) {
    return view instanceof JTable;
  }

  public static boolean isGapNeededForAnyComponent() {
    return true;
  }

  public static boolean isHorizontalGapNeededOnMac() {
    return false;
  }

  public static boolean isThumbSmallIfOpaque() {
    return false;
  }

  /* A heuristics that disables scrolling interpolation in diff / merge windows.
     We need to to make scrolling synchronization compatible with the interpolation first.

     NOTE: The implementation is a temporary, ad-hoc heuristics that is needed solely to
           facilitate testing of the experimental "true smooth scrolling" feature. */
  public static boolean isInterpolationEligibleFor(JScrollBar scrollbar) {
    Window window = (Window)scrollbar.getTopLevelAncestor();

    if (window instanceof JDialog && "Commit Changes".equals(((JDialog)window).getTitle())) {
      return false;
    }

    if (!(window instanceof RootPaneContainer)) {
      return true;
    }

    Component[] components = ((RootPaneContainer)window).getContentPane().getComponents();

    if (components.length == 1 && components[0].getClass().getName().contains("DiffWindow")) {
      return false;
    }

    if (components.length == 2 && components[0] instanceof Container) {
      Component[] subComponents = ((Container)components[0]).getComponents();
      if (subComponents.length == 1) {
        String name = subComponents[0].getClass().getName();
        if (name.contains("DiffWindow") || name.contains("MergeWindow")) {
          return false;
        }
      }
    }

    return true;
  }
}
