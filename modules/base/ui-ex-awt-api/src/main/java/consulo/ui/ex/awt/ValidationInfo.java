/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Describes why the data entered in a DialogWrapper is invalid.
 *
 * @author Konstantin Bulenkov
 * @see DialogWrapper#doValidate()
 */
public final class ValidationInfo {
    @Nonnull
    public final LocalizeValue message;

    @Nullable
    public final JComponent component;

    public boolean okEnabled;
    public boolean warning;

    /**
     * Creates a validation error message associated with a specific component. The component will have an error icon drawn next to it,
     * and will be focused when the user tries to close the dialog by pressing OK.
     *
     * @param message   the error message to display.
     * @param component the component containing the invalid data.
     */
    public ValidationInfo(@Nonnull LocalizeValue message, @Nullable JComponent component) {
        this.message = message;
        this.component = component;
    }

    /**
     * Creates a validation error message associated with a specific component. The component will have an error icon drawn next to it,
     * and will be focused when the user tries to close the dialog by pressing OK.
     *
     * @param message   the error message to display.
     * @param component the component containing the invalid data.
     */
    @Deprecated
    @DeprecationInfo("Use variant with LocalizaValue")
    public ValidationInfo(@Nonnull String message, @Nullable JComponent component) {
        this(LocalizeValue.of(message), component);
    }

    /**
     * Creates a validation error message not associated with a specific component.
     *
     * @param message the error message to display.
     */
    public ValidationInfo(@Nonnull LocalizeValue message) {
        this(message, null);
    }

    /**
     * Creates a validation error message not associated with a specific component.
     *
     * @param message the error message to display.
     */
    @Deprecated
    @DeprecationInfo("Use variant with LocalizaValue")
    public ValidationInfo(@Nonnull String message) {
        this(message, null);
    }

    public ValidationInfo withOKEnabled() {
        okEnabled = true;
        return this;
    }

    public ValidationInfo asWarning() {
        warning = true;
        return this;
    }

    public ValidationInfo forComponent(@Nullable JComponent component) {
        ValidationInfo result = new ValidationInfo(message, component);
        result.warning = warning;
        result.okEnabled = okEnabled;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || o instanceof ValidationInfo that
            && Objects.equals(this.message, that.message)
            && this.component == that.component
            && this.okEnabled == that.okEnabled
            && this.warning == that.warning;
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, component, okEnabled, warning);
    }
}
