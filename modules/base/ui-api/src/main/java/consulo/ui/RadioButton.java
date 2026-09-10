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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * A label the user can choose, drawn the way the platform draws one of a set of choices.
 * <p/>
 * A button made by {@link #create} is on its own, and stays that way: choosing it does not unchoose anything, and
 * nothing keeps track of which of several is chosen. All of that is the caller's to do - it is only worth making
 * one directly where that is what is wanted, such as a single option a plugin contributes to a dialog which
 * arranges the rest itself.
 * <p/>
 * <b>For a set of choices, use a {@link RadioGroup}.</b> Its {@link RadioGroup#newButton} hands back buttons which
 * unchoose one another, and the group is read and written as the type the choice actually means, rather than as
 * one boolean per option which the caller has to tell apart. The boolean here is then only the drawn state, and
 * setting it is the group's business.
 *
 * @author VISTALL
 * @since 2016-06-14
 */
public interface RadioButton extends ValueComponent<Boolean>, HasFocus {
    /**
     * A button which belongs to nothing. Whether it is chosen, and what that means, is the caller's to keep - see
     * {@link RadioGroup} for the case where the platform should keep it instead.
     */
    static RadioButton create(LocalizeValue textValue) {
        return create(textValue, false);
    }

    static RadioButton create(LocalizeValue textValue, boolean selected) {
        return UIInternal.get()._Components_radioButton(textValue, selected);
    }

    @Override
    Boolean getValue();

    @Override
    @RequiredUIAccess
    default void setValue(@Nullable Boolean value) {
        setValue(value, true);
    }

    @Override
    @RequiredUIAccess
    void setValue(@Nullable Boolean value, boolean fireListeners);

    LocalizeValue getLabelText();

    @RequiredUIAccess
    void setLabelText(LocalizeValue text);
}
