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
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Dummy-but-creatable headless {@link Button}. {@link #invoke} dispatches the click to the registered listeners the
 * way the rendering backends do, so a test can drive a button without a frontend.
 *
 * @author VISTALL
 */
public class HeadlessButton extends HeadlessComponentBase implements Button {
    private LocalizeValue myText;
    private @Nullable Image myImage;

    public HeadlessButton(LocalizeValue text) {
        myText = text;
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
    public @Nullable Image getIcon() {
        return myImage;
    }

    @Override
    public void setIcon(@Nullable Image image) {
        myImage = image;
    }

    @Override
    public void invoke(InputDetails inputDetails) {
        getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, inputDetails));
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public void focus() {
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void addStyle(ButtonStyle style) {
    }
}
