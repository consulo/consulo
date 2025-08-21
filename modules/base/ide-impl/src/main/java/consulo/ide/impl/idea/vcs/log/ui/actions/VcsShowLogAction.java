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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.ide.impl.idea.vcs.VcsShowToolWindowTabAction;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogContentProvider;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

public class VcsShowLogAction extends VcsShowToolWindowTabAction {
    public VcsShowLogAction() {
        super(LocalizeValue.localizeTODO("Show VCS Log"));
    }

    @Nonnull
    @Override
    protected String getTabName() {
        return VcsLogContentProvider.TAB_NAME;
    }
}

