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
package consulo.ide.impl.psi.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.presentation.TypePresentationService;
import consulo.language.editor.highlight.HighlightUsagesDescriptionLocation;
import consulo.language.pom.PomDescriptionProvider;
import consulo.language.pom.PomNamedTarget;
import consulo.language.pom.PomTarget;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageViewNodeTextLocation;
import consulo.usage.UsageViewTypeLocation;

/**
 * @author peter
 */
@ExtensionImpl(id = "pomDefault", order = "last")
public class UsagePomTargetDescriptionProvider extends PomDescriptionProvider {
    @Override
    public LocalizeValue getElementDescription(PomTarget element, ElementDescriptionLocation location) {
        if (element instanceof PsiElement) {
            return LocalizeValue.empty();
        }

        if (location == UsageViewTypeLocation.INSTANCE) {
            return getTypeName(element);
        }
        if (location == UsageViewNodeTextLocation.INSTANCE) {
            return LocalizeValue.join(
                getTypeName(element),
                LocalizeValue.space(),
                element instanceof PomNamedTarget pomNamedTarget ? LocalizeValue.of(pomNamedTarget.getName()) : LocalizeValue.of("''")
            );
        }
        if (location instanceof HighlightUsagesDescriptionLocation) {
            return getTypeName(element);
        }
        return LocalizeValue.empty();
    }

    private static LocalizeValue getTypeName(PomTarget element) {
        return LocalizeValue.of(TypePresentationService.getInstance().getTypeNameOrStub(element));
    }
}
