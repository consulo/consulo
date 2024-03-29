/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class PsiReferenceService {

  public static final Key<Hints> HINTS = Key.create("HINTS");

  public static PsiReferenceService getService() {
    return Application.get().getInstance(PsiReferenceService.class);
  }

  /**
   * By default, return the same as {@link PsiElement#getReferences()}.
   * For elements implementing {@link com.intellij.psi.ContributedReferenceHost} also run
   * the reference providers registered in {@link com.intellij.psi.PsiReferenceContributor}
   * extensions.
   * @param element PSI element to which the references will be bound
   * @param hints optional hints which are passed to {@link com.intellij.psi.PsiReferenceProvider#acceptsHints(PsiElement, PsiReferenceService.Hints)} and
   * {@link com.intellij.psi.PsiReferenceProvider#acceptsTarget(PsiElement)} before the {@link consulo.ide.impl.idea.patterns.ElementPattern} is matched, for performing
   * fail-fast checks in case the pattern takes long to match.
   * @return the references
   */
  @RequiredReadAction
  public abstract List<PsiReference> getReferences(@Nonnull final PsiElement element, @Nonnull final Hints hints);

  @Nonnull
  @RequiredReadAction
  public PsiReference[] getContributedReferences(@Nonnull final PsiElement element) {
    final List<PsiReference> list = getReferences(element, Hints.NO_HINTS);
    return ContainerUtil.toArray(list, PsiReference.ARRAY_FACTORY);
  }

  public static class Hints {
    public static final Hints NO_HINTS = new Hints();

    @Nullable public final PsiElement target;
    @Nullable public final Integer offsetInElement;

    public Hints() {
      target = null;
      offsetInElement = null;
    }

    public Hints(@Nullable PsiElement target, @Nullable Integer offsetInElement) {
      this.target = target;
      this.offsetInElement = offsetInElement;
    }
  }
}
