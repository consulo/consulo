// File: InlayGroupSettingProvider.java
// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.inlay.InlayGroup;

import javax.swing.*;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface InlayGroupSettingProvider {
    ExtensionPointName<InlayGroupSettingProvider> EXTENSION_POINT_NAME = ExtensionPointName.create(InlayGroupSettingProvider.class);

    public static InlayGroupSettingProvider findForGroup(InlayGroup group) {
        return EXTENSION_POINT_NAME.getExtensionList().stream()
            .filter(provider -> provider.getGroup() == group)
            .findFirst()
            .orElse(null);
    }

    InlayGroup getGroup();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    JComponent getComponent();

    void apply();

    boolean isModified();

    void reset();
}
