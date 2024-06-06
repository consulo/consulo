/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.uiOld;

import consulo.application.AllIcons;
import consulo.desktop.awt.internal.notification.NotificationsManagerImpl;
import consulo.platform.Platform;
import consulo.project.ui.notification.Notification;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * @author Alexander Lobas
 */
public class BalloonLayoutConfiguration {
  public final int iconPanelWidth;
  public final Dimension iconOffset;

  public final int topSpaceHeight;
  public final int titleContentSpaceHeight;
  public final int contentActionsSpaceHeight;
  public final int titleActionsSpaceHeight;
  public final int bottomSpaceHeight;

  public final int actionGap;

  public final Dimension rightActionsOffset;
  public final int closeOffset;
  public final int gearCloseSpace;
  public final int allActionsOffset;
  public final int beforeGearSpace;

  public static int MaxFullContentWidth() {
    return JBUI.scale(350);
  }

  @Nonnull
  public static String MaxFullContentWidthStyle() {
    return "width:" + MaxFullContentWidth() + "px;";
  }

  public static int MinWidth() {
    return JBUI.scale(100);
  }

  private static final int RawWidth;
  private static final int RawStyleWidth;

  static {
    int width;

    if (Platform.current().os().isMac()) {
      width = 360;
      RawStyleWidth = 240;
    }
    else if (Platform.current().os().isLinux()) {
      width = 410;
      RawStyleWidth = 270;
    }
    else {
      width = 330;
      RawStyleWidth = 205;
    }

    width += AllIcons.Ide.Shadow.Left.getWidth();
    width += AllIcons.Ide.Shadow.Right.getWidth();

    RawWidth = width;
  }

  public static int FixedWidth() {
    return JBUI.scale(RawWidth);
  }

  public static int MaxWidth() {
    return JBUI.scale(RawWidth - 60);
  }

  public static String MaxWidthStyle() {
    return "width:" + JBUI.scale(RawStyleWidth) + "px;";
  }

  @Nonnull
  public static BalloonLayoutConfiguration create(@Nonnull Notification notification,
                                                  @Nonnull BalloonLayoutData layoutData,
                                                  boolean actions) {
    boolean hasTitle = notification.hasTitle();
    boolean hasContent = notification.hasContent();
    if (hasTitle && hasContent && actions) {
      return treeLines();
    }
    if (hasContent && NotificationsManagerImpl.calculateContentHeight(hasTitle || actions ? 1 : 2) < layoutData.fullHeight) {
      return treeLines();
    }
    return twoLines();
  }

  @Nonnull
  public BalloonLayoutConfiguration replace(int topSpaceHeight, int bottomSpaceHeight) {
    return new BalloonLayoutConfiguration(iconPanelWidth, iconOffset, topSpaceHeight, titleContentSpaceHeight, contentActionsSpaceHeight,
                                          titleActionsSpaceHeight, bottomSpaceHeight, actionGap, null, 0, 0, 0);
  }

  @Nonnull
  private static BalloonLayoutConfiguration twoLines() {
    return new BalloonLayoutConfiguration(new JBDimension(10, 11),
                                          JBUI.scale(11), JBUI.scale(5), JBUI.scale(5), JBUI.scale(5), JBUI.scale(14));
  }

  @Nonnull
  private static BalloonLayoutConfiguration treeLines() {
    return new BalloonLayoutConfiguration(new JBDimension(10, 7),
                                          JBUI.scale(7), JBUI.scale(3), JBUI.scale(7), 0, JBUI.scale(8));
  }

  private BalloonLayoutConfiguration(@Nonnull Dimension iconOffset,
                                     int topSpaceHeight,
                                     int titleContentSpaceHeight,
                                     int contentActionsSpaceHeight,
                                     int titleActionsSpaceHeight,
                                     int bottomSpaceHeight) {
    this(JBUI.scale(32), iconOffset,
         topSpaceHeight, titleContentSpaceHeight, contentActionsSpaceHeight, titleActionsSpaceHeight, bottomSpaceHeight,
         JBUI.scale(16),
         new JBDimension(8, 6), JBUI.scale(7), JBUI.scale(5), JBUI.scale(15));
  }

  private BalloonLayoutConfiguration(int iconPanelWidth,
                                     @Nonnull Dimension iconOffset,
                                     int topSpaceHeight,
                                     int titleContentSpaceHeight,
                                     int contentActionsSpaceHeight,
                                     int titleActionsSpaceHeight,
                                     int bottomSpaceHeight,
                                     int actionGap,
                                     @Nullable Dimension rightActionsOffset,
                                     int afterGearSpace,
                                     int beforeCloseSpace,
                                     int beforeGearSpace) {
    this.iconPanelWidth = iconPanelWidth;
    this.iconOffset = iconOffset;
    this.topSpaceHeight = topSpaceHeight;
    this.titleContentSpaceHeight = titleContentSpaceHeight;
    this.contentActionsSpaceHeight = contentActionsSpaceHeight;
    this.titleActionsSpaceHeight = titleActionsSpaceHeight;
    this.bottomSpaceHeight = bottomSpaceHeight;
    this.actionGap = actionGap;

    if (rightActionsOffset == null) {
      this.rightActionsOffset = new Dimension();
      this.closeOffset = 0;
      this.gearCloseSpace = 0;
      this.allActionsOffset = 0;
      this.beforeGearSpace = 0;
    }
    else {
      this.rightActionsOffset = rightActionsOffset;
      this.closeOffset = beforeCloseSpace + AllIcons.Ide.Notification.Close.getWidth() + rightActionsOffset.width;
      this.gearCloseSpace = afterGearSpace + beforeCloseSpace;
      this.allActionsOffset = closeOffset + afterGearSpace + AllIcons.Ide.Notification.Gear.getWidth();
      this.beforeGearSpace = beforeGearSpace;
    }
  }
}