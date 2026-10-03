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
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.font.Font;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessTextArea extends HeadlessValueComponentBase<String> implements TextArea {
    private boolean myEditable = true;
    private boolean myFocusable = true;
    private LocalizeValue myPlaceholder = LocalizeValue.empty();
    private @Nullable Font myFont;
    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    public HeadlessTextArea(@Nullable String text) {
        super(StringUtil.notNullize(text));
    }

    @Override
    protected String normalize(@Nullable String value) {
        return StringUtil.notNullize(value);
    }

    @Override
    public String getValue() {
        return normalize(super.getValue());
    }

    @Override
    @RequiredUIAccess
    public void setEditable(boolean editable) {
        myEditable = editable;
    }

    @Override
    public boolean isEditable() {
        return myEditable;
    }

    @Override
    @RequiredUIAccess
    public void selectAll() {
    }

    @Override
    @RequiredUIAccess
    public void setFont(@Nullable Font font) {
        myFont = font;
    }

    public @Nullable Font getFont() {
        return myFont;
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text;
    }

    public LocalizeValue getPlaceholder() {
        return myPlaceholder;
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        myPrefixComponent = prefixComponent;
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
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
        myFocusable = focusable;
    }

    @Override
    public boolean isFocusable() {
        return myFocusable;
    }
}
