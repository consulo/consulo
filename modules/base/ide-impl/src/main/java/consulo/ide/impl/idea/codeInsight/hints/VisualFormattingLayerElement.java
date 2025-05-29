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

import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldingModelEx;

/**
 * Represents an element in the visual formatting layer (inline inlay, block inlay, or folding).
 */
public abstract class VisualFormattingLayerElement {
    /**
     * Applies this element to the given editor.
     */
    public abstract void applyToEditor(Editor editor);

    public static final class InlineInlay extends VisualFormattingLayerElement {
        private final int offset;
        private final int length;

        public InlineInlay(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public void applyToEditor(Editor editor) {
            editor.getInlayModel()
                .addInlineElement(offset, false, Integer.MAX_VALUE,
                    new VInlayPresentation(editor, length, false));
        }
    }

    public static final class BlockInlay extends VisualFormattingLayerElement {
        private final int offset;
        private final int lines;

        public BlockInlay(int offset, int lines) {
            this.offset = offset;
            this.lines = lines;
        }

        @Override
        public void applyToEditor(Editor editor) {
            editor.getInlayModel()
                .addBlockElement(offset, true, true, 0,
                    new VInlayPresentation(editor, lines, true));
        }
    }

    public static final class Folding extends VisualFormattingLayerElement {
        private final int offset;
        private final int length;

        public Folding(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public void applyToEditor(Editor editor) {
            FoldingModelEx foldingModel = (FoldingModelEx) editor.getFoldingModel();
            var region = foldingModel.createFoldRegion(offset, offset + length, "", null, true);
            if (region != null) {
                region.putUserData(VirtualFormattingInlaysInfo.VISUAL_FORMATTING_ELEMENT_KEY, Boolean.TRUE);
            }
        }
    }
}
