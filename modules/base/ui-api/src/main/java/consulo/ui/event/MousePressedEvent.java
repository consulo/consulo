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
package consulo.ui.event;

import consulo.ui.Component;
import consulo.ui.event.details.InputDetails;

/**
 * A mouse button went down on a component or on anything inside it. The press is only reported: whatever is under
 * the pointer still gets it and reacts to it as usual, so a listener learns where the user is working without taking
 * the press away from anything.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public final class MousePressedEvent extends ComponentEvent<Component> {
    public MousePressedEvent(Component component, InputDetails inputDetails) {
        super(component, inputDetails);
    }
}
