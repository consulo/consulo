/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.DatePicker;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXDatePicker;

import java.util.Date;

/**
 * @author VISTALL
 * @since 2024-12-13
 */
public class DesktopDatePickerImpl extends SwingComponentDelegate<DesktopDatePickerImpl.DatePickerImpl> implements DatePicker {
    public class DatePickerImpl extends JXDatePicker implements FromSwingComponentWrapper {
        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopDatePickerImpl.this;
        }
    }

    public DesktopDatePickerImpl(String datePattern) {
        DatePickerImpl picker = new DatePickerImpl();
        initialize(picker);

        if (datePattern != null) {
            picker.setFormats(datePattern);
        }
    }

    @Nullable
    @Override
    public Date getValue() {
        return toAWTComponent().getDate();
    }

    @RequiredUIAccess
    @Override
    public void setValue(Date value, boolean fireListeners) {
        toAWTComponent().setDate(value);
    }
}
