/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.internal.UIInternal;
import jakarta.annotation.Nonnull;

import java.net.URL;

/**
 * @author VISTALL
 * @since 24/11/2021
 * <p>
 * Simple html view. HTML5, CSS3, JS not supported. WebView can be used as replacement, if need more powerful html view
 */
public interface HtmlView extends Component {
    record RenderData(@Nonnull String html, @Nonnull String inlineCss, @Nonnull URL[] externalCsses) {
        public RenderData(@Nonnull String html) {
            this(html, "", new URL[0]);
        }
    }

    @Nonnull
    static HtmlView create() {
        return UIInternal.get()._Components_htmlView();
    }

    void render(@Nonnull RenderData renderData);

    void scrollToMarkdownSrcOffset(final int offset);

    @Nonnull
    default Disposable addHyperlinkListener(@Nonnull ComponentEventListener<Component, HyperlinkEvent> hyperlinkListener) {
        return addListener(HyperlinkEvent.class, hyperlinkListener);
    }
}
