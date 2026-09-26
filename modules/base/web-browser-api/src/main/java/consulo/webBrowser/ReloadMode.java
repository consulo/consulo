// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.webBrowser;

import consulo.localize.LocalizeValue;
import consulo.webBrowser.localize.WebBrowserLocalize;

public enum ReloadMode {
    DISABLED(WebBrowserLocalize.webPreviewReloadModeDisabled()),
    RELOAD_ON_SAVE(WebBrowserLocalize.webPreviewReloadModeOnSave()),
    RELOAD_ON_CHANGE(WebBrowserLocalize.webPreviewReloadModeOnChange());

    private final LocalizeValue myTitle;

    ReloadMode(LocalizeValue title) {
        myTitle = title;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }
}
