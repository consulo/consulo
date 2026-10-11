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
import consulo.ui.CheckBox;
import consulo.ui.CheckBoxStyle;
import consulo.ui.internal.SearchHighlightUtil;
import org.jspecify.annotations.Nullable;

/**
 * Dummy-but-creatable headless {@link CheckBox}.
 *
 * @author VISTALL
 */
public class HeadlessCheckBox extends HeadlessBooleanValueComponentBase implements CheckBox {
    private @Nullable String mySearchHighlight;

    private LocalizeValue myLabelText = LocalizeValue.empty();

    public HeadlessCheckBox() {
        super(false);
    }

    @Override
    public LocalizeValue getLabelText() {
        return myLabelText;
    }

    @Override
    public void setLabelText(LocalizeValue labelText) {
        myLabelText = labelText;
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
    public void addStyle(CheckBoxStyle style) {
    }

    @Override
    public void setSearchHighlight(@Nullable String query) {
        mySearchHighlight = SearchHighlightUtil.normalizeQuery(query);
    }

    @Override
    public @Nullable String getSearchHighlight() {
        return mySearchHighlight;
    }
}
