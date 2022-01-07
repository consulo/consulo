/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.ide.TypePresentationService;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.util.ConstantFunction;
import com.intellij.util.NotNullFunction;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.*;

/**
 * DOM-specific builder for {@link com.intellij.openapi.editor.markup.GutterIconRenderer}
 * and {@link com.intellij.codeInsight.daemon.LineMarkerInfo}.
 *
 * @author peter
 */
public class NavigationGutterIconBuilder<T> {
  @NonNls private static final String PATTERN = "&nbsp;&nbsp;&nbsp;&nbsp;{0}";
  protected static final NotNullFunction<PsiElement,Collection<? extends PsiElement>> DEFAULT_PSI_CONVERTOR = new NotNullFunction<PsiElement, Collection<? extends PsiElement>>() {
    @Override
    @Nonnull
    public Collection<? extends PsiElement> fun(final PsiElement element) {
      return ContainerUtil.createMaybeSingletonList(element);
    }
  };

  protected static final NullableFunction DEFAULT_NAMER = new NullableFunction<Object, String>() {
    @Nullable
    @Override
    public String fun(Object dom) {
      return TypePresentationService.getInstance().getTypePresentableName(dom.getClass());
    }
  };

  private final Image myIcon;
  private final NotNullFunction<T,Collection<? extends PsiElement>> myConverter;

  protected NotNullLazyValue<Collection<? extends T>> myTargets;
  protected boolean myLazy;
  private String myTooltipText;
  private String myPopupTitle;
  private String myEmptyText;
  private String myTooltipTitle;
  private GutterIconRenderer.Alignment myAlignment = GutterIconRenderer.Alignment.CENTER;
  private Computable<PsiElementListCellRenderer> myCellRenderer;
  private NullableFunction<T,String> myNamer = createDefaultNamer();
  private final NotNullFunction<T, Collection<? extends GotoRelatedItem>> myGotoRelatedItemProvider;

  public static final NotNullFunction<PsiElement, Collection<? extends GotoRelatedItem>> PSI_GOTO_RELATED_ITEM_PROVIDER = new NotNullFunction<PsiElement, Collection<? extends GotoRelatedItem>>() {
    @Nonnull
    @Override
    public Collection<? extends GotoRelatedItem> fun(PsiElement dom) {
      return Collections.singletonList(new GotoRelatedItem(dom, "XML"));
    }
  };

  protected NavigationGutterIconBuilder(@Nonnull final Image icon, @Nonnull NotNullFunction<T, Collection<? extends PsiElement>> converter) {
    this(icon, converter, null);
  }

  protected NavigationGutterIconBuilder(@Nonnull final Image icon,
                                        @Nonnull NotNullFunction<T, Collection<? extends PsiElement>> converter,
                                        final @Nullable NotNullFunction<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
    myIcon = icon;
    myConverter = converter;
    myGotoRelatedItemProvider = gotoRelatedItemProvider;
  }

  public static NavigationGutterIconBuilder<PsiElement> create(@Nonnull final Image icon) {
    return create(icon, DEFAULT_PSI_CONVERTOR, PSI_GOTO_RELATED_ITEM_PROVIDER);
  }

  public static <T> NavigationGutterIconBuilder<T> create(@Nonnull final Image icon,
                                                          @Nonnull NotNullFunction<T, Collection<? extends PsiElement>> converter) {
    return create(icon, converter, null);
  }

  public static <T> NavigationGutterIconBuilder<T> create(@Nonnull final Image icon,
                                                          @Nonnull NotNullFunction<T, Collection<? extends PsiElement>> converter,
                                                          final @Nullable NotNullFunction<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider) {
    return new NavigationGutterIconBuilder<T>(icon, converter, gotoRelatedItemProvider);
  }

  public NavigationGutterIconBuilder<T> setTarget(@Nullable T target) {
    return setTargets(ContainerUtil.createMaybeSingletonList(target));
  }

  public NavigationGutterIconBuilder<T> setTargets(@Nonnull T... targets) {
    return setTargets(Arrays.asList(targets));
  }

