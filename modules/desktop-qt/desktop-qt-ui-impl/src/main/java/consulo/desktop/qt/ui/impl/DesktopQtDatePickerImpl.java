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
package consulo.desktop.qt.ui.impl;

import consulo.ui.DatePicker;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import io.qt.core.QDate;
import io.qt.widgets.QDateEdit;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtDatePickerImpl extends QtComponentDelegate<QDateEdit> implements DatePicker {
    private final @Nullable String myDatePattern;

    private @Nullable Date myValue;

    private boolean myFireListeners = true;

    public DesktopQtDatePickerImpl(@Nullable String datePattern) {
        myDatePattern = datePattern;
    }

    @Override
    protected QDateEdit createQt(QWidget parent) {
        return new QDateEdit(parent);
    }

    @Override
    protected void initialize(QDateEdit component) {
        super.initialize(component);

        component.setCalendarPopup(true);

        if (myDatePattern != null) {
            component.setDisplayFormat(myDatePattern);
        }

        if (myValue != null) {
            component.setDate(toQDate(myValue));
        }

        component.dateChanged.connect(date -> {
            myValue = fromQDate(date);

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, myValue, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    @Override
    public @Nullable Date getValue() {
        return myValue;
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable Date value, boolean fireListeners) {
        myValue = value;

        if (myComponent != null && value != null) {
            myFireListeners = fireListeners;
            try {
                myComponent.setDate(toQDate(value));
            }
            finally {
                myFireListeners = true;
            }
        }
    }

    private static QDate toQDate(Date date) {
        LocalDate localDate = Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        return new QDate(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
    }

    private static Date fromQDate(QDate date) {
        LocalDate localDate = LocalDate.of(date.year(), date.month(), date.day());
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
