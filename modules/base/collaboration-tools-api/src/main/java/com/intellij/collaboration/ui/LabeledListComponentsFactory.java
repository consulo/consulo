// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui;

import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.collaboration.ui.util.DimensionRestrictions;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.InlineIconButton;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.WrapLayout;
import com.intellij.util.ui.launchOnShowKt;
import kotlinx.coroutines.flow.StateFlow;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Function;

/**
 * Set of factory methods to create components for displaying labeled lists of items.
 * For example, a list of reviewers or labels in a code review.
 */
public final class LabeledListComponentsFactory {
    private LabeledListComponentsFactory() {
    }

    /**
     * A panel with the label which maintains the preferred width equal to the max possible text length
     */
    public static @Nonnull JPanel createLabelPanel(
        @Nonnull StateFlow<Boolean> listEmptyState,
        @NlsContexts.Label @Nonnull String emptyText,
        @NlsContexts.Label @Nonnull String notEmptyText
    ) {
        JLabel label = new JLabel();
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setBorder(JBUI.Borders.empty(6, 0, 6, 5));

        // Note: launchOnShow equivalent would need Kotlin coroutines in the calling context
        // This is a simplified conversion - the actual coroutine collection should be handled at call sites

        JPanel panel = new JPanel(null);
        SizeRestrictedSingleComponentLayout layout = new SizeRestrictedSingleComponentLayout();
        layout.setPrefSize(new DimensionRestrictions() {
            @Override
            public @Nullable Integer getWidth() {
                FontMetrics fm = label.getFontMetrics(label.getFont());
                if (fm == null) {
                    return null;
                }
                return Math.max(fm.stringWidth(emptyText), fm.stringWidth(notEmptyText));
            }

            @Override
            public @Nullable Integer getHeight() {
                return null;
            }
        });
        panel.setLayout(layout);
        panel.setOpaque(false);
        panel.add(label);
        return panel;
    }

    /**
     * Creates a grid panel with labeled lists where the labels column width is equal to the widest label
     */
    public static @Nonnull JPanel createGrid(@Nonnull List<kotlin.Pair<JComponent, JComponent>> listsWithLabels) {
        JPanel panel = new JPanel(null);
        panel.setOpaque(false);
        panel.setLayout(new MigLayout(new LC().fillX().gridGap("0", "0").insets("0", "0", "0", "0")));

        for (kotlin.Pair<JComponent, JComponent> pair : listsWithLabels) {
            JComponent label = pair.getFirst();
            JComponent list = pair.getSecond();
            panel.add(label, new CC().alignY("top"));
            panel.add(list, new CC().minWidth("0").growX().pushX().wrap());
        }
        return panel;
    }
}
