// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.codeEditor.Editor;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

/**
 * Same as {@link TextCodeVisionEntry}, but with predefined click handler.
 * <p>
 * WARNING: do not store PSI inside the handler. Use smart pointers to avoid accidental PSI capture.
 */
public class ClickableTextCodeVisionEntry extends TextCodeVisionEntry implements CodeVisionPredefinedActionEntry {
    public static final Key<MouseEvent> MOUSE_EVENT_KEY = Key.create("CodeVisionEntryMouseEventKey");

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
        MouseEvent mouseEvent = getUserData(MOUSE_EVENT_KEY);
        onClick.onClick(mouseEvent, editor);
    }

    @FunctionalInterface
    public interface CodeVisionClickHandler {
        void onClick(@Nullable MouseEvent event, Editor editor);
    }
}
