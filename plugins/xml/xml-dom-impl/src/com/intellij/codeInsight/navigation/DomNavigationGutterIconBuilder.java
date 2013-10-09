/*
 * Copyright 2013 Consulo.org
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
package com.intellij.codeInsight.navigation;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiElement;
import com.intellij.util.NotNullFunction;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ElementPresentationManager;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 23:22/09.10.13
 */
public class DomNavigationGutterIconBuilder<T> extends NavigationGutterIconBuilder<T> {
  public static final NotNullFunction<DomElement,Collection<? extends PsiElement>> DEFAULT_DOM_CONVERTOR = new NotNullFunction<DomElement, Collection<? extends PsiElement>>() {
    @NotNull
    public Collection<? extends PsiElement> fun(final DomElement o) {
      return ContainerUtil.createMaybeSingletonList(o.getXmlElement());
    }
  };
  public static final NotNullFunction<DomElement, Collection<? extends GotoRelatedItem>> DOM_GOTO_RELATED_ITEM_PROVIDER = new NotNullFunction<DomElement, Collection<? extends GotoRelatedItem>>() {
    @NotNull
    @Override
    public Collection<? extends GotoRelatedItem> fun(DomElement dom) {
      if (dom.getXmlElement() != null) {
        return Collections.singletonList(new DomGotoRelatedItem(dom));
      }
      return Collections.emptyList();
    }
  };

  public static DomNavigationGutterIconBuilder<PsiElement> create(@NotNull final Icon icon) {
    return create(icon, DEFAULT_PSI_CONVERTOR, PSI_GOTO_RELATED_ITEM_PROVIDER);
  }

  public static <T> DomNavigationGutterIconBuilder<T> create(@NotNull final Icon icon,
                                                          @NotNull NotNullFunction<T, Collection<? extends PsiElement>> converter) {
    return create(icon, converter, null);
  }

  public static <T> DomNavigationGutterIconBuilder<T> create(@NotNull final Icon icon,
                                                          @NotNull NotNullFunction<T, Collection<? extends PsiElement>> converter,
                                                          final @Nullable NotNullFunction<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
    return new DomNavigationGutterIconBuilder<T>(icon, converter, gotoRelatedItemProvider);
  }

  protected DomNavigationGutterIconBuilder(@NotNull Icon icon, @NotNull NotNullFunction<T, Collection<? extends PsiElement>> converter) {
    super(icon, converter);
  }

  protected DomNavigationGutterIconBuilder(@NotNull Icon icon,
                                        @NotNull NotNullFunction<T, Collection<? extends PsiElement>> converter,
                                        @Nullable NotNullFunction<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
    super(icon, converter, gotoRelatedItemProvider);
  }

  @Nullable
  public Annotation install(@NotNull DomElementAnnotationHolder holder, @Nullable DomElement element) {
    if (!myLazy && myTargets.getValue().isEmpty() || element == null) return null;
    return doInstall(holder.createAnnotation(element, HighlightSeverity.INFORMATION, null), element.getManager().getProject());
  }

  @Override
  protected NullableFunction<T,String> createDefaultNamer() {
    return ElementPresentationManager.namer();
  }
}
