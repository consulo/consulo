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
package consulo.web.internal.ui.base;

import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ref.SimpleReference;

/**
 * Runs an action when a component is first shown, the way {@code UiNotifyConnector#doWhenFirstShown} does it for
 * awt. Being attached is not the same as being shown here - an unselected tab keeps its content attached and only
 * hides it - so the moment is decided in the browser and reported by {@code frontend/showNotifier.js}.
 * <p/>
 * The script is asked for on an element which may not be attached yet, which is what makes the registration alone
 * enough: flow keeps the call until the element has a ui to run it in.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public final class WebShowNotifier {
    private static final String EVENT = "consulo-shown";

    private WebShowNotifier() {
    }

    public static void once(Component component, @RequiredUIAccess Runnable action) {
        Element element = TargetVaadin.to(component).getElement();

        SimpleReference<DomListenerRegistration> ref = SimpleReference.create();

        // the listener before the script: the element may already be shown, and then the event is dispatched
        // within the same client message the call is sent in
        ref.set(element.addEventListener(EVENT, event -> {
            ref.get().remove();

            action.run();
        }));

        element.executeJs("window.consuloShowNotifier.once(this)");
    }
}
