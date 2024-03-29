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

package consulo.language.psi.meta;

import consulo.language.pattern.ElementPattern;
import consulo.language.psi.filter.ElementFilter;

import java.util.function.Supplier;

/**
 * Provides association for elements matching given filter with metadata class.
 *
 * @see MetaDataContributor
 */
public interface MetaDataRegistrar {
  /**
   * Associates elements matching given filter with metadata class.
   *
   * @param filter                  on element for finding metadata matches
   * @param metadataDescriptorFactory class of metadata, should be instantiable without parameters
   * @deprecated use {@link #registerMetaData(ElementPattern, Class)}
   */
  <T extends PsiMetaData> void registerMetaData(ElementFilter filter, Supplier<T> metadataDescriptorFactory);

  /**
   * Associates elements matching given filter with metadata class.
   *
   * @param pattern                 on element for finding metadata matches
   * @param metadataDescriptorFactory class of metadata, should be instantiable without parameters
   */
  <T extends PsiMetaData> void registerMetaData(ElementPattern<?> pattern, Supplier<T> metadataDescriptorFactory);
}
