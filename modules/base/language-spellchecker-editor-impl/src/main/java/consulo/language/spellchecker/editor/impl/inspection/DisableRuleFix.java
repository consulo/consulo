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
package consulo.language.spellchecker.editor.impl.inspection;

import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

/**
 * Turns off the engine rule which reported a grammar or style problem.
 *
 * @author VISTALL
 * @since 2026-11-03
 */
class DisableRuleFix implements LocalQuickFix, LowPriorityAction {
    private final SpellcheckerEngine myEngine;
    private final String myRuleId;
    private final LocalizeValue myRuleName;

    DisableRuleFix(SpellcheckerEngine engine, String ruleId, LocalizeValue ruleName) {
        myEngine = engine;
        myRuleId = ruleId;
        myRuleName = ruleName;
    }

    @Override
    public LocalizeValue getName() {
        return SpellCheckerLocalize.disableRule(myRuleName);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        myEngine.disableRule(myRuleId);
        DaemonCodeAnalyzer.getInstance(project).restart();
    }
}
