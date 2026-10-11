/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.ui.TreeNodePresentation;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.TextItemPresentation;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.util.TextAttributeUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class ArtifactTreePresentations {
    private ArtifactTreePresentations() {
    }

    public static PresentationData createPresentationData(TreeNodePresentation presentation) {
        PresentationData data = new PresentationData();
        presentation.render(data, SimpleTextAttributes.REGULAR_ATTRIBUTES, SimpleTextAttributes.GRAY_ATTRIBUTES);
        return data;
    }

    public static void render(PresentationData data, String tooltip, boolean waved, TextItemPresentation presentation) {
        presentation.withIcon(data.getIcon());

        List<PresentableNodeDescriptor.ColoredFragment> fragments = data.getColoredText();
        if (fragments.isEmpty()) {
            presentation.append(data.getPresentableText(), toAttribute(null, waved));
        }
        else {
            for (PresentableNodeDescriptor.ColoredFragment fragment : fragments) {
                presentation.append(fragment.getText(), toAttribute(fragment.getAttributes(), waved));
            }
        }

        if (!tooltip.isEmpty()) {
            presentation.withTooltip(LocalizeValue.of(tooltip));
        }
    }

    private static TextAttribute toAttribute(@Nullable SimpleTextAttributes attributes, boolean waved) {
        TextAttribute attribute = TextAttributeUtil.toTextAttribute(attributes);
        return waved ? attribute.withEffect(TextEffect.WAVED) : attribute;
    }
}
