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
package consulo.ide.impl.idea.codeInsight.completion.actions;

import consulo.annotation.component.ActionImpl;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionFeatures;
import consulo.language.editor.completion.CompletionType;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ActionImpl(id = "SmartTypeCompletion")
public class SmartCodeCompletionAction extends BaseCodeCompletionAction {
    public SmartCodeCompletionAction() {
        super(ActionLocalize.actionSmarttypecompletionText(), ActionLocalize.actionSmarttypecompletionDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_SMARTTYPE_GENERAL);
        invokeCompletion(e, CompletionType.SMART, 1);
    }
}
