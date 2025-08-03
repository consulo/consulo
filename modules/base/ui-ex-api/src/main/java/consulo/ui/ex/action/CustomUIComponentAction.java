/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ui.ex.action;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-08-03
 */
public interface CustomUIComponentAction {
    Key<Component> COMPONENT_KEY = Key.create("customComponent");
    Key<AnAction> ACTION_KEY = Key.create("customComponentAction");

    /**
     * @return custom Component that represents action in UI.
     * You (as a client/implementor) or this interface are not allowed to invoke
     * this method directly. Only action system can invoke it!
     * <br/>
     * <br/>
     * The component should not be stored in the action instance because it may
     * be shown on several toolbars simultaneously. Use {@link CustomUIComponentAction#COMPONENT_KEY}
     * to retrieve current component from a Presentation instance in {@link AnAction#update(AnActionEvent)} method.
     */
    @Nonnull
    @RequiredUIAccess
    Component createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place);
}

