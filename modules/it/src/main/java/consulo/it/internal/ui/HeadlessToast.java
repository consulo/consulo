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
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.image.Image;
import consulo.ui.internal.ToastInternal;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Headless {@link Toast}: keeps what it is given, and runs an action by its text the way a click on its link would.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
public class HeadlessToast extends HeadlessComponentBase implements ToastInternal {
    private @Nullable ToastStyle myStyle;
    private @Nullable Image myIcon;
    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue myText = LocalizeValue.empty();
    private final Map<String, Runnable> myActions = new LinkedHashMap<>();
    private @Nullable Component myToolbar;
    private boolean myShadow;

    @Override
    public void addStyle(ToastStyle style) {
        myStyle = style;
    }

    public @Nullable ToastStyle getStyle() {
        return myStyle;
    }

    @Override
    public Toast setTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    @Override
    public Toast setText(LocalizeValue text) {
        myText = text;
        return this;
    }

    public LocalizeValue getText() {
        return myText;
    }

    @Override
    public Toast setHtmlText(LocalizeValue html) {
        myText = html;
        return this;
    }

    @Override
    public Toast setIcon(@Nullable Image icon) {
        myIcon = icon;
        return this;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public Toast addAction(LocalizeValue text, Runnable action) {
        myActions.put(text.get(), action);
        return this;
    }

    public void runAction(String text) {
        Runnable action = myActions.get(text);
        if (action == null) {
            throw new IllegalArgumentException("No action '" + text + "', there are " + myActions.keySet());
        }
        action.run();
    }

    @Override
    public Toast setToolbarComponent(@Nullable Component toolbar) {
        myToolbar = toolbar;
        return this;
    }

    public @Nullable Component getToolbarComponent() {
        return myToolbar;
    }

    @Override
    public void setShadow(boolean shadow) {
        myShadow = shadow;
    }

    public boolean isShadow() {
        return myShadow;
    }
}
