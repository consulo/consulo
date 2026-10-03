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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Hyperlink;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
public class HeadlessHyperlink extends HeadlessComponentBase implements Hyperlink {
    private LocalizeValue myText;
    private @Nullable Image myIcon;

    public HeadlessHyperlink(LocalizeValue text) {
        myText = text;
    }

    public void click() {
        getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, ""));
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @Override
    public void setText(LocalizeValue text) {
        myText = text;
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }
}
