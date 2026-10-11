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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath;

import consulo.localize.LocalizeValue;
import consulo.ui.TextItemPresentation;
import consulo.ui.ex.UnifiedColoredTextContainer;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
record ClasspathTableItemPresentation(UnifiedColoredTextContainer text, LocalizeValue tooltip) {
    void render(TextItemPresentation presentation) {
        text.appendTo(presentation);
        if (tooltip.isNotEmpty()) {
            presentation.withTooltip(tooltip);
        }
    }

    String getSearchText() {
        return text.toString();
    }
}
