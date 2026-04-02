package com.intellij.collaboration.ui.codereview.create;

import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SideBorder;
import consulo.ui.ex.awt.UIUtil;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.HideMode;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Use a static method to construct
 */
public final class CodeReviewCreateReviewLayoutBuilder {
    private static final int BASE_GAP = 12;

    private boolean addSeparator = false;
    private final List<ComponentWithConstraints> componentsWithConstraints = new ArrayList<>();

    @Internal
    public CodeReviewCreateReviewLayoutBuilder() {
    }

    public @Nonnull CodeReviewCreateReviewLayoutBuilder addComponent(@Nonnull JComponent component) {
        return addComponent(component, false, null, false, true);
    }

    public @Nonnull CodeReviewCreateReviewLayoutBuilder addComponent(
        @Nonnull JComponent component,
        boolean zeroMinWidth,
        Float stretchYWithWeight,
        boolean withoutBorder,
        boolean withListBackground
    ) {
        CC cc = new CC().growX().pushX();
        if (zeroMinWidth) {
            cc.minWidth("0");
        }
        if (stretchYWithWeight != null) {
            cc.growY(stretchYWithWeight).pushY(stretchYWithWeight);
        }
        componentsWithConstraints.add(new ComponentWithConstraints(component, cc));
        setupBorderAndBackground(component, withoutBorder, withListBackground);
        return this;
    }

    public @Nonnull CodeReviewCreateReviewLayoutBuilder addSeparator() {
        addSeparator = true;
        return this;
    }

    private void setupBorderAndBackground(@Nonnull JComponent component, boolean withoutBorder, boolean withListBackground) {
        if (withListBackground) {
            component.setBackground(UIUtil.getListBackground());
        }

        if (!withoutBorder) {
            component.setBorder(JBUI.Borders.empty(BASE_GAP));
        }

        if (addSeparator) {
            addSeparator = false;
            component.setBorder(BorderFactory.createCompoundBorder(
                IdeBorderFactory.createBorder(SideBorder.TOP),
                component.getBorder()
            ));
        }
    }

    public @Nonnull JComponent build() {
        JPanel panel = new JPanel(null);
        panel.setBackground(UIUtil.getListBackground());
        panel.setLayout(new MigLayout(new LC().gridGap("0", "0").insets("0").fill().flowY().hideMode(HideMode.DISREGARD)));
        panel.setFocusCycleRoot(true);
        for (ComponentWithConstraints cwc : componentsWithConstraints) {
            panel.add(cwc.component, cwc.cc);
        }
        return panel;
    }

    public static @Nonnull CodeReviewCreateReviewLayoutBuilder create() {
        return new CodeReviewCreateReviewLayoutBuilder();
    }

    private record ComponentWithConstraints(@Nonnull JComponent component, @Nonnull CC cc) {
    }
}
