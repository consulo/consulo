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
package consulo.language.impl.psi;

import consulo.language.pom.PomRenameableTarget;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiWritableMetaData;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class PsiMetaDataTarget extends DelegatePsiTarget implements PomRenameableTarget<PsiMetaDataTarget> {
  private final PsiMetaData myMetaData;

  public PsiMetaDataTarget(@Nonnull PsiMetaData metaData) {
    super(metaData.getDeclaration());
    myMetaData = metaData;
  }

  @Override
  @Nonnull
  public String getName() {
    return myMetaData.getName();
  }

  @Override
  public boolean isWritable() {
    return myMetaData instanceof PsiWritableMetaData && getNavigationElement().isWritable();
  }

  @Override
  public PsiMetaDataTarget setName(@Nonnull String newName) {
    ((PsiWritableMetaData) myMetaData).setName(newName);
    return this;
  }

}
