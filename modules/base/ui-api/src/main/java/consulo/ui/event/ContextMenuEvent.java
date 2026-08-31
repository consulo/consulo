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
 * The gesture which asks for the menu of a component - a right click on the desktop and in the browser, and whatever
 * else the platform recognises. A frontend which raises this also keeps its own menu from opening, so the one the
 * listener shows is the only one.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public final class ContextMenuEvent extends ComponentEvent<Component> {
    public ContextMenuEvent(Component component, InputDetails inputDetails) {
        super(component, inputDetails);
    }
}
