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

import com.vaadin.flow.component.Component;
import consulo.ui.ComboBoxStyle;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class WebComboBoxStyleUtil {
    private WebComboBoxStyleUtil() {
    }

    static void apply(Component component, ComboBoxStyle style) {
        switch (style) {
            case TRANSPARENT_BACKGROUND:
                component.getStyle().set("--vaadin-input-field-background", "transparent");
                break;
            case INPLACE:
                component.getStyle()
                    .set("--vaadin-input-field-border-width", "0")
                    .set("--vaadin-input-field-border-radius", "0")
                    .set("--vaadin-focus-ring-width", "0")
                    .set("--aura-shadow-xs", "none");
                break;
        }
    }
}
