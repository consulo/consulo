/*
 * Copyright 2013-2024 consulo.io
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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.psi.event.PsiTreeChangePreprocessor;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 18-Jun-24
 */
@ExtensionImpl(order = "first")
public class PsiModificationTrackerTreePreprocessor implements PsiTreeChangePreprocessor {
  private final Provider<PsiModificationTracker> myPsiModificationTrackerProvider;

  @Inject
  public PsiModificationTrackerTreePreprocessor(Provider<PsiModificationTracker> psiModificationTrackerProvider) {
    myPsiModificationTrackerProvider = psiModificationTrackerProvider;
  }

  @Override
  public void treeChanged(@Nonnull PsiTreeChangeEvent event) {
    PsiModificationTrackerImpl tracker = (PsiModificationTrackerImpl)myPsiModificationTrackerProvider.get();

    tracker.treeChanged(event);
  }
}
