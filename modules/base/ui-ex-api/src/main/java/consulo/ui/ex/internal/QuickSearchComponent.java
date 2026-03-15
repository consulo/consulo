// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.internal;

import consulo.ui.ex.popup.JBPopup;

public interface QuickSearchComponent {

  void registerHint(JBPopup h);

  void unregisterHint();
}
