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
package consulo.ide.impl.idea.codeInsight.intention.actions;

import consulo.annotation.component.ActionImpl;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.intention.impl.ShowIntentionActionsHandler;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;

/**
 * @author mike
 */
@ActionImpl(id = IdeActions.ACTION_SHOW_INTENTION_ACTIONS)
public class ShowIntentionActionsAction extends BaseCodeInsightAction implements HintManagerImpl.ActionToIgnore {
    public ShowIntentionActionsAction() {
        super(ActionLocalize.actionShowintentionactionsText(), LocalizeValue.empty(), PlatformIconGroup.actionsIntentionbulbgrey());
        setEnabledInModalContext(true);
    }

    @Override
    protected boolean isValidForLookup() {
        return true;
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new ShowIntentionActionsHandler();
    }
}
