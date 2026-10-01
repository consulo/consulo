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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.dom.Element;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
final class WebPopupFocus {
    static final String CONTENT_ATTRIBUTE = "consulo-popup-content";

    private WebPopupFocus() {
    }

    static void markContent(Element content) {
        content.setAttribute(CONTENT_ATTRIBUTE, true);
    }

    static void forwardToContent(Element host, String contentSelector) {
        host.executeJs(
            """
            this.addEventListener('focus', event => {
                if (event.target !== this) {
                    return;
                }

                const content = this.querySelector($0);
                if (content && typeof content.focus === 'function') {
                    content.focus();
                }
            });
            """,
            contentSelector
        );
    }
}