  public NavigationGutterIconBuilder<T> setTargets(@Nonnull final NotNullLazyValue<Collection<? extends T>> targets) {
    myTargets = targets;
    myLazy = true;
    return this;
  }

  public NavigationGutterIconBuilder<T> setTargets(@Nonnull final Collection<? extends T> targets) {
    myTargets = new NotNullLazyValue<Collection<? extends T>>() {
      @Override
      @Nonnull
      public Collection<? extends T> compute() {
        return targets;
      }
    };
    return this;
  }

  public NavigationGutterIconBuilder<T> setTooltipText(@Nonnull String tooltipText) {
    myTooltipText = tooltipText;
    return this;
  }

  public NavigationGutterIconBuilder<T> setAlignment(@Nonnull final GutterIconRenderer.Alignment alignment) {
    myAlignment = alignment;
    return this;
  }

  public NavigationGutterIconBuilder<T> setPopupTitle(@Nonnull String popupTitle) {
    myPopupTitle = popupTitle;
    return this;
  }

  public NavigationGutterIconBuilder<T> setEmptyPopupText(@Nonnull String emptyText) {
    myEmptyText = emptyText;
    return this;
  }

  public NavigationGutterIconBuilder<T> setTooltipTitle(@Nonnull final String tooltipTitle) {
    myTooltipTitle = tooltipTitle;
    return this;
  }

  public NavigationGutterIconBuilder<T> setNamer(@Nonnull NullableFunction<T,String> namer) {
    myNamer = namer;
    return this;
  }

  public NavigationGutterIconBuilder<T> setCellRenderer(@Nonnull final PsiElementListCellRenderer cellRenderer) {
    myCellRenderer = new Computable.PredefinedValueComputable<PsiElementListCellRenderer>(cellRenderer);
    return this;
  }

  protected NullableFunction<T,String> createDefaultNamer() {
    return DEFAULT_NAMER;
  }

  @Nullable
  public Annotation install(@Nonnull AnnotationHolder holder, @Nullable PsiElement element) {
    if (!myLazy && myTargets.getValue().isEmpty() || element == null) return null;
    return doInstall(holder.createInfoAnnotation(element, null), element.getProject());
  }

  protected Annotation doInstall(@Nonnull Annotation annotation, @Nonnull Project project) {
    final MyNavigationGutterIconRenderer renderer = createGutterIconRenderer(project);
    annotation.setGutterIconRenderer(renderer);
    annotation.setNeedsUpdateOnTyping(false);
    return annotation;
  }

  public RelatedItemLineMarkerInfo<PsiElement> createLineMarkerInfo(@Nonnull PsiElement element) {
    final MyNavigationGutterIconRenderer renderer = createGutterIconRenderer(element.getProject());
    final String tooltip = renderer.getTooltipText();
    NotNullLazyValue<Collection<? extends GotoRelatedItem>> gotoTargets = new NotNullLazyValue<Collection<? extends GotoRelatedItem>>() {
      @Nonnull
      @Override
      protected Collection<? extends GotoRelatedItem> compute() {
        if (myGotoRelatedItemProvider != null) {
          return ContainerUtil.concat(myTargets.getValue(), myGotoRelatedItemProvider);
        }
        return Collections.emptyList();
      }
    };
    return new RelatedItemLineMarkerInfo<PsiElement>(element, element.getTextRange(), renderer.getIcon(), Pass.LINE_MARKERS,
                                                     tooltip == null ? null : new ConstantFunction<PsiElement, String>(tooltip),
                                                     renderer.isNavigateAction() ? renderer : null, renderer.getAlignment(),
                                                     gotoTargets);
  }

  private void checkBuilt() {
    assert myTargets != null : "Must have called .setTargets() before calling create()";
  }

