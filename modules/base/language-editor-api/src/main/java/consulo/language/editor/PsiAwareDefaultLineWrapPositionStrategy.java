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
package consulo.language.editor;

import consulo.codeEditor.LineWrapPositionStrategy;
import consulo.document.Document;
import consulo.language.ast.IElementType;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * {@link LineWrapPositionStrategy} implementation that uses
 * {@link LanguageLineWrapPositionStrategy#getDefaultImplementation() default line wrap strategy} but restricts its scope
 * by {@link #PsiAwareDefaultLineWrapPositionStrategy(boolean, IElementType...) target tokens/elements}.
 * 
 * @author Denis Zhdanov
 * @since 5/12/11 12:50 PM
 */
public abstract class PsiAwareDefaultLineWrapPositionStrategy extends PsiAwareLineWrapPositionStrategy {

  public PsiAwareDefaultLineWrapPositionStrategy(boolean nonVirtualOnly, @Nonnull IElementType ... enabledTypes) {
    super(nonVirtualOnly, enabledTypes);
  }

  @Override
  protected int doCalculateWrapPosition(@Nonnull Document document,
                                        @Nullable Project project,
                                        int startOffset,
                                        int endOffset,
                                        int maxPreferredOffset,
                                        boolean allowToBeyondMaxPreferredOffset,
                                        boolean virtual)
  {
    LineWrapPositionStrategy implementation = LanguageLineWrapPositionStrategy.getDefaultImplementation();
    return implementation.calculateWrapPosition(document, project, startOffset, endOffset, maxPreferredOffset,
                                                allowToBeyondMaxPreferredOffset, virtual);
  }
}
