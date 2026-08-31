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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.datepicker.DatePicker;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class WebDatePickerImpl extends VaadinComponentDelegate<WebDatePickerImpl.Vaadin> implements consulo.ui.DatePicker {
    public class Vaadin extends DatePicker implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebDatePickerImpl.this;
        }
    }

    @SuppressWarnings("unchecked")
    public WebDatePickerImpl(@Nullable String datePattern) {
        Vaadin component = getVaadinComponent();

        if (datePattern != null) {
            component.setI18n(new DatePicker.DatePickerI18n().setDateFormat(datePattern));
        }

        component.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent<>(this, toDate(event.getValue())));
            }
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public @Nullable Date getValue() {
        return toDate(getVaadinComponent().getValue());
    }

    @RequiredUIAccess
    @Override
    @SuppressWarnings("unchecked")
    public void setValue(@Nullable Date value, boolean fireListeners) {
        UIAccess.assertIsUIThread();

        getVaadinComponent().setValue(toLocalDate(value));

        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, value));
        }
    }

    private static @Nullable Date toDate(@Nullable LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static @Nullable LocalDate toLocalDate(@Nullable Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
