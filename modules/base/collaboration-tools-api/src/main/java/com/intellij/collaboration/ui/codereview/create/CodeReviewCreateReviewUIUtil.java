// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.create;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.comment.CodeReviewMarkdownEditor;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.colorScheme.EditorColorsManager;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.awt.JBTextArea;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.log.VcsCommitMetadata;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.util.function.IntSupplier;

public final class CodeReviewCreateReviewUIUtil {
    private CodeReviewCreateReviewUIUtil() {
    }

    private static Font getTitleFont() {
        return JBUI.Fonts.label(16f);
    }

    public static void applyDefaults(@Nonnull JBTextArea textArea) {
        textArea.setFont(getTitleFont());
        textArea.setBackground(UIUtil.getListBackground());
        textArea.setLineWrap(true);
    }

    public static @Nonnull JBTextArea createTitleEditor() {
        return createTitleEditor("");
    }

    public static @Nonnull JBTextArea createTitleEditor(@Nonnull String emptyText) {
        JBTextArea textArea = new JBTextArea(new SingleLineDocument());
        applyDefaults(textArea);
        textArea.getEmptyText().setText(emptyText);
        textArea.setPreferredSize(new Dimension(0, JBUI.scale(textArea.getFont().getSize() * 5)));
        CollaborationToolsUIUtil.registerFocusActions(textArea);
        return textArea;
    }

    public static @Nonnull Editor createTitleEditor(@Nonnull Project project, @Nls @Nonnull String emptyText) {
        Editor editor = CodeReviewMarkdownEditor.create(project, true, true);
        editor.getComponent().setFont(getTitleFont());

        if (editor instanceof EditorEx editorEx) {
            configurePlaceholder(editorEx, emptyText);
            setScrollbarsBackground(editorEx);
        }
        return editor;
    }

    public static @Nonnull Editor createDescriptionEditor(@Nonnull Project project, @Nls @Nonnull String emptyText) {
        Editor editor = CodeReviewMarkdownEditor.create(project, true, false);
        if (editor instanceof EditorEx editorEx) {
            configurePlaceholder(editorEx, emptyText);
            editorEx.setShowPlaceholderWhenFocused(true);
            setScrollbarsBackground(editorEx);
        }
        return editor;
    }

    private static void configurePlaceholder(@Nonnull EditorEx editor, @Nls @Nonnull String emptyText) {
        editor.setPlaceholder(emptyText);
        editor.setShowPlaceholderWhenFocused(true);
    }

    private static void setScrollbarsBackground(@Nonnull EditorEx editor) {
        JBColor editorBackground = JBColor.lazy(() -> EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
        if (editor.getScrollPane().getHorizontalScrollBar() != null) {
            editor.getScrollPane().getHorizontalScrollBar().setBackground(editorBackground);
        }
        if (editor.getScrollPane().getVerticalScrollBar() != null) {
            editor.getScrollPane().getVerticalScrollBar().setBackground(editorBackground);
        }
    }

    public static @Nonnull ListCellRenderer<VcsCommitMetadata> createCommitListCellRenderer() {
        return new CodeReviewTwoLinesCommitRenderer();
    }

    @ApiStatus.Internal
    public static @Nonnull JComponent createGenerationToolbarOverlay(
        @Nonnull JComponent editorPanel,
        @Nonnull ActionToolbar toolbar
    ) {
        return createGenerationToolbarOverlay(editorPanel, toolbar, null);
    }

    @ApiStatus.Internal
    public static @Nonnull JComponent createGenerationToolbarOverlay(
        @Nonnull JComponent editorPanel,
        @Nonnull ActionToolbar toolbar,
        @Nullable IntSupplier getSpacerWidth
    ) {
        JComponent buttonPanel = toolbar.getComponent();
        buttonPanel.setBorder(JBUI.Borders.empty());
        buttonPanel.setOpaque(false);
        buttonPanel.putClientProperty(ActionToolbarImpl.IMPORTANT_TOOLBAR_KEY, true);

        JBLayeredPane layeredPane = new JBLayeredPane();
        SpringLayout layout = new SpringLayout();
        layeredPane.setLayout(layout);

        layout.putConstraint(SpringLayout.NORTH, editorPanel, 0, SpringLayout.NORTH, layeredPane);
        layout.putConstraint(SpringLayout.SOUTH, editorPanel, 0, SpringLayout.SOUTH, layeredPane);
        layout.putConstraint(SpringLayout.EAST, editorPanel, 0, SpringLayout.EAST, layeredPane);
        layout.putConstraint(SpringLayout.WEST, editorPanel, 0, SpringLayout.WEST, layeredPane);
        layeredPane.add(editorPanel, (Object) 1);

        layout.putConstraint(SpringLayout.SOUTH, buttonPanel, 0, SpringLayout.SOUTH, layeredPane);

        if (getSpacerWidth != null) {
            // Needed to avoid overlapping with the editor's vertical scrollbar
            JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            spacer.setEnabled(false);
            spacer.setBorder(new AbstractBorder() {
                @Override
                public Insets getBorderInsets(Component c, Insets insets) {
                    super.getBorderInsets(c, insets);
                    insets.right = getSpacerWidth.getAsInt();
                    return insets;
                }
            });
            layout.putConstraint(SpringLayout.SOUTH, spacer, 0, SpringLayout.SOUTH, layeredPane);
            layout.putConstraint(SpringLayout.EAST, spacer, 0, SpringLayout.EAST, layeredPane);
            layeredPane.add(spacer, (Object) 0);

            layout.putConstraint(SpringLayout.EAST, buttonPanel, 0, SpringLayout.WEST, spacer);
        }
        else {
            layout.putConstraint(SpringLayout.EAST, buttonPanel, 0, SpringLayout.EAST, layeredPane);
        }

        layeredPane.add(buttonPanel, (Object) 2);
        return layeredPane;
    }
}
