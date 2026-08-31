// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.codeEditor.Editor;
import consulo.ui.event.ComponentEvent;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * Same as {@link TextCodeVisionEntry}, but with predefined click handler.
 * <p>
 * WARNING: do not store PSI inside the handler. Use smart pointers to avoid accidental PSI capture.
 */
public class ClickableTextCodeVisionEntry extends TextCodeVisionEntry implements CodeVisionPredefinedActionEntry {
    /**
     * Left on the entry by whatever renderer dispatched the click, so the handler learns where it happened.
     * An invocation that arrives without one - the more popup, a shortcut - is handed a programmatic event
     * anchored on the editor instead.
     */
    public static final Key<ComponentEvent<?>> EVENT_KEY = Key.create("CodeVisionEntryEventKey");

    private final CodeVisionClickHandler onClick;

    public ClickableTextCodeVisionEntry(String text,
                                        String providerId,
                                        CodeVisionClickHandler onClick,
                                        @Nullable Icon icon,
                                        String longPresentation,
                                        String tooltip,
                                        List<CodeVisionEntryExtraActionModel> extraActions) {
        super(text, providerId, icon, longPresentation, tooltip, extraActions);
        this.onClick = onClick;
    }

    public ClickableTextCodeVisionEntry(String text, String providerId, CodeVisionClickHandler onClick) {
        this(text, providerId, onClick, null, text, "", Collections.emptyList());
    }

    @Override
    public void onClick(Editor editor) {
        ComponentEvent<?> event = getUserData(EVENT_KEY);
        onClick.onClick(event == null ? new ComponentEvent<>(editor.getUIComponent()) : event, editor);
    }

    @FunctionalInterface
    public interface CodeVisionClickHandler {
        void onClick(ComponentEvent<?> event, Editor editor);
    }
}
