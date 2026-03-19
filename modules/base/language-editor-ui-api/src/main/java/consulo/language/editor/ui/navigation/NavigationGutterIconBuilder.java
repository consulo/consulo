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
package consulo.language.editor.ui.navigation;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.presentation.TypePresentationService;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.editor.Pass;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.ui.DefaultPsiElementCellRenderer;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * DOM-specific builder for {@link GutterIconRenderer}
 * and {@link LineMarkerInfo}.
 *
 * @author peter
 */
public class NavigationGutterIconBuilder<T> {
    private static final String PATTERN = "&nbsp;&nbsp;&nbsp;&nbsp;{0}";
    protected static final Function<PsiElement, Collection<? extends PsiElement>> DEFAULT_PSI_CONVERTOR =
        ContainerUtil::createMaybeSingletonList;

    protected static final Function DEFAULT_NAMER = dom -> TypePresentationService.getInstance().getTypeNameOrStub(dom);

    private final Image myIcon;
    private final Function<T, Collection<? extends PsiElement>> myConverter;

    protected Supplier<Collection<? extends T>> myTargets;
    protected boolean myLazy;
    
    private LocalizeValue myTooltipText = LocalizeValue.empty();
    
    private LocalizeValue myPopupTitle = LocalizeValue.empty();
    
    private LocalizeValue myEmptyText = LocalizeValue.empty();
    
    private LocalizeValue myTooltipTitle = LocalizeValue.empty();
    private GutterIconRenderer.Alignment myAlignment = GutterIconRenderer.Alignment.CENTER;
    private Supplier<PsiElementListCellRenderer> myCellRenderer;
    private Function<T, String> myNamer = createDefaultNamer();
    private final Function<T, Collection<? extends GotoRelatedItem>> myGotoRelatedItemProvider;

    public static final Function<PsiElement, Collection<? extends GotoRelatedItem>> PSI_GOTO_RELATED_ITEM_PROVIDER =
        dom -> Collections.singletonList(new GotoRelatedItem(dom, "XML"));

    protected NavigationGutterIconBuilder(Image icon, Function<T, Collection<? extends PsiElement>> converter) {
        this(icon, converter, null);
    }

    protected NavigationGutterIconBuilder(
        Image icon,
        Function<T, Collection<? extends PsiElement>> converter,
        @Nullable Function<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider
    ) {
        myIcon = icon;
        myConverter = converter;
        myGotoRelatedItemProvider = gotoRelatedItemProvider;
    }

    public static NavigationGutterIconBuilder<PsiElement> create(Image icon) {
        return create(icon, DEFAULT_PSI_CONVERTOR, PSI_GOTO_RELATED_ITEM_PROVIDER);
    }

    public static <T> NavigationGutterIconBuilder<T> create(
        Image icon,
        Function<T, Collection<? extends PsiElement>> converter
    ) {
        return create(icon, converter, null);
    }

    public static <T> NavigationGutterIconBuilder<T> create(
        Image icon,
        Function<T, Collection<? extends PsiElement>> converter,
        @Nullable Function<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider
    ) {
        return new NavigationGutterIconBuilder<>(icon, converter, gotoRelatedItemProvider);
    }

    public NavigationGutterIconBuilder<T> setTarget(@Nullable T target) {
        return setTargets(ContainerUtil.createMaybeSingletonList(target));
    }

    @SafeVarargs
    public final NavigationGutterIconBuilder<T> setTargets(T... targets) {
        return setTargets(Arrays.asList(targets));
    }

    public NavigationGutterIconBuilder<T> setTargets(Supplier<Collection<? extends T>> targets) {
        myTargets = targets;
        myLazy = true;
        return this;
    }

    public NavigationGutterIconBuilder<T> setTargets(Collection<? extends T> targets) {
        myTargets = LazyValue.notNull(() -> targets);
        return this;
    }

