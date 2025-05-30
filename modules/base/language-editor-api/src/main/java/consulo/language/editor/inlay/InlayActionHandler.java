// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.component.extension.ExtensionPointName;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Handler for inlay clicks (must be clicked while holding ctrl).
 * It can be found by id, which is provided during the construction of inlay
 * in PresentationTreeBuilder. Must have reasonable equals. Otherwise, it will
 * be required to replace the corresponding tree element each time the
 * inlay provider is run.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface InlayActionHandler {
    ExtensionPointName<InlayActionHandler> EP = ExtensionPointName.create(InlayActionHandler.class);

    @Nullable
    static InlayActionHandler getActionHandler(String handlerId) {
        return EP.findFirstSafe(b -> handlerId.equals(b.getHandlerId()));
    }

    @Nonnull
    String getHandlerId();

    /**
     * Handles click on the corresponding inlay entry. Payload is provided by the entry.
     */
    @RequiredUIAccess
    @Deprecated
    @DeprecationInfo("Please override handleClick(EditorMouseEvent, InlayActionPayload) instead")
    default void handleClick(Editor editor, InlayActionPayload payload) {
        throw new UnsupportedOperationException();
    }

    /**
     * Handles click on the corresponding inlay entry. Payload is provided by the entry.
     */
    @RequiredUIAccess
    default void handleClick(EditorMouseEvent e, InlayActionPayload payload) {
        handleClick(e.getEditor(), payload);
    }
}
