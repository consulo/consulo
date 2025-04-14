/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.language.Language;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class UsageViewNodeTextLocation extends ElementDescriptionLocation {
    private UsageViewNodeTextLocation() {
    }

    public static final UsageViewNodeTextLocation INSTANCE = new UsageViewNodeTextLocation();

    @Override
    public ElementDescriptionProvider getDefaultProvider() {
        return DEFAULT_PROVIDER;
    }

    private static final ElementDescriptionProvider DEFAULT_PROVIDER = new ElementDescriptionProvider() {
        @Override
        public String getElementDescription(@Nonnull final PsiElement element, @Nonnull final ElementDescriptionLocation location) {
            if (!(location instanceof UsageViewNodeTextLocation)) {
                return null;
            }

            if (element instanceof PsiMetaOwner) {
                final PsiMetaData metaData = ((PsiMetaOwner)element).getMetaData();
                if (metaData instanceof PsiPresentableMetaData) {
                    return ((PsiPresentableMetaData)metaData).getTypeName() + " " + DescriptiveNameUtil.getMetaDataName(metaData);
                }
            }

            if (element instanceof PsiFile) {
                return ((PsiFile)element).getName();
            }

            Language language = element.getLanguage();
            FindUsagesProvider provider = FindUsagesProvider.forLanguage(language);
            return provider.getNodeText(element, true);
        }
    };
}