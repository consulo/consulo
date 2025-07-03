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

import consulo.language.Language;
import consulo.language.impl.ast.TreeElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.util.CharTable;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public interface HolderFactory {
  DummyHolder createHolder(@Nonnull PsiManager manager, TreeElement contentElement, PsiElement context);
  DummyHolder createHolder(@Nonnull PsiManager manager, CharTable table, boolean validity);
  DummyHolder createHolder(@Nonnull PsiManager manager, PsiElement context);
  DummyHolder createHolder(@Nonnull PsiManager manager, Language language, PsiElement context);
  DummyHolder createHolder(@Nonnull PsiManager manager, TreeElement contentElement, PsiElement context, CharTable table);
  DummyHolder createHolder(@Nonnull PsiManager manager, PsiElement context, CharTable table);
  DummyHolder createHolder(@Nonnull PsiManager manager, final CharTable table, final Language language);
}