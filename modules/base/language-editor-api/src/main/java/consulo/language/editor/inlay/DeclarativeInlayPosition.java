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
package consulo.language.editor.inlay;

/**
 * @author VISTALL
 * @since 2025-05-27
 */
public sealed interface DeclarativeInlayPosition {
    public final class InlineInlayPosition implements DeclarativeInlayPosition {
        private final int offset;
        private final boolean relatedToPrevious;
        private final int priority;

        public InlineInlayPosition(int offset, boolean relatedToPrevious) {
            this(offset, relatedToPrevious, 0);
        }

        public InlineInlayPosition(int offset, boolean relatedToPrevious, int priority) {
            this.offset = offset;
            this.relatedToPrevious = relatedToPrevious;
            this.priority = priority;
        }

        public int getOffset() {
            return offset;
        }

        public boolean isRelatedToPrevious() {
            return relatedToPrevious;
        }

        public int getPriority() {
            return priority;
        }
    }

    public final class EndOfLinePosition implements DeclarativeInlayPosition {
        private final int line;
        private final int priority;

        public EndOfLinePosition(int line) {
            this(line, 0);
        }

        public EndOfLinePosition(int line, int priority) {
            this.line = line;
            this.priority = priority;
        }

        public int getLine() {
            return line;
        }

        public int getPriority() {
            return priority;
        }
    }

    /**
     * Positions an inlay hint above the line that contains the given offset.
     *
     * @param offset           document offset whose line will host the inlay
     * @param verticalPriority hints with higher verticalPriority will be placed closer to the line
     *                         given by offset; hints from the same provider with the same verticalPriority
     *                         will be placed on the same line
     * @param priority         within a single line, hints are sorted by priority in descending order
     */
    public final class AboveLineIndentedPosition implements DeclarativeInlayPosition {
        private final int offset;
        private final int verticalPriority;
        private final int priority;

        public AboveLineIndentedPosition(int offset) {
            this(offset, 0, 0);
        }

        public AboveLineIndentedPosition(int offset, int verticalPriority) {
            this(offset, verticalPriority, 0);
        }

        public AboveLineIndentedPosition(int offset, int verticalPriority, int priority) {
            this.offset = offset;
            this.verticalPriority = verticalPriority;
            this.priority = priority;
        }

        public int getOffset() {
            return offset;
        }

        public int getVerticalPriority() {
            return verticalPriority;
        }

        public int getPriority() {
            return priority;
        }
    }
}
