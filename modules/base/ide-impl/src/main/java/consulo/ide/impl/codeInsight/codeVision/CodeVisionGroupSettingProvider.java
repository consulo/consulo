// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.codeVision.CodeVisionAnchorKind;
import consulo.language.editor.codeVision.CodeVisionSettings;
import consulo.language.editor.impl.internal.inlay.setting.InlayGroupSettingProvider;
import consulo.language.editor.inlay.InlayGroup;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Provides group-level settings for {@link InlayGroup#CODE_VISION_GROUP_NEW} in the
 * Inlay Hints settings panel.
 * <p>
 * Port of JB's {@code CodeVisionGlobalSettingsProvider}: manages the global enabled flag,
 * the default lens position (Top / Right), and the maximum number of visible lenses
 * above / next-to declarations.
 * <p>
 * Per-provider enable/disable is handled by the individual
 * {@link consulo.language.editor.impl.internal.inlay.setting.InlayProviderSettingsModel}
 * nodes contributed by {@link CodeVisionInlaySettingsProvider}.
 */
@ExtensionImpl
public class CodeVisionGroupSettingProvider implements InlayGroupSettingProvider {

    /** Positions the user may choose from (mirrors JB's {@code defaultAnchors}). */
    private static final CodeVisionAnchorKind[] DEFAULT_ANCHORS =
        {CodeVisionAnchorKind.Top, CodeVisionAnchorKind.Right};

    private final CodeVisionSettings mySettings = CodeVisionSettings.getInstance();

    // UI controls — created lazily in getComponent()
    private JComponent myComponent;
    private JComboBox<CodeVisionAnchorKind> myDefaultPositionComboBox;
    private JSpinner myVisibleMetricsAbove;
    private JSpinner myVisibleMetricsNext;

    // -------------------------------------------------------------------------
    // InlayGroupSettingProvider
    // -------------------------------------------------------------------------

    @Override
    public InlayGroup getGroup() {
        return InlayGroup.CODE_VISION_GROUP_NEW;
    }

    @Override
    public boolean isEnabled() {
        return mySettings.isCodeVisionEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        mySettings.setCodeVisionEnabled(enabled);
    }

    @Override
    public JComponent getComponent() {
        if (myComponent == null) {
            myComponent = buildComponent();
            reset();
        }
        return myComponent;
    }

    @Override
    public void apply() {
        mySettings.setDefaultPosition((CodeVisionAnchorKind) myDefaultPositionComboBox.getSelectedItem());
        mySettings.setVisibleMetricsAboveDeclarationCount((Integer) myVisibleMetricsAbove.getValue());
        mySettings.setVisibleMetricsNextToDeclarationCount((Integer) myVisibleMetricsNext.getValue());
    }

    @Override
    public boolean isModified() {
        if (myDefaultPositionComboBox == null) return false;
        return mySettings.getDefaultPosition() != myDefaultPositionComboBox.getSelectedItem()
            || mySettings.getVisibleMetricsAboveDeclarationCount() != (Integer) myVisibleMetricsAbove.getValue()
            || mySettings.getVisibleMetricsNextToDeclarationCount() != (Integer) myVisibleMetricsNext.getValue();
    }

    @Override
    public void reset() {
        if (myDefaultPositionComboBox == null) return;
        myDefaultPositionComboBox.setSelectedItem(mySettings.getDefaultPosition());
        myVisibleMetricsAbove.setValue(mySettings.getVisibleMetricsAboveDeclarationCount());
        myVisibleMetricsNext.setValue(mySettings.getVisibleMetricsNextToDeclarationCount());
    }

    // -------------------------------------------------------------------------
    // UI construction
    // -------------------------------------------------------------------------

    private JComponent buildComponent() {
        myDefaultPositionComboBox = new JComboBox<>(DEFAULT_ANCHORS);
        myDefaultPositionComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CodeVisionAnchorKind kind) {
                    setText(kind.name());
                }
                return this;
            }
        });

        myVisibleMetricsAbove = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
        myVisibleMetricsNext = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));

        JPanel panel = new JPanel(new MigLayout("insets 0, fillx", "[left][grow, fill]", "[][][][] "));

        panel.add(new JLabel("Default position:"));
        panel.add(myDefaultPositionComboBox, "wrap");

        panel.add(new JLabel("Lenses above declaration:"));
        panel.add(myVisibleMetricsAbove, "wrap");

        panel.add(new JLabel("Lenses next to declaration:"));
        panel.add(myVisibleMetricsNext, "wrap");

        return panel;
    }
}
