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

import consulo.ui.Component;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Hands a vaadin child of a layout to a {@code forEachChild} consumer when it stands for a ui component; the caption,
 * holders and other parts a layout builds for itself are passed over.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
final class WebLayoutChildren {
    private WebLayoutChildren() {
    }

    static void accept(com.vaadin.flow.component.@Nullable Component child, Consumer<Component> consumer) {
        if (child instanceof FromVaadinComponentWrapper wrapper) {
            Component component = wrapper.toUIComponent();
            if (component != null) {
                consumer.accept(component);
            }
        }
    }
}
