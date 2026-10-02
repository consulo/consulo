// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.util;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.editor.ui.navigation.NavigationGutterIconRenderer;
import consulo.language.editor.ui.navigation.TargetPresentationProvider;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.RelativePoint2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class UsagesNavigationGutterIconBuilder<T> extends NavigationGutterIconBuilder<T> {
    public UsagesNavigationGutterIconBuilder(
        Image icon,
        Function<T, Collection<? extends PsiElement>> converter,
        @Nullable Function<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider
    ) {
        super(icon, converter, gotoRelatedItemProvider);
        setAlignment(GutterIconRenderer.Alignment.LEFT);
    }

    public static <T> UsagesNavigationGutterIconBuilder<T> create(Image icon, Function<T, Collection<? extends PsiElement>> converter) {
        return create(icon, converter, null);
    }

    public static <T> UsagesNavigationGutterIconBuilder<T> create(
        Image icon,
        Function<T, Collection<? extends PsiElement>> converter,
        @Nullable Function<T, Collection<? extends GotoRelatedItem>> gotoRelatedItemProvider
    ) {
        return new UsagesNavigationGutterIconBuilder<>(icon, converter, gotoRelatedItemProvider);
    }

    @Override
    protected NavigationGutterIconRenderer createGutterIconRenderer(
        Supplier<List<SmartPsiElementPointer>> pointers,
        @Nullable TargetPresentationProvider<PsiElement> presentationProvider,
        boolean empty,
        @Nullable GutterIconNavigationHandler<PsiElement> navigationHandler
    ) {
        return new UsagesNavigationGutterIconRenderer(this, myAlignment, myIcon, myTooltipText, pointers, presentationProvider, empty);
    }

    @RequiredUIAccess
    private static void showUsages(ComponentEvent<?> event, PsiElement element) {
        Project project = element.getProject();
        if (DumbService.getInstance(project).isDumb()) {
            return;
        }
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        RelativePoint2D popupPosition = event.getInputDetails() instanceof ProgrammaticInputDetails
            ? EditorPopupHelper.getInstance().guessBestPopupLocation(editor)
            : RelativePoint2D.of(event);

        LanguageEditorInternalHelper.getInstance().startFindUsages(editor, project, element, popupPosition);
    }

    private static class UsagesNavigationGutterIconRenderer extends NavigationGutterIconRenderer {
        private final GutterIconRenderer.Alignment myAlignment;
        private final Image myIcon;
        private final LocalizeValue myTooltipText;
        private final boolean myEmpty;

        UsagesNavigationGutterIconRenderer(
            UsagesNavigationGutterIconBuilder<?> builder,
            GutterIconRenderer.Alignment alignment,
            Image icon,
            LocalizeValue tooltipText,
            Supplier<List<SmartPsiElementPointer>> pointers,
            @Nullable TargetPresentationProvider<PsiElement> presentationProvider,
            boolean empty
        ) {
            super(
                builder.myPopupTitle,
                builder.myEmptyText,
                presentationProvider,
                pointers,
                UsagesNavigationGutterIconBuilder::showUsages
            );
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
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || !super.equals(other)) {
                return false;
            }
            UsagesNavigationGutterIconRenderer that = (UsagesNavigationGutterIconRenderer) other;
            if (myAlignment != that.myAlignment) {
                return false;
            }
            if (!myIcon.equals(that.myIcon)) {
                return false;
            }
            return myTooltipText.equals(that.myTooltipText);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + myAlignment.hashCode();
            result = 31 * result + myIcon.hashCode();
            result = 31 * result + myTooltipText.hashCode();
            return result;
        }
    }
}
