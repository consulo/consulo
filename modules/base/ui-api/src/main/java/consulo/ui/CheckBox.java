/*
 * Copyright 2013-2016 consulo.io
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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2016-06-09
 */
public interface CheckBox extends ValueComponent<Boolean>, HasFocus, HasMnemonic, HasComponentStyle<CheckBoxStyle> {
    @Deprecated(forRemoval = true)
    @DeprecationInfo("Please don't use not localized text")
    @RequiredUIAccess
    static CheckBox create(String label) {
        return create(LocalizeValue.of(label));
    }

    @Deprecated(forRemoval = true)
    @DeprecationInfo("Please don't use not localized text")
    @RequiredUIAccess
    static CheckBox create(String label, boolean selected) {
        return create(LocalizeValue.of(label), selected);
    }

    @RequiredUIAccess
    static CheckBox create(LocalizeValue label) {
        return create(label, false);
    }

    @RequiredUIAccess
    static CheckBox create(LocalizeValue label, boolean selected) {
        CheckBox box = UIInternal.get()._Components_checkBox();
        box.setLabelText(label);
        box.setValue(selected);
        return box;
    }

    @Override
    Boolean getValue();

    @Override
    @RequiredUIAccess
    default void setValue(@Nullable Boolean value) {
        setValue(value, true);
    }

    @RequiredUIAccess
    void setValue(@Nullable Boolean value, boolean fireListeners);

    LocalizeValue getLabelText();

    @RequiredUIAccess
    void setLabelText(LocalizeValue labelText);
}
