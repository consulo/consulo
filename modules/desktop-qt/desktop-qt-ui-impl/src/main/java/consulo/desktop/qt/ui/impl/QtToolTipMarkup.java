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
package consulo.desktop.qt.ui.impl;

import consulo.application.util.HtmlChunk;
import consulo.ui.ToolTip;
import consulo.ui.impl.ToolTipMarkupBuilder;
import consulo.ui.internal.ToolTipImpl;
import io.qt.gui.QPalette;
import io.qt.widgets.QApplication;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
final class QtToolTipMarkup extends ToolTipMarkupBuilder {
    private final String mySecondaryColor;

    private QtToolTipMarkup(String secondaryColor) {
        mySecondaryColor = secondaryColor;
    }

    static @Nullable String toText(@Nullable ToolTip toolTip) {
        ToolTipImpl impl = (ToolTipImpl) toolTip;
        if (impl == null || impl.isEmpty()) {
            return null;
        }

        if (isPlain(impl)) {
            return impl.getTitle().getNullIfEmpty();
        }

        String secondaryColor = QApplication.palette().color(QPalette.ColorRole.PlaceholderText).name();
        return new QtToolTipMarkup(secondaryColor).build(impl);
    }

    @Override
    protected HtmlChunk.Element root() {
        return HtmlChunk.div();
    }

    @Override
    protected HtmlChunk.Element header() {
        return HtmlChunk.div();
    }

    @Override
    protected HtmlChunk.Element shortcut(HtmlChunk.Element element) {
        return element.style("color:" + mySecondaryColor);
    }

    @Override
    protected HtmlChunk.Element paragraph(boolean underTitle) {
        return HtmlChunk.div().style(underTitle ? "margin-top:4px; color:" + mySecondaryColor : "margin-top:4px");
    }
}
