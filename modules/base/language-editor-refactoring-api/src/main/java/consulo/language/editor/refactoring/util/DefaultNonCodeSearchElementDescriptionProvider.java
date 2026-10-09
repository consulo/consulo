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
package consulo.language.editor.refactoring.util;

import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.language.psi.PsiPackageHelper;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;

/**
 * @author yole
 */
public class DefaultNonCodeSearchElementDescriptionProvider implements ElementDescriptionProvider {
    private static final Logger LOG = Logger.getInstance(DefaultNonCodeSearchElementDescriptionProvider.class);

    public static final DefaultNonCodeSearchElementDescriptionProvider INSTANCE = new DefaultNonCodeSearchElementDescriptionProvider();

    @Override
    public LocalizeValue getElementDescription(PsiElement element, ElementDescriptionLocation location) {
        if (!(location instanceof NonCodeSearchDescriptionLocation)) {
            return LocalizeValue.empty();
        }
        NonCodeSearchDescriptionLocation ncdLocation = (NonCodeSearchDescriptionLocation) location;

        if (element instanceof PsiDirectory) {
            if (ncdLocation.isNonJava()) {
                String qName = PsiPackageHelper.getInstance(element.getProject()).getQualifiedName((PsiDirectory) element, false);
                return LocalizeValue.of(qName);
            }
            return LocalizeValue.localizeTODO(((PsiDirectory) element).getName());
        }

        if (element instanceof PsiMetaOwner) {
            PsiMetaOwner psiMetaOwner = (PsiMetaOwner)element;
            PsiMetaData metaData = psiMetaOwner.getMetaData();
            if (metaData != null) {
                return LocalizeValue.localizeTODO(metaData.getName());
            }
        }
        if (element instanceof PsiNamedElement namedElem) {
            return LocalizeValue.ofNullable(namedElem.getName());
        }
        else {
            // LOG.error("Unknown element type: " + element);
            return LocalizeValue.empty();
        }
    }
}
