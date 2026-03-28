// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class TextCodeVisionEntry extends CodeVisionEntry {
    public final String text;

    public TextCodeVisionEntry(String text,
                               String providerId,
                               @Nullable Icon icon,
                               String longPresentation,
                               String tooltip,
                               List<CodeVisionEntryExtraActionModel> extraActions) {
        super(providerId, icon, longPresentation, tooltip, extraActions);
        this.text = text;
    }

    public TextCodeVisionEntry(String text, String providerId) {
        this(text, providerId, null, text, text, Collections.emptyList());
    }
}