  private MyNavigationGutterIconRenderer createGutterIconRenderer(@Nonnull Project project) {
    checkBuilt();
    final SmartPointerManager manager = SmartPointerManager.getInstance(project);

    NotNullLazyValue<List<SmartPsiElementPointer>> pointers = new NotNullLazyValue<List<SmartPsiElementPointer>>() {
      @Override
      @Nonnull
      public List<SmartPsiElementPointer> compute() {
        Set<PsiElement> elements = new HashSet<PsiElement>();
        Collection<? extends T> targets = myTargets.getValue();
        final List<SmartPsiElementPointer> list = new ArrayList<SmartPsiElementPointer>(targets.size());
        for (final T target : targets) {
          for (final PsiElement psiElement : myConverter.fun(target)) {
            if (elements.add(psiElement) && psiElement.isValid()) {
              list.add(manager.createSmartPsiElementPointer(psiElement));
            }
          }
        }
        return list;
      }
    };

    final boolean empty = isEmpty();

    if (myTooltipText == null && !myLazy) {
      final SortedSet<String> names = new TreeSet<String>();
      for (T t : myTargets.getValue()) {
        final String text = myNamer.fun(t);
        if (text != null) {
          names.add(MessageFormat.format(PATTERN, text));
        }
      }
      @NonNls StringBuilder sb = new StringBuilder("<html><body>");
      if (myTooltipTitle != null) {
        sb.append(myTooltipTitle).append("<br>");
      }
      for (String name : names) {
        sb.append(name).append("<br>");
      }
      sb.append("</body></html>");
      myTooltipText = sb.toString();
    }

    Computable<PsiElementListCellRenderer> renderer =
      myCellRenderer == null ? DefaultPsiElementCellRenderer::new : myCellRenderer;
    return new MyNavigationGutterIconRenderer(this, myAlignment, myIcon, myTooltipText, pointers, renderer, empty);
  }

  private boolean isEmpty() {
    if (myLazy) {
      return false;
    }

    Set<PsiElement> elements = new HashSet<PsiElement>();
    Collection<? extends T> targets = myTargets.getValue();
    for (final T target : targets) {
      for (final PsiElement psiElement : myConverter.fun(target)) {
        if (elements.add(psiElement)) {
          return false;
        }
      }
    }
    return true;
  }

  private static class MyNavigationGutterIconRenderer extends NavigationGutterIconRenderer {
    private final Alignment myAlignment;
    private final Image myIcon;
    private final String myTooltipText;
    private final boolean myEmpty;

    public MyNavigationGutterIconRenderer(@Nonnull NavigationGutterIconBuilder builder,
                                          final Alignment alignment,
                                          final Image icon,
                                          @Nullable final String tooltipText,
                                          @Nonnull NotNullLazyValue<List<SmartPsiElementPointer>> pointers,
                                          Computable<PsiElementListCellRenderer> cellRenderer,
                                          boolean empty) {
      super(builder.myPopupTitle, builder.myEmptyText, cellRenderer, pointers);
      myAlignment = alignment;
      myIcon = icon;
      myTooltipText = tooltipText;
      myEmpty = empty;
    }

    @Override
    public boolean isNavigateAction() {
      return !myEmpty;
    }

    @Override
    @Nonnull
    public Image getIcon() {
      return myIcon;
    }

    @Override
    @Nullable
    public String getTooltipText() {
      return myTooltipText;
    }

    @Override
    public Alignment getAlignment() {
      return myAlignment;
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!super.equals(o)) return false;

      final MyNavigationGutterIconRenderer that = (MyNavigationGutterIconRenderer)o;

      if (myAlignment != that.myAlignment) return false;
      if (myIcon != null ? !myIcon.equals(that.myIcon) : that.myIcon != null) return false;
      if (myTooltipText != null ? !myTooltipText.equals(that.myTooltipText) : that.myTooltipText != null) return false;

      return true;
    }

    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (myAlignment != null ? myAlignment.hashCode() : 0);
      result = 31 * result + (myIcon != null ? myIcon.hashCode() : 0);
      result = 31 * result + (myTooltipText != null ? myTooltipText.hashCode() : 0);
      return result;
    }
  }
}
