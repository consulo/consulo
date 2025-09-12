/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesViewContentManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.VcsShowToolWindowTabAction;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "Vcs.Show.Shelf")
public class VcsShowShelfAction extends VcsShowToolWindowTabAction {
    public VcsShowShelfAction() {
        super(VcsLocalize.actionShowShelfText());
    }

    @Nonnull
    @Override
    protected String getTabName() {
        return ChangesViewContentManager.SHELF;
    }
}

