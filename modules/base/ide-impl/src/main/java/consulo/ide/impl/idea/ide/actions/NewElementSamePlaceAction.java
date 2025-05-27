/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@ActionImpl(id = "NewElementSamePlace")
public class NewElementSamePlaceAction extends NewElementAction {
    @Inject
    public NewElementSamePlaceAction(ActionManager actionManager) {
        super(actionManager);
    }

    @Nonnull
    @Override
    protected LocalizeValue getPopupTitle() {
        return IdeLocalize.titlePopupNewElementSamePlace();
    }
}