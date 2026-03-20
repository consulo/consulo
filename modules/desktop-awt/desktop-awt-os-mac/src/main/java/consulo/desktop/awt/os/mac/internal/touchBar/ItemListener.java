// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.os.mac.internal.touchBar;

interface ItemListener {
    // NOTE: called from AppKit thread
    void onItemEvent(TBItem src, int evcode);
}
