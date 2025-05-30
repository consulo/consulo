// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.util.dataholder.Key;

import java.util.List;

/**
 * Interface for retrieving virtual formatting inlays in an editor.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface VirtualFormattingInlaysInfo {
    Key<Boolean> VISUAL_FORMATTING_ELEMENT_KEY = Key.create("visual.formatting.element");
    Key<CodeStyleSettings> EDITOR_VISUAL_FORMATTING_LAYER_CODE_STYLE_SETTINGS =
        Key.create("visual.formatting.layer.info");

    /**
     * @return the singleton service instance
     */
    static VirtualFormattingInlaysInfo getInstance() {
        return ApplicationManager.getApplication().getService(VirtualFormattingInlaysInfo.class);
    }

    /**
     * Measures total width of virtual formatting inline inlays between startOffset and endOffset.
     */
    static int measureVirtualFormattingInlineInlays(Editor editor, int startOffset, int endOffset) {
        VirtualFormattingInlaysInfo instance = getInstance();
        if (instance.isVirtualFormattingEnabled(editor)) {
            return 0;
        }
        return instance.getVisualFormattingInlineInlays(editor, startOffset, endOffset)
            .stream()
            .mapToInt(inlay -> inlay.getRenderer().calcWidthInPixels(inlay))
            .sum();
    }

    /**
     * @return true if virtual formatting is enabled for this editor
     */
    boolean isVirtualFormattingEnabled(Editor editor);

    /**
     * @return list of inline inlays representing virtual formatting between the given offsets
     */
    List<Inlay<?>> getVisualFormattingInlineInlays(Editor editor, int startOffset, int endOffset);
}

