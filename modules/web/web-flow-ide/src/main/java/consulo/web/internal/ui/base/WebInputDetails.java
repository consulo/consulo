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

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import consulo.ui.Point2D;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;

import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * The typed vaadin click event carries no position relative to the component it happened in, and that is
 * what a caller placing a popup at the click needs. A dom listener can ask for the fields of the browser
 * event itself, so clicks are taken from there instead.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebInputDetails {
    private static final String OFFSET_X = "event.offsetX";
    private static final String OFFSET_Y = "event.offsetY";
    private static final String SCREEN_X = "event.screenX";
    private static final String SCREEN_Y = "event.screenY";
    private static final String BUTTON = "event.button";
    private static final String ALT = "event.altKey";
    private static final String CTRL = "event.ctrlKey";
    private static final String SHIFT = "event.shiftKey";
    private static final String META = "event.metaKey";

    public static DomListenerRegistration addClickListener(Element element, Consumer<InputDetails> consumer) {
        DomListenerRegistration registration = element.addEventListener("click", event -> consumer.accept(convert(event)));

        registration.addEventData(OFFSET_X);
        registration.addEventData(OFFSET_Y);
        registration.addEventData(SCREEN_X);
        registration.addEventData(SCREEN_Y);
        registration.addEventData(BUTTON);
        registration.addEventData(ALT);
        registration.addEventData(CTRL);
        registration.addEventData(SHIFT);
        registration.addEventData(META);

        return registration;
    }

    private static MouseInputDetails convert(DomEvent event) {
        var data = event.getEventData();

        EnumSet<ModifiedInputDetails.Modifier> modifiers = EnumSet.noneOf(ModifiedInputDetails.Modifier.class);
        if (data.path(ALT).asBoolean(false)) {
            modifiers.add(ModifiedInputDetails.Modifier.ALT);
        }
        if (data.path(CTRL).asBoolean(false)) {
            modifiers.add(ModifiedInputDetails.Modifier.CTRL);
        }
        if (data.path(SHIFT).asBoolean(false)) {
            modifiers.add(ModifiedInputDetails.Modifier.SHIFT);
        }
        if (data.path(META).asBoolean(false)) {
            modifiers.add(ModifiedInputDetails.Modifier.META);
        }

        MouseInputDetails.MouseButton button = switch (data.path(BUTTON).asInt(0)) {
            case 1 -> MouseInputDetails.MouseButton.MIDDLE;
            case 2 -> MouseInputDetails.MouseButton.RIGHT;
            default -> MouseInputDetails.MouseButton.LEFT;
        };

        Point2D position = new Point2D((int) data.path(OFFSET_X).asDouble(0), (int) data.path(OFFSET_Y).asDouble(0));
        Point2D positionOnScreen = new Point2D((int) data.path(SCREEN_X).asDouble(0), (int) data.path(SCREEN_Y).asDouble(0));

        return new MouseInputDetails(position, positionOnScreen, modifiers, button);
    }
}
