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

import consulo.language.editor.inlay.HintFormat;
import consulo.language.editor.inlay.DeclarativeInlayPayload;
import consulo.language.editor.inlay.DeclarativeInlayPosition;

import java.util.List;

public final class InlayData {
    private final DeclarativeInlayPosition position;
    private final String tooltip;
    private final HintFormat hintFormat;
    private final TinyTree<Object> tree;
    private final String providerId;
    private final boolean disabled;
    private final List<DeclarativeInlayPayload> payloads;
    private final Class<?> providerClass;
    private final String sourceId;

    public InlayData(DeclarativeInlayPosition position,
                     String tooltip,
                     HintFormat hintFormat,
                     TinyTree<Object> tree,
                     String providerId,
                     boolean disabled,
                     List<DeclarativeInlayPayload> payloads,
                     Class<?> providerClass,
                     String sourceId) {
        this.position = position;
        this.tooltip = tooltip;
        this.hintFormat = hintFormat;
        this.tree = tree;
        this.providerId = providerId;
        this.disabled = disabled;
        this.payloads = payloads;
        this.providerClass = providerClass;
        this.sourceId = sourceId;
    }

    public DeclarativeInlayPosition getPosition() {
        return position;
    }

    public String getTooltip() {
        return tooltip;
    }

    public HintFormat getHintFormat() {
        return hintFormat;
    }

    public TinyTree<Object> getTree() {
        return tree;
    }

    public String getProviderId() {
        return providerId;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public List<DeclarativeInlayPayload> getPayloads() {
        return payloads;
    }

    public Class<?> getProviderClass() {
        return providerClass;
    }

    public String getSourceId() {
        return sourceId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<# ");
        buildStringFromTextNodes((byte) 0, sb);
        sb.append(" #>");
        return sb.toString();
    }

    private void buildStringFromTextNodes(byte index, StringBuilder builder) {
        tree.processChildren(index, i -> {
            byte tag = tree.getBytePayload(i);
            if (tag == InlayTags.TEXT_TAG) {
                Object data = tree.getDataPayload(i);
                if (data instanceof String) {
                    builder.append(data);
                }
                else if (data instanceof ActionWithContent) {
                    builder.append(((ActionWithContent) data).getContent());
                }
                else {
                    builder.append("%error: unexpected data in text node%");
                }
            }
            else if (tag == InlayTags.COLLAPSIBLE_LIST_COLLAPSED_BRANCH_TAG) {
                // do nothing
            }
            else {
                buildStringFromTextNodes((byte) i, builder);
            }
            return true;
        });
    }
}