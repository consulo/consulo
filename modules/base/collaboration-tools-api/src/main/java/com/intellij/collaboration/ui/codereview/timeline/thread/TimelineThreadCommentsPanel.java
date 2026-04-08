// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.timeline.thread;

import com.intellij.collaboration.ui.SingleValueModel;
import consulo.application.AllIcons;
import consulo.codeEditor.Editor;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.dataContext.DataSink;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shows thread items with folding if there are more than {@link #FOLD_THRESHOLD} of them
 */
public final class TimelineThreadCommentsPanel<T> extends BorderLayoutPanel implements UiDataProvider {
    public static final int FOLD_THRESHOLD = 3;
    public static final int UNFOLD_BUTTON_VERTICAL_GAP = 18;

    private final ListModel<T> commentsModel;
    private final Function<T, JComponent> commentComponentFactory;
    private final int foldButtonOffset;

    public final SingleValueModel<Boolean> foldModel = new SingleValueModel<>(true);
    private final SingleValueModel<Integer> collapsedCountModel;
    private final JComponent unfoldButtonPanel;
    private final FoldablePanel foldablePanel;

    public TimelineThreadCommentsPanel(
        @Nonnull ListModel<T> commentsModel,
        @Nonnull Function<T, JComponent> commentComponentFactory,
        int offset,
        int foldButtonOffset
    ) {
        this.commentsModel = commentsModel;
        this.commentComponentFactory = commentComponentFactory;
        this.foldButtonOffset = foldButtonOffset;
        this.collapsedCountModel = new SingleValueModel<>(commentsModel.getSize() - FOLD_THRESHOLD - 1);

        commentsModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                collapsedCountModel.setValue(commentsModel.getSize() - FOLD_THRESHOLD - 1);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                collapsedCountModel.setValue(commentsModel.getSize() - FOLD_THRESHOLD - 1);
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                collapsedCountModel.setValue(commentsModel.getSize() - FOLD_THRESHOLD - 1);
            }
        });

        this.unfoldButtonPanel = createUnfoldPanel(collapsedCountModel);

        this.foldablePanel = new FoldablePanel(unfoldButtonPanel, offset);
        for (int i = 0; i < commentsModel.getSize(); i++) {
            foldablePanel.addComponent(commentComponentFactory.apply(commentsModel.getElementAt(i)), i);
        }

        setOpaque(false);
        addToCenter(foldablePanel);

        commentsModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalRemoved(ListDataEvent e) {
                for (int i = e.getIndex1(); i >= e.getIndex0(); i--) {
                    foldablePanel.removeComponent(i);
                }
                updateFolding(foldModel.getValue());
                foldablePanel.revalidate();
                foldablePanel.repaint();
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    foldablePanel.addComponent(commentComponentFactory.apply(commentsModel.getElementAt(i)), i);
                }
                foldablePanel.revalidate();
                foldablePanel.repaint();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                for (int i = e.getIndex1(); i >= e.getIndex0(); i--) {
                    foldablePanel.removeComponent(i);
                }
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    foldablePanel.addComponent(commentComponentFactory.apply(commentsModel.getElementAt(i)), i);
                }
                foldablePanel.validate();
                foldablePanel.repaint();
            }
        });

        foldModel.addListener(this::updateFolding);
        updateFolding(foldModel.getValue());
    }

    public TimelineThreadCommentsPanel(
        @Nonnull ListModel<T> commentsModel,
        @Nonnull Function<T, JComponent> commentComponentFactory
    ) {
        this(commentsModel, commentComponentFactory, JBUI.scale(8), 30);
    }

    @Override
    public void uiDataSnapshot(@Nonnull DataSink sink) {
        sink.setNull(Editor.KEY);
    }

    private void updateFolding(boolean folded) {
        boolean shouldFold = folded && commentsModel.getSize() > FOLD_THRESHOLD;
        unfoldButtonPanel.setVisible(shouldFold);

        if (commentsModel.getSize() == 0) {
            return;
        }

        foldablePanel.getModelComponent(0).setVisible(true);
        foldablePanel.getModelComponent(commentsModel.getSize() - 1).setVisible(true);

        for (int i = 1; i < commentsModel.getSize() - 1; i++) {
            foldablePanel.getModelComponent(i).setVisible(!shouldFold);
        }
    }

    /**
     * {@link FoldablePanel} hides unfoldButton and allows to use this panel like it doesn't contain it
     */
    private static final class FoldablePanel extends JPanel {
        private final JComponent unfoldButton;

        FoldablePanel(@Nonnull JComponent unfoldButton, int offset) {
            super(ListLayout.vertical(offset));
            this.unfoldButton = unfoldButton;
            setOpaque(false);
            add(unfoldButton);
        }

        void addComponent(@Nonnull JComponent component, int index) {
            remove(unfoldButton);
            add(component, null, index);
            add(unfoldButton, null, 1);
        }

        void removeComponent(int index) {
            remove(unfoldButton);
            remove(index);

            int unfoldButtonIndex = getComponents().length == 0 ? 0 : 1;
            add(unfoldButton, null, unfoldButtonIndex);
        }

        @Nonnull
        Component getModelComponent(int modelIndex) {
            if (modelIndex == 0) {
                return getComponent(0);
            }
            else {
                return getComponent(modelIndex + 1);
            }
        }
    }

    private @Nonnull JComponent createUnfoldPanel(@Nonnull SingleValueModel<Integer> foldedCount) {
        BorderLayoutPanel panel = new BorderLayoutPanel();
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.emptyLeft(foldButtonOffset));
        panel.addToLeft(createUnfoldComponent(foldedCount, e -> foldModel.setValue(!foldModel.getValue())));
        return panel;
    }

    public static @Nonnull JComponent createUnfoldComponent(
        int foldedCount,
        @Nonnull Consumer<ActionEvent> actionListener
    ) {
        return createUnfoldComponent(new SingleValueModel<>(foldedCount), actionListener);
    }

    private static @Nonnull JComponent createUnfoldComponent(
        @Nonnull SingleValueModel<Integer> foldedCount,
        @Nonnull Consumer<ActionEvent> actionListener
    ) {
        ActionLink link = new ActionLink("", e -> actionListener.accept(e));
        link.setIcon(AllIcons.Actions.MoreHorizontal);
        link.setBorder(JBUI.Borders.empty(UNFOLD_BUTTON_VERTICAL_GAP, 0));

        foldedCount.addAndInvokeListener(count -> link.setText(CollaborationToolsLocalize.reviewThreadMoreReplies(count).get()));

        return link;
    }
}
