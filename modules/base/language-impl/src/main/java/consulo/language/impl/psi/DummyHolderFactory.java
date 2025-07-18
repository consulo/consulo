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

import consulo.annotation.DeprecationInfo;
import consulo.language.Language;
import consulo.language.impl.ast.TreeElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.util.CharTable;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class DummyHolderFactory  {
  private static HolderFactory INSTANCE = new DefaultFactory();

  private DummyHolderFactory() {}

  @Deprecated
  @DeprecationInfo("Do not allow overriding it via plugins. Need provide new options based on target language")
  public static void setFactory(HolderFactory factory) {
    INSTANCE = factory;
  }

  public static DummyHolder createHolder(@Nonnull PsiManager manager, TreeElement contentElement, PsiElement context) {
    return INSTANCE.createHolder(manager, contentElement, context);
  }

  public static DummyHolder createHolder(@Nonnull PsiManager manager, CharTable table, boolean validity) {
    return INSTANCE.createHolder(manager, table, validity);
  }

  public static DummyHolder createHolder(@Nonnull PsiManager manager, PsiElement context) {
    return INSTANCE.createHolder(manager, context);
  }

  public static DummyHolder createHolder(@Nonnull PsiManager manager, Language language, PsiElement context) {
    return INSTANCE.createHolder(manager, language, context);
  }

  public static DummyHolder createHolder(@Nonnull PsiManager manager, TreeElement contentElement, PsiElement context, CharTable table) {
    return INSTANCE.createHolder(manager, contentElement, context, table);
  }

  public static DummyHolder createHolder(@Nonnull PsiManager manager, PsiElement context, CharTable table) {
    return INSTANCE.createHolder(manager, context, table);
  }

  public static DummyHolder createHolder(@Nonnull PsiManager manager, final CharTable table, final Language language) {
    return INSTANCE.createHolder(manager, table, language);
  }

  private static class DefaultFactory implements HolderFactory {
    @Override
    public DummyHolder createHolder(@Nonnull PsiManager manager, TreeElement contentElement, PsiElement context) {
      return new DummyHolder(manager, contentElement, context);
    }

    @Override
    public DummyHolder createHolder(@Nonnull PsiManager manager, CharTable table, boolean validity) {
      return new DummyHolder(manager, table, validity);
    }

    @Override
    public DummyHolder createHolder(@Nonnull PsiManager manager, PsiElement context) {
      return new DummyHolder(manager, context);
    }

    @Override
    public DummyHolder createHolder(@Nonnull final PsiManager manager, final Language language, final PsiElement context) {
      return new DummyHolder(manager, language, context);
    }

    @Override
    public DummyHolder createHolder(@Nonnull PsiManager manager, TreeElement contentElement, PsiElement context, CharTable table) {
      return new DummyHolder(manager, contentElement, context, table);
    }

    @Override
    public DummyHolder createHolder(@Nonnull PsiManager manager, PsiElement context, CharTable table) {
      return new DummyHolder(manager, context, table);
    }

    @Override
    public DummyHolder createHolder(@Nonnull PsiManager manager, final CharTable table, final Language language) {
      return new DummyHolder(manager, table, language);
    }
  }
}