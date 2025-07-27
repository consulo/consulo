/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.codeEditor.impl.internal.action.SimplePasteAction;
import consulo.ide.impl.idea.openapi.editor.actions.MultiplePasteAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.NonTrivialActionGroup;

import jakarta.annotation.Nonnull;

// from kotlin
@ActionImpl(
    id = "PasteGroup",
    children = {
        @ActionRef(type = PasteAction.class),
        @ActionRef(type = MultiplePasteAction.class),
        @ActionRef(type = SimplePasteAction.class)
    }
)
public class PopupInMainMenuActionGroup extends NonTrivialActionGroup {
    public PopupInMainMenuActionGroup() {
        super(ActionLocalize.groupPastegroupText(), true);
    }

    @Override
    public boolean isPopup(@Nonnull String place) {
        return ActionPlaces.MAIN_MENU.equals(place);
    }
}
