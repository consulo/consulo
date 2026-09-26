// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.webBrowser.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.toolbar.floating.AbstractFloatingToolbarProvider;

@ExtensionImpl(id = "OpenInBrowserFloatingToolbarProvider", order = "after DefaultFloatingToolbarProvider")
public class OpenInBrowserFloatingToolbarProvider extends AbstractFloatingToolbarProvider {
    private static final String ACTION_GROUP = "OpenInBrowserEditorContextBarGroup";

    public OpenInBrowserFloatingToolbarProvider() {
        super(ACTION_GROUP);
    }
}
