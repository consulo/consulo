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
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Dummy-but-creatable headless {@link Label}.
 *
 * @author VISTALL
 */
public class HeadlessLabel extends HeadlessComponentBase implements Label {
    private LocalizeValue myText;
    private @Nullable Image myImage;
    private @Nullable Component myTarget;

    public HeadlessLabel(LocalizeValue text) {
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
    public void setImage(@Nullable Image icon) {
        myImage = icon;
    }

    @Override
    public @Nullable Image getImage() {
        return myImage;
    }

    @Override
    public void setTarget(@Nullable Component component) {
        myTarget = component;
    }

    @Override
    public @Nullable Component getTarget() {
        return myTarget;
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
    public void addStyle(LabelStyle style) {
    }
}
