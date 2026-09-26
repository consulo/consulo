// Copyright 2000-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl.internal.floating;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.toolbar.floating.AbstractFloatingToolbarProvider;

@ExtensionImpl(id = "DefaultFloatingToolbarProvider")
public class DefaultFloatingToolbarProvider extends AbstractFloatingToolbarProvider {
    private static final String ACTION_GROUP = "EditorContextBarMenu";

    public DefaultFloatingToolbarProvider() {
        super(ACTION_GROUP);
    }
}
