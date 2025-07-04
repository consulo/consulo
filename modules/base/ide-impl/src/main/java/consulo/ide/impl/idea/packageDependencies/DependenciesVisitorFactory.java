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
package consulo.ide.impl.idea.packageDependencies;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.language.psi.PsiReference;

import jakarta.inject.Singleton;

/**
 * @author anna
 * @since 2008-01-21
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class DependenciesVisitorFactory {

  public static DependenciesVisitorFactory getInstance() {
    return ServiceManager.getService(DependenciesVisitorFactory.class);
  }

  public PsiElementVisitor createVisitor(final DependenciesBuilder.DependencyProcessor processor) {
    return new PsiRecursiveElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);
        PsiReference[] refs = element.getReferences();
        for (PsiReference ref : refs) {
          PsiElement resolved = ref.resolve();
          if (resolved != null) {
            processor.process(ref.getElement(), resolved);
          }
        }
      }
    };
  }
}