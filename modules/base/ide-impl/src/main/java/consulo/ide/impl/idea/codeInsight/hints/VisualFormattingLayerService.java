/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.application.ApplicationManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.Inlay;
import consulo.language.codeStyle.CodeStyleSettings;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for applying and querying visual formatting layer elements and foldings.
 */
public abstract class VisualFormattingLayerService {
    /**
     * Should zombie fold regions be removed?
     */
    private static final String REMOVE_ZOMBIE_FOLDINGS_REGISTRY_KEY = "editor.readerMode.vfmt.removeZombies";

    /**
     * @return true if the registry flag to remove zombie foldings is set
     */
    public static boolean shouldRemoveZombieFoldings() {
        return Registry.is(REMOVE_ZOMBIE_FOLDINGS_REGISTRY_KEY);
    }

    /**
     * @return the singleton service instance
     */
    public static VisualFormattingLayerService getInstance() {
        return ApplicationManager.getApplication().getInstance(VisualFormattingLayerService.class);
    }

    private static CodeStyleSettings getCodeStyleSettings(Editor editor) {
        return editor.getUserData(VirtualFormattingInlaysInfo.EDITOR_VISUAL_FORMATTING_LAYER_CODE_STYLE_SETTINGS);
    }

    /**
     * @return true if visual formatting is enabled for the given editor
     */
    public static boolean isEnabledForEditor(Editor editor) {
        return getCodeStyleSettings(editor) != null;
    }

    /**
     * Enables visual formatting for this editor using the given code style settings.
     */
    public static void enableForEditor(Editor editor, CodeStyleSettings codeStyleSettings) {
        editor.putUserData(VirtualFormattingInlaysInfo.EDITOR_VISUAL_FORMATTING_LAYER_CODE_STYLE_SETTINGS, codeStyleSettings);
    }

    /**
     * Disables visual formatting for this editor.
     */
    public static void disableForEditor(Editor editor) {
        editor.putUserData(VirtualFormattingInlaysInfo.EDITOR_VISUAL_FORMATTING_LAYER_CODE_STYLE_SETTINGS, null);
    }

    /**
     * Retrieves platform virtual formatting inline inlays in the given range.
     */
    @SuppressWarnings("unchecked")
    public static List<Inlay<?>> getVisualFormattingInlineInlays(Editor editor, int startOffset, int endOffset) {
        return editor.getInlayModel()
            .getInlineElementsInRange(startOffset, endOffset)
            .stream()
            .filter(inlay -> {
                EditorCustomElementRenderer r = inlay.getRenderer();
                return r instanceof VInlayPresentation && !((VInlayPresentation) r).vertical;
            })
            .collect(Collectors.toList());
    }

    /**
     * Collects elements to be rendered for visual formatting (inline inlays, block inlays, fold regions).
     */
    public abstract List<VisualFormattingLayerElement> collectVisualFormattingLayerElements(Editor editor);

    /**
     * Applies a list of visual formatting layer elements to the editor.
     */
    public abstract void applyVisualFormattingLayerElementsToEditor(Editor editor,
                                                                    List<VisualFormattingLayerElement> elements);
}
