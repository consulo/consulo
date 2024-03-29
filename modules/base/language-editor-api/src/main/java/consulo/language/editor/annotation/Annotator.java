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
package consulo.language.editor.annotation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Implemented by a custom language plugin to add annotations to files in the language.
 *
 * DO NOT STORE any state inside annotator.
 * If you absolutely must, clear the state upon exit from the {@link #annotate(PsiElement, AnnotationHolder)} method.
 *
 * @author max
 * @see AnnotatorFactory
 */
public interface Annotator {
  /**
   * Annotates the specified PSI element.
   * It is guaranteed to be executed in non-reentrant fashion.
   * I.e there will be no call of this method for this instance before previous call get completed.
   * Multiple instances of the annotator might exist simultaneously, though.
   *
   * @param element to annotate.
   * @param holder  the container which receives annotations created by the plugin.
   */
  void annotate(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder);
}
