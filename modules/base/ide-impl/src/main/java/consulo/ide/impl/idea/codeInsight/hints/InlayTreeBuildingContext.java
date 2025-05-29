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

import consulo.language.editor.inlay.InlayPosition;

class InlayTreeBuildingContext {
    final TinyTree<Object> tree;
    private final InlayPosition position;
    int depth = 0;
    private int nodeCount = 1;
    private boolean limitReached = false;
    private int textElementCount = 0;

    InlayTreeBuildingContext(InlayPosition position) {
        this.position = position;
        this.tree = new TinyTree<>(InlayTags.LIST_TAG, null);
    }

    byte addNode(byte parent, byte nodePayload, Object data) {
        if (limitReached) {
            return PresentationTreeBuilderImpl.DOESNT_FIT_INDEX;
        }
        if (nodeCount == PresentationTreeBuilderImpl.MAX_NODE_COUNT - 1) {
            limitReached = true;
            tree.add(parent, InlayTags.TEXT_TAG, "…");
            return PresentationTreeBuilderImpl.DOESNT_FIT_INDEX;
        }
        nodeCount++;
        return tree.add(parent, nodePayload, data);
    }

    boolean isTruncateTextNodes() {
        return position instanceof InlayPosition.InlineInlayPosition;
    }

    void incrementTextElementCount() {
        textElementCount++;
    }

    int getTextElementCount() {
        return textElementCount;
    }

    TinyTree<Object> getTree() {
        return tree;
    }
}
