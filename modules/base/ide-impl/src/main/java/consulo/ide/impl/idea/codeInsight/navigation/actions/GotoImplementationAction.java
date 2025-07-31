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

package consulo.ide.impl.idea.codeInsight.navigation.actions;

import consulo.annotation.component.ActionImpl;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.ide.impl.idea.codeInsight.navigation.GotoImplementationHandler;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.dumb.DumbAware;
import consulo.language.psi.search.DefinitionsScopedSearch;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "GotoImplementation")
public class GotoImplementationAction extends BaseCodeInsightAction implements DumbAware {
    public GotoImplementationAction() {
        super(ActionLocalize.actionGotoimplementationText(), ActionLocalize.actionGotoimplementationDescription());
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new GotoImplementationHandler();
    }

    @Override
    protected boolean isValidForLookup() {
        return true;
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        if (!DefinitionsScopedSearch.INSTANCE.hasAnyExecutors()) {
            event.getPresentation().setVisible(false);
        }
        else {
            super.update(event);
        }
    }
}