    public NavigationGutterIconBuilder<T> setTooltipText(LocalizeValue tooltipText) {
        myTooltipText = tooltipText;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public NavigationGutterIconBuilder<T> setTooltipText(String tooltipText) {
        myTooltipText = LocalizeValue.of(tooltipText);
        return this;
    }

    public NavigationGutterIconBuilder<T> setAlignment(GutterIconRenderer.Alignment alignment) {
        myAlignment = alignment;
        return this;
    }

    public NavigationGutterIconBuilder<T> setPopupTitle(LocalizeValue popupTitle) {
        myPopupTitle = popupTitle;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public NavigationGutterIconBuilder<T> setPopupTitle(String popupTitle) {
        myPopupTitle = LocalizeValue.of(popupTitle);
        return this;
    }

    public NavigationGutterIconBuilder<T> setEmptyPopupText(LocalizeValue emptyText) {
        myEmptyText = emptyText;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public NavigationGutterIconBuilder<T> setEmptyPopupText(String emptyText) {
        myEmptyText = LocalizeValue.of(emptyText);
        return this;
    }

    public NavigationGutterIconBuilder<T> setTooltipTitle(LocalizeValue tooltipTitle) {
        myTooltipTitle = tooltipTitle;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public NavigationGutterIconBuilder<T> setTooltipTitle(String tooltipTitle) {
        return setTooltipTitle(LocalizeValue.of(tooltipTitle));
    }

    public NavigationGutterIconBuilder<T> setNamer(Function<T, String> namer) {
        myNamer = namer;
        return this;
    }

    public NavigationGutterIconBuilder<T> setCellRenderer(PsiElementListCellRenderer cellRenderer) {
        myCellRenderer = () -> cellRenderer;
        return this;
    }

    @SuppressWarnings("unchecked")
    protected Function<T, String> createDefaultNamer() {
        return DEFAULT_NAMER;
    }

    public @Nullable Annotation install(AnnotationHolder holder, @Nullable PsiElement element) {
        if (!myLazy && myTargets.get().isEmpty() || element == null) {
            return null;
        }
        return doInstall(holder.createInfoAnnotation(element, null), element.getProject());
    }

    protected Annotation doInstall(Annotation annotation, Project project) {
        MyNavigationGutterIconRenderer renderer = createGutterIconRenderer(project);
        annotation.setGutterIconRenderer(renderer);
        annotation.setNeedsUpdateOnTyping(false);
        return annotation;
    }

    @RequiredReadAction
    public RelatedItemLineMarkerInfo<PsiElement> createLineMarkerInfo(PsiElement element) {
        MyNavigationGutterIconRenderer renderer = createGutterIconRenderer(element.getProject());
        LocalizeValue tooltip = renderer.getTooltipValue();
        Supplier<Collection<? extends GotoRelatedItem>> gotoTargets = LazyValue.notNull(() -> {
            if (myGotoRelatedItemProvider != null) {
                return ContainerUtil.concat(myTargets.get(), myGotoRelatedItemProvider);
            }
            return Collections.emptyList();
        });
        return new RelatedItemLineMarkerInfo<>(
            element,
            element.getTextRange(),
            renderer.getIcon(),
            Pass.LINE_MARKERS,
            tooltip.isEmpty() ? null : i -> tooltip.get(),
            renderer.isNavigateAction() ? renderer : null,
            renderer.getAlignment(),
            gotoTargets
        );
    }

    private void checkBuilt() {
        assert myTargets != null : "Must have called .setTargets() before calling create()";
    }

    private MyNavigationGutterIconRenderer createGutterIconRenderer(Project project) {
        checkBuilt();
        SmartPointerManager manager = SmartPointerManager.getInstance(project);

        Supplier<List<SmartPsiElementPointer>> pointers = LazyValue.notNull(() -> {
            Set<PsiElement> elements = new HashSet<>();
            Collection<? extends T> targets = myTargets.get();
            List<SmartPsiElementPointer> list = new ArrayList<>(targets.size());
            for (T target : targets) {
                for (PsiElement psiElement : myConverter.apply(target)) {
                    if (elements.add(psiElement) && psiElement.isValid()) {
                        list.add(manager.createSmartPsiElementPointer(psiElement));
                    }
                }
            }
            return list;
        });

        boolean empty = isEmpty();

        if (myTooltipText.isEmpty() && !myLazy) {
            SortedSet<String> names = new TreeSet<>();
            for (T t : myTargets.get()) {
                String text = myNamer.apply(t);
                if (text != null) {
                    names.add(MessageFormat.format(PATTERN, text));
                }
            }
            StringBuilder sb = new StringBuilder("<html><body>");
            if (myTooltipTitle != null) {
                sb.append(myTooltipTitle).append("<br>");
            }
            for (String name : names) {
                sb.append(name).append("<br>");
            }
            sb.append("</body></html>");
            myTooltipText = LocalizeValue.of(sb.toString());
        }

        Supplier<PsiElementListCellRenderer> renderer = myCellRenderer == null ? DefaultPsiElementCellRenderer::new : myCellRenderer;
        return new MyNavigationGutterIconRenderer(this, myAlignment, myIcon, myTooltipText, pointers, renderer, empty);
    }

    private boolean isEmpty() {
        if (myLazy) {
            return false;
        }

        Set<PsiElement> elements = new HashSet<>();
        Collection<? extends T> targets = myTargets.get();
        for (T target : targets) {
            for (PsiElement psiElement : myConverter.apply(target)) {
                if (elements.add(psiElement)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static class MyNavigationGutterIconRenderer extends NavigationGutterIconRenderer {
        private final GutterIconRenderer.Alignment myAlignment;
        private final Image myIcon;
        private final LocalizeValue myTooltipText;
        private final boolean myEmpty;

        public MyNavigationGutterIconRenderer(
            NavigationGutterIconBuilder builder,
            GutterIconRenderer.Alignment alignment,
            Image icon,
            LocalizeValue tooltipText,
            Supplier<List<SmartPsiElementPointer>> pointers,
            Supplier<PsiElementListCellRenderer> cellRenderer,
            boolean empty
        ) {
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
        
        public Image getIcon() {
            return myIcon;
        }

        
        @Override
        public LocalizeValue getTooltipValue() {
            return myTooltipText;
        }

        
        @Override
        public GutterIconRenderer.Alignment getAlignment() {
            return myAlignment;
        }

        @Override
        public boolean equals(Object o) {
            return this == o
                || super.equals(o)
                && o instanceof MyNavigationGutterIconRenderer that
                && myAlignment == that.myAlignment
                && Objects.equals(myIcon, that.myIcon)
                && Objects.equals(myTooltipText, that.myTooltipText);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (myAlignment != null ? myAlignment.hashCode() : 0);
            result = 31 * result + (myIcon != null ? myIcon.hashCode() : 0);
            result = 31 * result + (myTooltipText != null ? myTooltipText.hashCode() : 0);
            return result;
        }
    }
}
