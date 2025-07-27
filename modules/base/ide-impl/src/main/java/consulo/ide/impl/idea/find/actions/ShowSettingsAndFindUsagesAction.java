/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.actions;

import consulo.annotation.component.ActionImpl;
import consulo.find.FindManager;
import consulo.language.psi.PsiElement;
import consulo.platform.base.localize.ActionLocalize;import jakarta.annotation.Nonnull;

@ActionImpl(id = "ShowSettingsAndFindUsages")
public class ShowSettingsAndFindUsagesAction extends FindUsagesAction {
    public ShowSettingsAndFindUsagesAction() {
        super(ActionLocalize.actionShowsettingsandfindusagesText(), ActionLocalize.actionShowsettingsandfindusagesDescription());
    }

    @Override
    protected void startFindUsages(@Nonnull PsiElement element) {
        FindManager.getInstance(element.getProject()).findUsages(element, true);
    }
}
