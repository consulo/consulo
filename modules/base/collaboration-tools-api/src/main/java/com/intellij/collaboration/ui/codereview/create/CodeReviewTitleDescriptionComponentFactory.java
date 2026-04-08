// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.create;

import com.intellij.collaboration.async.AsyncUtilKt;
import com.intellij.collaboration.ui.LoadingLabel;
import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.collaboration.ui.util.DimensionRestrictions;
import com.intellij.collaboration.ui.util.SwingBindingsKt;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public final class CodeReviewTitleDescriptionComponentFactory {
    private static final int EDITOR_MARGINS = 12;
    private static final int EDITORS_GAP = 8;

    private CodeReviewTitleDescriptionComponentFactory() {
    }

    public static @Nonnull JPanel createIn(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewTitleDescriptionViewModel vm,
        @Nonnull Editor titleEditor,
        @Nonnull Editor descriptionEditor
    ) {
        JPanel textPanel = new JPanel(null) {
            @Override
            public void addNotify() {
                super.addNotify();
                InternalDecoratorImpl.componentWithEditorBackgroundAdded(this);
            }

            @Override
            public void removeNotify() {
                super.removeNotify();
                InternalDecoratorImpl.componentWithEditorBackgroundRemoved(this);
            }
        };
        textPanel.setOpaque(true);
        textPanel.setBackground(JBColor.lazy(() -> EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground()));
        InternalDecoratorImpl.preventRecursiveBackgroundUpdateOnToolwindow(textPanel);

        AsyncUtilKt.launchNow(cs, continuation -> {
            vm.getIsTemplateLoading().collect(isLoading -> {
                textPanel.removeAll();
                if (isLoading) {
                    textPanel.setLayout(new SingleComponentCenteringLayout());
                    textPanel.add(new LoadingLabel());
                }
                else {
                    textPanel.setLayout(new BorderLayout(0, JBUI.scale(EDITORS_GAP)));
                    textPanel.add(withMinHeightOfEditorLine(titleEditor.getComponent(), titleEditor, 0), BorderLayout.NORTH);
                    textPanel.add(
                        withMinHeightOfEditorLine(descriptionEditor.getComponent(), descriptionEditor, EDITOR_MARGINS),
                        BorderLayout.CENTER
                    );
                }
                textPanel.revalidate();
                textPanel.repaint();
                return null;
            }, continuation);
            return null;
        });

        return textPanel;
    }

    public static @Nonnull Editor createTitleEditorIn(
        @Nonnull Project project,
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewTitleDescriptionViewModel vm,
        @Nls @Nonnull String titlePlaceholder
    ) {
        Editor editor = CodeReviewCreateReviewUIUtil.createTitleEditor(project, titlePlaceholder);
        setEditorMargins(editor, JBUI.insets(EDITOR_MARGINS, EDITOR_MARGINS, 0, EDITOR_MARGINS));
        kotlinx.coroutines.CoroutineScopeKt.launch(cs, Dispatchers.getMain(), CoroutineStart.ATOMIC, (scope, continuation) -> {
            try {
                SwingBindingsKt.bindTextIn(editor.getDocument(), scope, vm.getTitleText(), vm::setTitle);
                kotlinx.coroutines.AwaitCancellationKt.awaitCancellation();
            }
            finally {
                EditorFactory.getInstance().releaseEditor(editor);
            }
            return null;
        });
        return editor;
    }

    public static @Nonnull Editor createDescriptionEditorIn(
        @Nonnull Project project,
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewTitleDescriptionViewModel vm,
        @Nls @Nonnull String descriptionPlaceholder
    ) {
        Editor editor = CodeReviewCreateReviewUIUtil.createDescriptionEditor(project, descriptionPlaceholder);
        setEditorMargins(editor, JBUI.insets(0, EDITOR_MARGINS));
        kotlinx.coroutines.CoroutineScopeKt.launch(
            cs,
            Dispatchers.getMain(),
            CoroutineStart.ATOMIC,
            (scope, continuation) -> {
                try {
                    SwingBindingsKt.bindTextIn(editor.getDocument(), scope, vm.getDescriptionText(), vm::setDescription);
                    kotlinx.coroutines.AwaitCancellationKt.awaitCancellation();
                }
                finally {
                    EditorFactory.getInstance().releaseEditor(editor);
                }
                return null;
            }
        );
        return editor;
    }

    private static void setEditorMargins(@Nonnull Editor editor, @Nonnull Insets margins) {
        if (editor instanceof EditorEx editorEx) {
            editorEx.getScrollPane().setViewportBorder(JBUI.Borders.empty(margins));
        }
    }

    private static @Nonnull JPanel withMinHeightOfEditorLine(
        @Nonnull Component component,
        @Nonnull Editor editor,
        int additionalGapBottom
    ) {
        DimensionRestrictions restrictions = new DimensionRestrictions() {
            @Override
            public @Nullable Integer getWidth() {
                return null;
            }

            @Override
            public @Nonnull Integer getHeight() {
                Insets editorInsets = editor.getComponent().getInsets();
                Insets editorMargins = getEditorMargins(editor);
                return editor.getLineHeight() +
                    JBUI.scale(additionalGapBottom) +
                    editorInsets.top + editorInsets.bottom +
                    editorMargins.top + editorMargins.bottom;
            }
        };
        return withMinSize(component, restrictions);
    }

    private static @Nonnull Insets getEditorMargins(@Nonnull Editor editor) {
        if (editor instanceof EditorEx editorEx) {
            var border = editorEx.getScrollPane().getViewportBorder();
            if (border != null) {
                return border.getBorderInsets(editorEx.getScrollPane().getViewport());
            }
        }
        return JBUI.emptyInsets();
    }

    private static @Nonnull JPanel withMinSize(@Nonnull Component component, @Nonnull DimensionRestrictions restrictions) {
        JPanel panel = new JPanel(null);
        panel.setOpaque(false);
        SizeRestrictedSingleComponentLayout layout = new SizeRestrictedSingleComponentLayout();
        layout.setMinSize(restrictions);
        panel.setLayout(layout);
        panel.add(component);
        return panel;
    }
}
