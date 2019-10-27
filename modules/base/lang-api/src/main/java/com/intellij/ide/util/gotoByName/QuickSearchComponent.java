// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.openapi.ui.popup.JBPopup;
import javax.annotation.Nonnull;

public interface QuickSearchComponent {

  void registerHint(@Nonnull JBPopup h);

  void unregisterHint();
}
