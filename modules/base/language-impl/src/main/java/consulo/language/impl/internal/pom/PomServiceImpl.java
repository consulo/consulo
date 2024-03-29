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
package consulo.language.impl.internal.pom;

import consulo.annotation.component.ServiceImpl;
import consulo.language.impl.psi.PomTargetPsiElementImpl;
import consulo.language.pom.PomService;
import consulo.language.pom.PomTarget;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public class PomServiceImpl extends PomService {
  private final Project myProject;

  @Inject
  public PomServiceImpl(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public PsiElement convertToPsi(@Nonnull PomTarget target) {
    if (target instanceof PsiElement) {
      return (PsiElement)target;
    }
    return new PomTargetPsiElementImpl(myProject, target);
  }
}
