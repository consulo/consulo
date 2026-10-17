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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * A short message with an icon, a title, a text and the actions it offers. It is the same component inside a layout,
 * as a banner above the content it is about, and as a notification shown over the window.
 * <p/>
 * Actions stand as links under the text. The toolbar component sits in the top right corner, level with the title -
 * a toolbar of actions such as the less used options and close.
 * <p/>
 * When more than one of {@link ToastStyle#INFO}, {@link ToastStyle#SUCCESS}, {@link ToastStyle#WARNING} and
 * {@link ToastStyle#ERROR} is added, the last one is used. {@link ToastStyle#GLASSY} goes together with any of them.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
public interface Toast extends Component, HasComponentStyle<ToastStyle> {
    static Toast create() {
        return UIInternal.get()._Components_toast();
    }

    @RequiredUIAccess
    Toast setTitle(LocalizeValue title);

    @RequiredUIAccess
    Toast setText(LocalizeValue text);

    /**
     * Text with HTML markup in place of the plain text. A click on a link of it is told to the hyperlink listeners,
     * {@link HyperlinkEvent#getDescription()} being the {@code href} of the link.
     */
    @RequiredUIAccess
    Toast setHtmlText(LocalizeValue html);

    default Disposable addHyperlinkListener(ComponentEventListener<Component, HyperlinkEvent> hyperlinkListener) {
        return addListener(HyperlinkEvent.class, hyperlinkListener);
    }

    /**
     * Replaces the icon of the style. {@code null} brings the icon of the style back.
     */
    @RequiredUIAccess
    Toast setIcon(@Nullable Image icon);

    @RequiredUIAccess
    Toast addAction(LocalizeValue text, @RequiredUIAccess Runnable action);

    @RequiredUIAccess
    Toast setToolbarComponent(@Nullable Component toolbar);
}
