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
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Simple HTML view. HTML5, CSS3, JS not supported. WebView can be used as replacement, if you need more powerful HTML view.
 *
 * @author VISTALL
 * @since 2021-11-24
 */
public interface HtmlView extends Component {
    /**
     * Marks the {@code src} of an {@code img} as an id of an image of the platform rather than as something to
     * fetch. A relative path on purpose - it stays a valid url whichever renderer parses the document, so no
     * frontend has to rewrite the markup before it hands it over.
     *
     * @see #setImageResolver(Function)
     */
    String IMAGE_SRC_PREFIX = "consulo-image/";

    record RenderData(String html, String inlineCss, URL[] externalCsses) {
        public RenderData(String html) {
            this(html, "", new URL[0]);
        }
    }

    static HtmlView create() {
        return UIInternal.get()._Components_htmlView();
    }

    CompletableFuture<?> render(RenderData renderData);

    /**
     * Resolves an id of an {@code img} under {@link #IMAGE_SRC_PREFIX} to an image of the platform. A document
     * names an image by an id of its own - a plugin id, a name of an icon - rather than by something which
     * could be fetched, and only whoever built the html knows what those stand for.
     * <p>
     * An id nobody resolves draws nothing, so a document may name an image which this build does not have.
     */
    default void setImageResolver(@Nullable Function<String, Image> imageResolver) {
    }

    /**
     * Use only of result of processing {@link #render(RenderData)}
     */
    void scrollToMarkdownSrcOffset(int offset);

    default Disposable addHyperlinkListener(ComponentEventListener<Component, HyperlinkEvent> hyperlinkListener) {
        return addListener(HyperlinkEvent.class, hyperlinkListener);
    }
}
