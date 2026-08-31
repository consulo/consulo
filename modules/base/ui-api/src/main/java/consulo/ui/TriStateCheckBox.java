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
import consulo.ui.internal.UIInternal;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

/**
 * A check box with three states, mirroring {@link CheckBox} but holding {@link ThreeState} instead of {@link Boolean}.
 * <p>
 * States: {@link ThreeState#YES} — checked, {@link ThreeState#NO} — unchecked,
 * {@link ThreeState#UNSURE} — indeterminate (partially checked).
 * <p>
 * Clicking cycles through all three states. When the third state is disabled via
 * {@link #setUnsureEnabled(boolean)}, user interaction toggles only between
 * {@code YES} and {@code NO}, while {@code UNSURE} can still be set programmatically.
 *
 * @author VISTALL
 * @since 2026-08-23
 */
public interface TriStateCheckBox extends ValueComponent<ThreeState>, HasFocus, HasMnemonic, HasComponentStyle<CheckBoxStyle> {
    @RequiredUIAccess
    static TriStateCheckBox create(LocalizeValue label) {
        return create(label, ThreeState.UNSURE);
    }

    @RequiredUIAccess
    static TriStateCheckBox create(LocalizeValue label, ThreeState selected) {
        TriStateCheckBox box = UIInternal.get()._Components_triStateCheckBox();
        box.setLabelText(label);
        box.setValue(selected);
        return box;
    }

    @Override
    ThreeState getValue();

    @Override
    @RequiredUIAccess
    default void setValue(@Nullable ThreeState value) {
        setValue(value, true);
    }

    @RequiredUIAccess
    void setValue(@Nullable ThreeState value, boolean fireListeners);

    boolean isUnsureEnabled();

    @RequiredUIAccess
    void setUnsureEnabled(boolean unsureEnabled);

    LocalizeValue getLabelText();

    @RequiredUIAccess
    void setLabelText(LocalizeValue labelText);
}
