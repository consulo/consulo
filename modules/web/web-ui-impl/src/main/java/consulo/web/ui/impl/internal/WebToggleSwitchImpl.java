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

import com.vaadin.componentfactory.ToggleButton;
import consulo.ui.Component;
import consulo.ui.ToggleSwitch;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.vaadin.WebBooleanValueComponentBase;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class WebToggleSwitchImpl extends WebBooleanValueComponentBase<WebToggleSwitchImpl.Vaadin> implements ToggleSwitch {
    public class Vaadin extends ToggleButton implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebToggleSwitchImpl.this;
        }
    }

    @SuppressWarnings("unchecked")
    public WebToggleSwitchImpl(boolean selected) {
        super(selected);

        getVaadinComponent().addValueChangeListener(event -> {
            if (event.isFromClient()) {
                getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, event.getValue()));
            }
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
