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

@SuppressWarnings({"HardCodedStringLiteral", "UtilityClassWithoutPrivateConstructor"})
public class Patches {
  /**
   * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6322854.
   * java.lang.NullPointerException: Failed to retrieve atom name.
   */
  public static final boolean SUN_BUG_ID_6322854 = Platform.current().os().isXWindow();

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
   * JDK on Mac detects font style for system fonts based only on their name (PostScript name).
   * This doesn't work for some fonts which don't use recognizable style suffixes in their names.
   * Corresponding JDK request for enhancement - <a href="https://bugs.openjdk.java.net/browse/JDK-8139151">JDK-8139151</a>.
   */
  public static final boolean JDK_MAC_FONT_STYLE_DETECTION_WORKAROUND = Platform.current().os().isMac();

  /**
   * Older JDK versions could mistakenly use derived italics font, when genuine italics font was available in the system.
   * The issue was fixed in JDK 1.8.0_60 as part of <a href="https://bugs.openjdk.java.net/browse/JDK-8064833">JDK-8064833</a>.
   */
  public static final boolean JDK_MAC_FONT_STYLE_BUG = false;

}
