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
package consulo.web.ui.impl.internal.base;

import com.vaadin.flow.component.shared.Tooltip;
import consulo.application.util.HtmlChunk;
import consulo.ui.ToolTip;
import consulo.ui.impl.ToolTipMarkupBuilder;
import consulo.ui.internal.ToolTipImpl;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
final class WebToolTipMarkup extends ToolTipMarkupBuilder {
    private static final WebToolTipMarkup INSTANCE = new WebToolTipMarkup();

    private WebToolTipMarkup() {
    }

    static void apply(com.vaadin.flow.component.Component component, @Nullable ToolTip toolTip) {
        Tooltip tooltip = Tooltip.forComponent(component);

        ToolTipImpl impl = (ToolTipImpl) toolTip;
        if (impl == null || impl.isEmpty()) {
            tooltip.setText(null);
            return;
        }

        if (isPlain(impl)) {
            tooltip.setText(impl.getTitle().get());
            return;
        }

        tooltip.setHideDelay(100);
        tooltip.setMarkdown(INSTANCE.build(impl));
    }

    @Override
    protected HtmlChunk.Element root() {
        return HtmlChunk.div().setClass("consulo-tooltip");
    }

    @Override
    protected HtmlChunk.Element header() {
        return HtmlChunk.div().setClass("consulo-tooltip-header");
    }

    @Override
    protected HtmlChunk.Element shortcut(HtmlChunk.Element element) {
        return element.setClass("consulo-tooltip-shortcut");
    }

    @Override
    protected HtmlChunk.Element paragraph(boolean underTitle) {
        return HtmlChunk.div().setClass(underTitle ? "consulo-tooltip-info" : "consulo-tooltip-description");
    }
}
