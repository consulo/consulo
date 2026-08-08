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
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import tools.jackson.databind.JsonNode;

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
    private static final String KEY = "event.key";
    private static final String KEY_CODE = "event.keyCode";
    private static final String ALT = "event.altKey";
    private static final String CTRL = "event.ctrlKey";
    private static final String SHIFT = "event.shiftKey";
    private static final String META = "event.metaKey";

    /**
     * Click count of the ui event - a pointer sets it, a click the browser synthesises for enter or space on a
     * focused control leaves it at zero, which is what tells the two apart.
     */
    private static final String DETAIL = "event.detail";

    /**
     * A keyboard activation carries no coordinates at all - the box of the element stands in for them, so a
     * caller placing a popup at the event still puts it against the control the user acted on.
     */
    private static final String ELEMENT_LEFT = "element.getBoundingClientRect().left";
    private static final String ELEMENT_TOP = "element.getBoundingClientRect().top";
    private static final String ELEMENT_SCREEN_X = "window.screenX + element.getBoundingClientRect().left";
    private static final String ELEMENT_SCREEN_Y = "window.screenY + element.getBoundingClientRect().top";

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
        registration.addEventData(DETAIL);
        registration.addEventData(ELEMENT_LEFT);
        registration.addEventData(ELEMENT_TOP);
        registration.addEventData(ELEMENT_SCREEN_X);
        registration.addEventData(ELEMENT_SCREEN_Y);

        return registration;
    }

    public static DomListenerRegistration addKeyListener(
        Element element,
        String eventType,
        Consumer<KeyboardInputDetails> consumer
    ) {
        DomListenerRegistration registration = element.addEventListener(eventType, event -> consumer.accept(convertKey(event)));

        registration.addEventData(KEY);
        registration.addEventData(KEY_CODE);
        registration.addEventData(ALT);
        registration.addEventData(CTRL);
        registration.addEventData(SHIFT);
        registration.addEventData(META);
        registration.addEventData(ELEMENT_SCREEN_X);
        registration.addEventData(ELEMENT_SCREEN_Y);

        return registration;
    }

    private static KeyboardInputDetails convertKey(DomEvent event) {
        JsonNode data = event.getEventData();

        Point2D positionOnScreen = new Point2D(
            (int) data.path(ELEMENT_SCREEN_X).asDouble(0),
            (int) data.path(ELEMENT_SCREEN_Y).asDouble(0)
        );

        return new KeyboardInputDetails(new Point2D(0, 0), positionOnScreen, modifiers(data), keyCode(data));
    }

    /**
     * The browser numbers its keys on its own - enter is 13 where {@link KeyCode#ENTER} is 10 - so the named keys
     * are matched by {@code event.key} and only the rest fall back to the numeric code.
     */
    private static KeyCode keyCode(JsonNode data) {
        String key = data.path(KEY).asString("");

        return switch (key) {
            case "Enter" -> KeyCode.ENTER;
            case "Escape" -> KeyCode.ESCAPE;
            case "Tab" -> KeyCode.TAB;
            case "ArrowUp" -> KeyCode.UP;
            case "ArrowDown" -> KeyCode.DOWN;
            case "ArrowLeft" -> KeyCode.LEFT;
            case "ArrowRight" -> KeyCode.RIGHT;
            case "Home" -> KeyCode.HOME;
            case "End" -> KeyCode.END;
            case "Shift" -> KeyCode.SHIFT;
            case "Control" -> KeyCode.CTRL;
            case "Alt" -> KeyCode.ALT;
            case "Meta" -> KeyCode.META;
            default -> KeyCode.of(data.path(KEY_CODE).asInt(0));
        };
    }

    private static EnumSet<ModifiedInputDetails.Modifier> modifiers(JsonNode data) {
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
        return modifiers;
    }

    private static InputDetails convert(DomEvent event) {
        JsonNode data = event.getEventData();

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

        if (data.path(DETAIL).asInt(0) == 0) {
            Point2D position = new Point2D(0, 0);
            Point2D positionOnScreen = new Point2D(
                (int) data.path(ELEMENT_SCREEN_X).asDouble(0),
                (int) data.path(ELEMENT_SCREEN_Y).asDouble(0)
            );

            // the browser never says which key it was, and enter is the one every control answers to
            return new KeyboardInputDetails(position, positionOnScreen, modifiers, KeyCode.ENTER);
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
