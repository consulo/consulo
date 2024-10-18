/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.editor.gutter;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.MarkupEditorFilter;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.SeparatorPlacement;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.util.TextRange;
import consulo.language.editor.Pass;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;

public class LineMarkerInfo<T extends PsiElement> {
    @Nonnull
    @RequiredReadAction
    public static LineMarkerInfo<PsiElement> createMethodSeparatorLineMarker(
        @Nonnull PsiElement startFrom,
        @Nonnull EditorColorsManager colorsManager
    ) {
        LineMarkerInfo<PsiElement> info = new LineMarkerInfo<>(
            startFrom,
            startFrom.getTextRange(),
            null,
            Pass.LINE_MARKERS,
            null,
            null,
            GutterIconRenderer.Alignment.RIGHT
        );
        EditorColorsScheme scheme = colorsManager.getGlobalScheme();
        info.separatorColor = scheme.getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR);
        info.separatorPlacement = SeparatorPlacement.TOP;
        return info;
    }

    protected final Image myIcon;
    private final SmartPsiElementPointer<T> elementRef;
    public final int startOffset;
    public final int endOffset;
    public ColorValue separatorColor;
    public SeparatorPlacement separatorPlacement;
    public RangeHighlighter highlighter;

    public final int updatePass;
    @Nullable
    private final Function<? super T, String> myTooltipProvider;
    private AnAction myNavigateAction = new NavigateAction<>(this);
    @Nonnull
    private final GutterIconRenderer.Alignment myIconAlignment;
    @Nullable
    private final GutterIconNavigationHandler<T> myNavigationHandler;

    /**
     * Creates a line marker info for the element.
     *
     * @param element         the element for which the line marker is created.
     * @param range           the range (relative to beginning of file) with which the marker is associated
     * @param icon            the icon to show in the gutter for the line marker
     * @param updatePass      the ID of the daemon pass during which the marker should be recalculated
     * @param tooltipProvider the callback to calculate the tooltip for the gutter icon
     * @param navHandler      the handler executed when the gutter icon is clicked
     */
    public LineMarkerInfo(
        @Nonnull T element,
        @Nonnull TextRange range,
        Image icon,
        int updatePass,
        @Nullable Function<? super T, String> tooltipProvider,
        @Nullable GutterIconNavigationHandler<T> navHandler,
        @Nonnull GutterIconRenderer.Alignment alignment
    ) {
        myIcon = icon;
        myTooltipProvider = tooltipProvider;
        myIconAlignment = alignment;
        elementRef = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
        myNavigationHandler = navHandler;
        startOffset = range.getStartOffset();
        endOffset = range.getEndOffset();
        this.updatePass = 11; //Pass.LINE_MARKERS;
    }

    /**
     * @deprecated use {@link LineMarkerInfo#LineMarkerInfo(PsiElement, TextRange, Image, int, Function, GutterIconNavigationHandler, GutterIconRenderer.Alignment)} instead
     */
    public LineMarkerInfo(
        @Nonnull T element,
        int startOffset,
        Image icon,
        int updatePass,
        @Nullable Function<? super T, String> tooltipProvider,
        @Nullable GutterIconNavigationHandler<T> navHandler,
        @Nonnull GutterIconRenderer.Alignment alignment
    ) {
        this(element, new TextRange(startOffset, startOffset), icon, updatePass, tooltipProvider, navHandler, alignment);
    }

    /**
     * @deprecated use {@link LineMarkerInfo#LineMarkerInfo(PsiElement, TextRange, Image, int, Function, GutterIconNavigationHandler, GutterIconRenderer.Alignment)} instead
     */
    public LineMarkerInfo(
        @Nonnull T element,
        int startOffset,
        Image icon,
        int updatePass,
        @Nullable Function<? super T, String> tooltipProvider,
        @Nullable GutterIconNavigationHandler<T> navHandler
    ) {
        this(element, startOffset, icon, updatePass, tooltipProvider, navHandler, GutterIconRenderer.Alignment.RIGHT);
    }

    @Nullable
    public GutterIconRenderer createGutterRenderer() {
        if (myIcon == null) {
            return null;
        }
        return new LineMarkerGutterIconRenderer<>(this);
    }

    @Nonnull
    public LocalizeValue getLineMarkerTooltipValue() {
        if (myTooltipProvider == null) {
            return LocalizeValue.empty();
        }
        T element = getElement();
        return element == null || !element.isValid() ? LocalizeValue.empty() : LocalizeValue.ofNullable(myTooltipProvider.apply(element));
    }

    @Nullable
    public String getLineMarkerTooltip() {
        if (myTooltipProvider == null) {
            return null;
        }
        T element = getElement();
        return element == null || !element.isValid() ? null : myTooltipProvider.apply(element);
    }

    @Nullable
    public T getElement() {
        return elementRef.getElement();
    }

    void setNavigateAction(@Nonnull AnAction navigateAction) {
        myNavigateAction = navigateAction;
    }

    @Nullable
    public GutterIconNavigationHandler<T> getNavigationHandler() {
        return myNavigationHandler;
    }

    @Nonnull
    public MarkupEditorFilter getEditorFilter() {
        return MarkupEditorFilter.EMPTY;
    }

    public static class LineMarkerGutterIconRenderer<T extends PsiElement> extends GutterIconRenderer {
        private final LineMarkerInfo<T> myInfo;

        public LineMarkerGutterIconRenderer(@Nonnull LineMarkerInfo<T> info) {
            myInfo = info;
        }

        public LineMarkerInfo<T> getLineMarkerInfo() {
            return myInfo;
        }

        @Override
        @Nonnull
        public Image getIcon() {
            return myInfo.myIcon;
        }

        @Override
        public AnAction getClickAction() {
            return myInfo.myNavigateAction;
        }

        @Override
        public boolean isNavigateAction() {
            return myInfo.myNavigationHandler != null;
        }

        @Nonnull
        @Override
        public LocalizeValue getTooltipValue() {
            try {
                return myInfo.getLineMarkerTooltipValue();
            }
            catch (IndexNotReadyException ignored) {
                return LocalizeValue.empty();
            }
        }

        @Nonnull
        @Override
        public Alignment getAlignment() {
            return myInfo.myIconAlignment;
        }

        protected boolean looksTheSameAs(@Nonnull LineMarkerGutterIconRenderer renderer) {
            return myInfo.getElement() != null &&
                renderer.myInfo.getElement() != null &&
                myInfo.getElement() == renderer.myInfo.getElement() &&
                Comparing.equal(myInfo.myTooltipProvider, renderer.myInfo.myTooltipProvider) &&
                Comparing.equal(myInfo.myIcon, renderer.myInfo.myIcon);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LineMarkerGutterIconRenderer && looksTheSameAs((LineMarkerGutterIconRenderer)obj);
        }

        @Override
        public int hashCode() {
            T element = myInfo.getElement();
            return element == null ? 0 : element.hashCode();
        }
    }

    @Override
    public String toString() {
        return "(" + startOffset + "," + endOffset + ") -> " + elementRef;
    }
}
