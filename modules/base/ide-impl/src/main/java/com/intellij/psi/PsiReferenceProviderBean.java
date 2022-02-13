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

package com.intellij.psi;

import consulo.language.Language;
import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceProvider;
import consulo.logging.Logger;
import consulo.component.extension.AbstractExtensionPointBean;
import consulo.component.extension.ExtensionPointName;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.ElementPatternBean;
import consulo.language.pattern.StandardPatterns;
import consulo.component.extension.KeyedLazyInstance;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Registers a {@link PsiReferenceProvider} in plugin.xml
 */
public class PsiReferenceProviderBean extends AbstractExtensionPointBean implements KeyedLazyInstance<PsiReferenceProviderBean> {

  public static final ExtensionPointName<PsiReferenceProviderBean> EP_NAME = ExtensionPointName.create("consulo.psi.referenceProvider");

  @Attribute("language")
  public String language = Language.ANY.getID();

  @Attribute("providerClass")
  public String className;

  @Tag("description")
  public String description;

  @Property(surroundWithTag = false)
  @AbstractCollection(surroundWithTag = false)
  public ElementPatternBean[] patterns;

  public String getDescription() {
    return description;
  }

  private static final Logger LOG = Logger.getInstance(PsiReferenceProviderBean.class);

  public PsiReferenceProvider instantiate() {
    try {
      return (PsiReferenceProvider)instantiate(className, Application.get().getInjectingContainer());
    }
    catch (ClassNotFoundException e) {
      LOG.error(e);
    }
    return null;
  }

  private static final NullableFunction<ElementPatternBean,ElementPattern<? extends PsiElement>> PATTERN_NULLABLE_FUNCTION = new NullableFunction<ElementPatternBean, ElementPattern<? extends PsiElement>>() {
    @Override
    public ElementPattern<? extends PsiElement> fun(ElementPatternBean elementPatternBean) {
      return elementPatternBean.compilePattern();
    }
  };

  @Nullable
  public ElementPattern<PsiElement> createElementPattern() {
    if (patterns.length > 1) {
      List<ElementPattern<? extends PsiElement>> list = ContainerUtil.mapNotNull(patterns, PATTERN_NULLABLE_FUNCTION);
      //noinspection unchecked
      return StandardPatterns.or(list.toArray(new ElementPattern[list.size()]));
    }
    else if (patterns.length == 1) {
      return patterns[0].compilePattern();
    }
    else {
      LOG.error("At least one pattern should be specified");
      return null;
    }
  }

  @Override
  public String getKey() {
    return language;
  }

  @Override
  public PsiReferenceProviderBean getInstance() {
    return this;
  }
}
