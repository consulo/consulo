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

package consulo.usage;

import consulo.application.presentation.TypePresentationService;
import consulo.language.Language;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.util.lang.StringUtil;

/**
 * @author peter
 */
public class UsageViewTypeLocation extends ElementDescriptionLocation {
    private UsageViewTypeLocation() {
    }

    public static final UsageViewTypeLocation INSTANCE = new UsageViewTypeLocation();

    @Override
    public ElementDescriptionProvider getDefaultProvider() {
        return DEFAULT_PROVIDER;
    }

    private static final ElementDescriptionProvider DEFAULT_PROVIDER = (psiElement, location) -> {
        if (!(location instanceof UsageViewTypeLocation)) {
            return null;
        }

        if (psiElement instanceof PsiMetaOwner metaOwner) {
            PsiMetaData metaData = metaOwner.getMetaData();
            if (metaData instanceof PsiPresentableMetaData presentableMetaData) {
                return presentableMetaData.getTypeName();
            }
        }

        if (psiElement instanceof PsiFile) {
            return LanguageLocalize.termsFile().get();
        }
        if (psiElement instanceof PsiDirectory) {
            return LanguageLocalize.termsDirectory().get();
        }

        Language lang = psiElement.getLanguage();
        FindUsagesProvider provider = FindUsagesProvider.forLanguage(lang);
        String type = provider.getType(psiElement);
        if (StringUtil.isNotEmpty(type)) {
            return type;
        }

        return TypePresentationService.getInstance().getTypeNameOrStub(psiElement);
    };
}