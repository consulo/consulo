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
package consulo.ide.impl.idea.ide.hierarchy.actions;

import consulo.annotation.component.ActionImpl;import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.hierarchy.CallHierarchyProvider;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "CallHierarchy")
public final class BrowseCallHierarchyAction extends BrowseHierarchyActionBase<CallHierarchyProvider> {
    public BrowseCallHierarchyAction() {
        super(ActionLocalize.actionCallhierarchyText(), ActionLocalize.actionCallhierarchyDescription(), CallHierarchyProvider.class);
    }

    @Override
    @RequiredUIAccess
    public final void update(@Nonnull AnActionEvent e) {
        if (!ActionPlaces.MAIN_MENU.equals(e.getPlace())) {
            e.getPresentation().setTextValue(IdeLocalize.actionBrowseCallHierarchy());
        }

        super.update(e);
    }
}