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
package consulo.language.pattern;

import consulo.language.ast.IElementType;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.ProcessingContext;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class PlatformPatterns extends StandardPatterns {

  public static PsiElementPattern.Capture<PsiElement> psiElement() {
    return new PsiElementPattern.Capture<>(PsiElement.class);
  }

  public static PsiElementPattern.Capture<PsiComment> psiComment() {
    return new PsiElementPattern.Capture<>(PsiComment.class);
  }

  public static PsiElementPattern.Capture<PomTargetPsiElement> pomElement(final ElementPattern<? extends PomTarget> targetPattern) {
    return new PsiElementPattern.Capture<>(PomTargetPsiElement.class).with(new PatternCondition<>("withPomTarget") {
      @Override
      public boolean accepts(@Nonnull final PomTargetPsiElement element, final ProcessingContext context) {
        return targetPattern.accepts(element.getTarget(), context);
      }
    });
  }

  public static PsiFilePattern.Capture<PsiFile> psiFile() {
    return new PsiFilePattern.Capture<>(PsiFile.class);
  }

  public static <T extends PsiFile> PsiFilePattern.Capture<T> psiFile(Class<T> fileClass) {
    return new PsiFilePattern.Capture<>(fileClass);
  }

  public static PsiElementPattern.Capture<PsiElement> psiElement(IElementType type) {
    return psiElement().withElementType(type);
  }

  public static <T extends PsiElement> PsiElementPattern.Capture<T> psiElement(final Class<T> aClass) {
    return new PsiElementPattern.Capture<>(aClass);
  }

  public static IElementTypePattern elementType() {
    return new IElementTypePattern();
  }

  public static VirtualFilePattern virtualFile() {
    return new VirtualFilePattern();
  }
}
