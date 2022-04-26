/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.psi.meta;

import consulo.application.Application;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.ElementFilter;

import javax.annotation.Nullable;

/**
 * Provides association for elements matching given filter with metadata class.
 *
 * @see MetaDataContributor
 */
public interface MetaDataService extends MetaDataRegistrar {
  /**
   * Associates elements matching given filter with metadata class.
   *
   * @param filter                  on element for finding metadata matches
   * @param metadataDescriptorClass class of metadata, should be instantiable without parameters
   * @deprecated use {@link #registerMetaData(ElementPattern, Class)}
   */
  <T extends PsiMetaData> void registerMetaData(ElementFilter filter, Class<T> metadataDescriptorClass);

  /**
   * Associates elements matching given filter with metadata class.
   *
   * @param pattern                 on element for finding metadata matches
   * @param metadataDescriptorClass class of metadata, should be instantiable without parameters
   */
  <T extends PsiMetaData> void registerMetaData(ElementPattern<?> pattern, Class<T> metadataDescriptorClass);

  @Nullable
  PsiMetaData getMeta(final PsiElement element);

  void bindDataToElement(final PsiElement element, final PsiMetaData data);

  public static MetaDataService getInstance() {
    return Application.get().getInstance(MetaDataService.class);
  }
}
