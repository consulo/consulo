/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.findUsage;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;

public class DescriptiveNameUtil {
  private static final Logger LOG = Logger.getInstance(DescriptiveNameUtil.class);

  @Nonnull
  public static String getMetaDataName(PsiMetaData metaData) {
    String name = metaData.getName();
    return StringUtil.isEmpty(name) ? "''" : name;
  }

  @RequiredReadAction
  public static String getDescriptiveName(@Nonnull PsiElement psiElement) {
    LOG.assertTrue(psiElement.isValid());

    if (psiElement instanceof PsiMetaOwner) {
      PsiMetaOwner psiMetaOwner = (PsiMetaOwner)psiElement;
      PsiMetaData metaData = psiMetaOwner.getMetaData();
      if (metaData != null) return getMetaDataName(metaData);
    }

    Language lang = psiElement.getLanguage();
    FindUsagesProvider provider = FindUsagesProvider.forLanguage(lang);
    return provider.getDescriptiveName(psiElement);
  }
}
