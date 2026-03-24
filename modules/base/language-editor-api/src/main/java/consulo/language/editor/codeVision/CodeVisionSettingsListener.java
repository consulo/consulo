// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

/**
 * Listener for changes to {@link CodeVisionSettings}.
 * <p>
 * Mirrors JB's {@code CodeVisionSettings.CodeVisionSettingsListener}.
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface CodeVisionSettingsListener {

    default void globalEnabledChanged(boolean newValue) {
    }

    default void providerAvailabilityChanged(String id, boolean isEnabled) {
    }

    default void groupPositionChanged(String id, CodeVisionAnchorKind position) {
    }

    default void defaultPositionChanged(CodeVisionAnchorKind newDefaultPosition) {
    }

    default void visibleMetricsAboveDeclarationCountChanged(int newValue) {
    }

    default void visibleMetricsNextToDeclarationCountChanged(int newValue) {
    }
}
