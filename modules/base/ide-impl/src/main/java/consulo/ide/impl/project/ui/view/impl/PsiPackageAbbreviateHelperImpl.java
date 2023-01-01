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
package consulo.ide.impl.project.ui.view.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.language.psi.AnyPsiChangeListener;
import consulo.language.psi.PsiPackage;
import consulo.project.Project;
import consulo.project.ui.view.internal.PsiPackageAbbreviateHelper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@Singleton
@ServiceImpl
public class PsiPackageAbbreviateHelperImpl implements PsiPackageAbbreviateHelper, Disposable {
  private static final int SUBPACKAGE_LIMIT = 2;

  private final ConcurrentMap<PsiPackage, Boolean> myShouldAbbreviateNameMap = new ConcurrentHashMap<>();

  @Inject
  public PsiPackageAbbreviateHelperImpl(Project project) {
    project.getMessageBus().connect(this).subscribe(AnyPsiChangeListener.class, new AnyPsiChangeListener() {
      @Override
      public void beforePsiChanged(boolean isPhysical) {
        myShouldAbbreviateNameMap.clear();
      }
    });
  }

  private static boolean scanPackages(@Nonnull PsiPackage p, int packageNameOccurrencesFound) {
    final PsiPackage[] subPackages = p.getSubPackages();
    packageNameOccurrencesFound += subPackages.length;
    if (packageNameOccurrencesFound > SUBPACKAGE_LIMIT) {
      return true;
    }
    for (PsiPackage subPackage : subPackages) {
      if (scanPackages(subPackage, packageNameOccurrencesFound)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean shouldAbbreviateName(@Nonnull PsiPackage aPackage) {
    return myShouldAbbreviateNameMap.computeIfAbsent(aPackage, psiPackage -> scanPackages(psiPackage, 1));
  }

  @Override
  public void dispose() {
    myShouldAbbreviateNameMap.clear();
  }
}
