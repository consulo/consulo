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
package consulo.application.util;

import consulo.platform.Platform;

import java.awt.*;

@SuppressWarnings({"HardCodedStringLiteral", "UtilityClassWithoutPrivateConstructor"})
public class Patches {
  /**
   * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6322854.
   * java.lang.NullPointerException: Failed to retrieve atom name.
   */
  public static final boolean SUN_BUG_ID_6322854 = Platform.current().os().isXWindow();

  /**
   * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4893787.
   * BasicTreeUI.FocusHandler doesn't properly repaint JTree on focus changes.
   */
  public static final boolean SUN_BUG_ID_4893787 = true;

  /**
   * IBM java machine 1.4.2 crashes if debugger uses ObjectReference.disableCollection() and ObjectReference.enableCollection().
   */
  public static final boolean IBM_JDK_DISABLE_COLLECTION_BUG = "false".equalsIgnoreCase(System.getProperty("idea.debugger.keep.temp.objects"));

  /**
   * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4818143.
   * The bug is marked as fixed but it actually isn't - {@link java.awt.datatransfer.Clipboard#getContents(Object)} call may hang
   * for up to 10 seconds if clipboard owner is not responding.
   */
  public static final boolean SLOW_GETTING_CLIPBOARD_CONTENTS = Platform.current().os().isUnix();

  /**
   * Desktop API support on X Window is limited to GNOME (and even there it may work incorrectly).
   * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6486393.
   */
  public static final boolean SUN_BUG_ID_6486393 = Platform.current().os().isXWindow();

  /**
   * Desktop API calls may crash on Windows.
   * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6457572.
   */
  public static final boolean SUN_BUG_ID_6457572 = Platform.current().os().isWindows() && !SystemInfo.isJavaVersionAtLeast(7, 0, 0);

  /**
   * Java 7 incorrectly calculates screen insets on multi-monitor X Window configurations.
   * https://bugs.openjdk.java.net/browse/JDK-8020443
   */
  public static final boolean SUN_BUG_ID_8020443 = Platform.current().os().isXWindow() && SystemInfo.isJavaVersionAtLeast(7, 0, 0) && !SystemInfo.isJavaVersionAtLeast(9, 0, 0);

  /**
   * XToolkit.getScreenInsets() may be very slow.
   * See https://bugs.openjdk.java.net/browse/JDK-8170937.
   */
  public static boolean isJdkBugId8004103() {
    return Platform.current().os().isXWindow() && !GraphicsEnvironment.isHeadless();
  }

  /**
   * No BindException when another program is using the port.
   * See https://bugs.openjdk.java.net/browse/JDK-7179799.
   */
  public static final boolean SUN_BUG_ID_7179799 = Platform.current().os().isWindows() && !SystemInfo.isJavaVersionAtLeast(8, 0, 0);

  /**
   * Frame size reverts meaning of maximized attribute if frame size close to display.
   * See http://bugs.openjdk.java.net/browse/JDK-8007219
   * Fixed in JDK 8.
   */
  public static final boolean JDK_BUG_ID_8007219 = Platform.current().os().isMac() && SystemInfo.isJavaVersionAtLeast(7, 0, 0) && !SystemInfo.isJavaVersionAtLeast(8, 0, 0);

  /**
   * Support default methods in JDI
   * See <a href="https://bugs.openjdk.java.net/browse/JDK-8042123">JDK-8042123</a>
   */
  public static final boolean JDK_BUG_ID_8042123 = !SystemInfo.isJavaVersionAtLeast(8, 0, 45);

  /**
   * JDK on Mac detects font style for system fonts based only on their name (PostScript name).
   * This doesn't work for some fonts which don't use recognizable style suffixes in their names.
   * Corresponding JDK request for enhancement - <a href="https://bugs.openjdk.java.net/browse/JDK-8139151">JDK-8139151</a>.
   */
  public static final boolean JDK_MAC_FONT_STYLE_DETECTION_WORKAROUND = Platform.current().os().isMac();

  /**
   * Older JDK versions could mistakenly use derived italics font, when genuine italics font was available in the system.
   * The issue was fixed in JDK 1.8.0_60 as part of <a href="https://bugs.openjdk.java.net/browse/JDK-8064833">JDK-8064833</a>.
   */
  public static final boolean JDK_MAC_FONT_STYLE_BUG = Platform.current().os().isMac() && !SystemInfo.isJavaVersionAtLeast(8, 0, 60);

  /**
   * On Mac OS font ligatures are not supported for natively loaded fonts, font needs to be loaded explicitly by JDK.
   */
  public static final boolean JDK_BUG_ID_7162125 = Platform.current().os().isMac() && !SystemInfo.isJavaVersionAtLeast(9, 0, 0);

  /**
   * Some HTTP connections lock the context class loader: https://bugs.openjdk.java.net/browse/JDK-8032832
   */
  public static boolean JDK_BUG_ID_8032832 = true;

  /**
   * https://bugs.openjdk.java.net/browse/JDK-8220231
   */
  public static final boolean TEXT_LAYOUT_IS_SLOW = !SystemInfo.isJetBrainsJvm && !SystemInfo.isJavaVersionAtLeast(13, 0, 0);
}
