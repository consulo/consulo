// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.mac.foundation;

import consulo.util.lang.Comparing;
import jakarta.annotation.Nullable;

import static consulo.application.util.mac.foundation.Foundation.invoke;
import static consulo.application.util.mac.foundation.Foundation.toStringViaUTF8;

/**
 * @author pegov
 */
public class MacUtil {
  private MacUtil() {
  }

  @Nullable
  public static ID findWindowForTitle(@Nullable String title) {
    if (title == null || title.isEmpty()) return null;
    final ID pool = invoke("NSAutoreleasePool", "new");

    ID focusedWindow = null;
    try {
      final ID sharedApplication = invoke("NSApplication", "sharedApplication");
      final ID windows = invoke(sharedApplication, "windows");
      final ID windowEnumerator = invoke(windows, "objectEnumerator");

      while (true) {
        // dirty hack: walks through all the windows to find a cocoa window to show sheet for
        final ID window = invoke(windowEnumerator, "nextObject");
        if (0 == window.intValue()) break;

        final ID windowTitle = invoke(window, "title");
        if (windowTitle != null && windowTitle.intValue() != 0) {
          final String titleString = toStringViaUTF8(windowTitle);
          if (Comparing.equal(titleString, title)) {
            focusedWindow = window;
            break;
          }
        }
      }
    }
    finally {
      invoke(pool, "release");
    }

    return focusedWindow;
  }
}
