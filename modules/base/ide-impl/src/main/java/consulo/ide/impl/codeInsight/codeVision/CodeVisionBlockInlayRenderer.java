// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.hints.HintRenderer;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.language.editor.codeVision.CodeVisionPredefinedActionEntry;
import consulo.language.editor.codeVision.DaemonBoundCodeVisionProvider;
import consulo.language.editor.codeVision.ClickableTextCodeVisionEntry;

import java.awt.event.MouseEvent;

/**
 * Block inlay renderer for a single code vision entry.
 * <p>
 * Painted above the corresponding line using {@link HintRenderer}, and dispatches
 * mouse-click events to the owning {@link DaemonBoundCodeVisionProvider}.
 */
public class CodeVisionBlockInlayRenderer extends HintRenderer {
    private final DaemonBoundCodeVisionProvider provider;
    private final TextRange textRange;
    private final CodeVisionEntry entry;

    public CodeVisionBlockInlayRenderer(String text,
                                        DaemonBoundCodeVisionProvider provider,
                                        TextRange textRange,
                                        CodeVisionEntry entry) {
        super(text);
        this.provider = provider;
        this.textRange = textRange;
        this.entry = entry;
    }

    public DaemonBoundCodeVisionProvider getProvider() {
        return provider;
    }

    public TextRange getTextRange() {
        return textRange;
    }

    public CodeVisionEntry getEntry() {
        return entry;
    }

    /**
     * Called when the user clicks on this inlay.
     */
    public void handleClick(MouseEvent event, Editor editor) {
        if (entry instanceof CodeVisionPredefinedActionEntry actionEntry) {
            if (event != null) {
                entry.putUserData(ClickableTextCodeVisionEntry.MOUSE_EVENT_KEY, event);
            }
            actionEntry.onClick(editor);
        } else {
            provider.handleClick(editor, textRange, entry);
        }
    }
}
