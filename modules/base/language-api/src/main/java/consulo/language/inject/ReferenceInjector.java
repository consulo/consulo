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
package consulo.language.inject;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 * @since 2013-08-01
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ReferenceInjector extends Injectable {

  public final static ExtensionPointName<ReferenceInjector> EXTENSION_POINT_NAME = ExtensionPointName.create(ReferenceInjector.class);

  @Override
  public final Language getLanguage() {
    return null;
  }

  /**
   * Generated references should be soft ({@link PsiReference#isSoft()})
   */
  @Nonnull
  public abstract PsiReference[] getReferences(@Nonnull PsiElement element, @Nonnull ProcessingContext context, @Nonnull TextRange range);

  public static ReferenceInjector findById(String id) {
    return ContainerUtil.find(EXTENSION_POINT_NAME.getExtensionList(), injector -> id.equals(injector.getId()));
  }
}
