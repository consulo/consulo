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
package consulo.language.spellchecker.editor.impl.quickfix;

import consulo.language.spellchecker.editor.SpellCheckerManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptorUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.ui.image.Image;

public class AcceptWordAsCorrect implements SpellCheckerQuickFix {
    private String myWord;

    public AcceptWordAsCorrect(String word) {
        myWord = word;
    }

    public AcceptWordAsCorrect() {
    }

    @Override
    public LocalizeValue getName() {
        return myWord != null ? SpellCheckerLocalize.add0ToDictionary(myWord) : SpellCheckerLocalize.addToDictionary();
    }

    @Override
    @RequiredReadAction
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        SpellCheckerManager spellCheckerManager = SpellCheckerManager.getInstance(project);
        if (myWord != null) {
            spellCheckerManager.acceptWordAsCorrect(myWord);
        }
        else {
            spellCheckerManager.acceptWordAsCorrect(ProblemDescriptorUtil.extractHighlightedText(descriptor, descriptor.getPsiElement()));
        }
    }

    @Override
    public Image getIcon(int flags) {
        return PlatformIconGroup.generalInspectionstypos();
    }
}
