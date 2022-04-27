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
package consulo.language.impl.internal.psi;

import consulo.component.extension.BaseKeyedLazyInstance;
import consulo.component.extension.KeyedLazyInstance;
import consulo.language.Language;
import consulo.language.psi.PsiReferenceContributor;
import consulo.util.xml.serializer.annotation.Attribute;

import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class PsiReferenceContributorEP extends BaseKeyedLazyInstance<PsiReferenceContributor> implements KeyedLazyInstance<PsiReferenceContributor> {

  @Attribute("language")
  public String language = Language.ANY.getID();

  @Attribute("implementation")
  public String implementationClass;

  @Nullable
  @Override
  protected String getImplementationClassName() {
    return implementationClass;
  }

  @Override
  public String getKey() {
    return language;
  }
}
