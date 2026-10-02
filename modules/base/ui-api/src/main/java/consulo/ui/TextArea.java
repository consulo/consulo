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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.font.Font;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public interface TextArea extends ValueComponent<String>, HasFocus, HasPlaceholder, HasPrefixComponent, HasSuffixComponent {
    @RequiredUIAccess
    static TextArea create() {
        return create(null);
    }

    @RequiredUIAccess
    static TextArea create(@Nullable String text) {
        return UIInternal.get()._Components_textArea(text == null ? "" : text);
    }

    @RequiredUIAccess
    default TextArea withPlaceholder(LocalizeValue text) {
        setPlaceholder(text);
        return this;
    }

    @RequiredUIAccess
    void setEditable(boolean editable);

    boolean isEditable();

    @RequiredUIAccess
    default TextArea withEditable(boolean editable) {
        setEditable(editable);
        return this;
    }

    @RequiredUIAccess
    void selectAll();

    @RequiredUIAccess
    default void replaceSelection(String text) {
        String value = getValue();
        setValue(value == null ? text : value + text);
    }

    @RequiredUIAccess
    void setFont(@Nullable Font font);

    @RequiredUIAccess
    default void setVisibleLength(int columns) {
    }

    @RequiredUIAccess
    default void setMinRows(int rows) {
    }

    @RequiredUIAccess
    default void setMaxRows(int rows) {
    }

    @Override
    default void setSuffixComponent(@Nullable Component suffixComponent) {
    }

    @Override
    default @Nullable Component getSuffixComponent() {
        return null;
    }
}
