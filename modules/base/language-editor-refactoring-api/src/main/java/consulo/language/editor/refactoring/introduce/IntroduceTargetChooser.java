// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.introduce;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.unwrap.ScopeHighlighter;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.awt.popup.GroupedItemsListRenderer;
import consulo.ui.ex.awt.popup.ListItemDescriptorAdapter;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public final class IntroduceTargetChooser {
    private IntroduceTargetChooser() {
    }

    public static <T extends PsiElement> void showChooser(@Nonnull Editor editor,
                                                          @Nonnull List<? extends T> expressions,
                                                          @Nonnull Consumer<? super T> callback,
                                                          @Nonnull Function<? super T, String> renderer) {
        showChooser(editor, expressions, callback, renderer, RefactoringBundle.message("introduce.target.chooser.expressions.title"));
    }

    public static <T extends PsiElement> void showChooser(@Nonnull Editor editor,
                                                          @Nonnull List<? extends T> expressions,
                                                          @Nonnull Consumer<? super T> callback,
                                                          @Nonnull Function<? super T, String> renderer,
                                                          @Nonnull String title) {
        showChooser(editor, expressions, callback, renderer, title, ScopeHighlighter.NATURAL_RANGER);
    }

    public static <T extends PsiElement> void showChooser(@Nonnull Editor editor,
                                                          @Nonnull List<? extends T> expressions,
                                                          @Nonnull Consumer<? super T> callback,
                                                          @Nonnull Function<? super T, String> renderer,
                                                          @Nonnull String title,
                                                          @Nonnull Function<? super PsiElement, ? extends TextRange> ranger) {
        showChooser(editor, expressions, callback, renderer, title, -1, ranger);
    }

    public static <T extends PsiElement> void showChooser(@Nonnull Editor editor,
                                                          @Nonnull List<? extends T> expressions,
                                                          @Nonnull Consumer<? super T> callback,
                                                          @Nonnull Function<? super T, String> renderer,
                                                          @Nonnull String title,
                                                          int selection,
                                                          @Nonnull Function<? super PsiElement, ? extends TextRange> ranger) {

        ReadAction.nonBlocking(() -> ContainerUtil.map(expressions, t -> new MyIntroduceTarget<>(t, ranger.apply(t), renderer.apply(t))))
            .finishOnUiThread(Application::getNoneModalityState, targets ->
                showIntroduceTargetChooser(editor, targets, target -> callback.accept(target.getPlace()), title, selection))
            .expireWhen(editor::isDisposed)
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    public static <T extends IntroduceTarget> void showIntroduceTargetChooser(@Nonnull Editor editor,
                                                                              @Nonnull List<? extends T> expressions,
                                                                              @Nonnull Consumer<? super T> callback,
                                                                              @Nonnull String title,
                                                                              int selection) {
        showIntroduceTargetChooser(editor, expressions, callback, title, null, selection);
    }

    public static <T extends IntroduceTarget> void showIntroduceTargetChooser(@Nonnull Editor editor,
                                                                              @Nonnull List<? extends T> expressions,
                                                                              @Nonnull Consumer<? super T> callback,
                                                                              @Nonnull String title,
                                                                              @Nullable JComponent southComponent,
                                                                              int selection) {
        AtomicReference<ScopeHighlighter> highlighter = new AtomicReference<>(new ScopeHighlighter(editor));

        IPopupChooserBuilder<T> builder = JBPopupFactory.getInstance().<T>createPopupChooserBuilder(expressions)
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            .setSelectedValue(expressions.get(selection > -1 ? selection : 0), true)
            .setAccessibleName(title)
            .setTitle(title)
            .setMovable(false)
            .setResizable(false)
            .setRequestFocus(true)
            .setItemSelectedCallback(expr -> {
                ScopeHighlighter h = highlighter.get();
                if (h == null) return;
                h.dropHighlight();
                if (expr != null && expr.isValid()) {
                    TextRange range = expr.getTextRange();
                    h.highlight(Pair.create(range, Collections.singletonList(range)));
                }
            })
            .setItemChosenCallback(expr -> {
                if (expr.isValid()) {
                    callback.accept(expr);
                }
            })
            .addListener(new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                    highlighter.getAndSet(null).dropHighlight();
                }
            })
            .setRenderer(new GroupedItemsListRenderer<>(new ListItemDescriptorAdapter<T>() {
                @Override
                public String getTextFor(T value) {
                    String text = value.render();
                    int firstNewLinePos = text.indexOf('\n');
                    String trimmedText = text.substring(0, firstNewLinePos != -1 ? firstNewLinePos : Math.min(100, text.length()));
                    if (trimmedText.length() != text.length()) trimmedText += " ...";
                    return trimmedText;
                }
            }));

        if (southComponent != null) {
            builder.setSouthComponent(southComponent);
        }

        JBPopup popup = builder.createPopup();
        EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, popup);
        Project project = editor.getProject();
        if (project != null && !popup.isDisposed()) {
            PopupNavigationUtil.hidePopupIfDumbModeStarts(popup, project);
        }
    }

    private static final class MyIntroduceTarget<T extends PsiElement> extends PsiIntroduceTarget<T> {
        private final TextRange myTextRange;
        private final String myText;

        MyIntroduceTarget(@Nonnull T psi, @Nonnull TextRange range, @Nonnull String text) {
            super(psi);
            myTextRange = range;
            myText = text;
        }

        @RequiredReadAction
        @Override
        public @Nonnull TextRange getTextRange() {
            return myTextRange;
        }

        @RequiredReadAction
        @Override
        public @Nonnull String render() {
            return myText;
        }

        @Override
        public String toString() {
            return myText;
        }
    }
}
