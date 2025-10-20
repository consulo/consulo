// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.popup;

import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutProvider;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.PopupInlineActionsSupport;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class PopupListElementRenderer<E> extends GroupedItemsListRenderer<E> {
    protected final ListPopupInternal myPopup;
    private JLabel myShortcutLabel;
    @Nullable
    private JLabel myValueLabel;

    private JPanel myButtonPane;
    protected JComponent myButtonSeparator;
    private Boolean hasExtraButtons = null; // state initialized in updateExtraButtons

    public PopupListElementRenderer(final AWTListPopup aPopup) {
        super(new ListItemDescriptor<E>() {
            @Override
            public String getTextFor(E value) {
                return aPopup.getListStep().getTextFor(value);
            }

            @Override
            public Image getIconFor(E value) {
                return aPopup.getListStep().getIconFor(value);
            }

            @Override
            public Image getSelectedIconFor(E value) {
                return aPopup.getListStep().getSelectedIconFor(value);
            }

            @Override
            public boolean hasSeparatorAboveOf(E value) {
                return aPopup.getListModel().isSeparatorAboveOf(value);
            }

            @Override
            public String getCaptionAboveOf(E value) {
                return aPopup.getListModel().getCaptionAboveOf(value);
            }

            @Override
            public boolean isSeparator(E value) {
                return aPopup.getListModel().isSeparator(value);
            }

            @Nullable
            @Override
            public String getTooltipFor(E value) {
                ListPopupStep<Object> listStep = aPopup.getListStep();
                if (!(listStep instanceof ListPopupStepEx)) {
                    return null;
                }
                return ((ListPopupStepEx<E>) listStep).getTooltipTextFor(value);
            }
        });
        myPopup = aPopup;
    }

    @Override
    protected JComponent createItemComponent() {
        createLabel();
        JPanel panel = new JPanel(new BorderLayout()) {
            private final AccessibleContext myAccessibleContext = myTextLabel.getAccessibleContext();

            @Override
            public AccessibleContext getAccessibleContext() {
                if (myAccessibleContext == null) {
                    return super.getAccessibleContext();
                }
                return myAccessibleContext;
            }
        };
        panel.setOpaque(false);
        panel.add(myTextLabel, BorderLayout.WEST);
        myValueLabel = new JLabel();
        myValueLabel.setEnabled(false);
        myValueLabel.setOpaque(false);
        myValueLabel.setBorder(JBUI.Borders.empty(0, JBUIScale.scale(8), 0, 0));
        myValueLabel.setForeground(UIManager.getColor("MenuItem.acceleratorForeground"));
        panel.add(myValueLabel, BorderLayout.CENTER);
        myShortcutLabel = new JLabel();
        myShortcutLabel.setBorder(JBUI.Borders.empty(0, 0, 0, 3));
        myShortcutLabel.setForeground(UIManager.getColor("MenuItem.acceleratorForeground"));
        myShortcutLabel.setOpaque(false);
        panel.add(myShortcutLabel, BorderLayout.EAST);
        return layoutComponent(panel);
    }

    @Override
    protected final JComponent layoutComponent(JComponent middleItemComponent) {
        myNextStepLabel = new JLabel();
        myNextStepLabel.setOpaque(false);

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        myButtonPane = right;

        myButtonSeparator = createButtonsSeparator();

        BorderLayoutPanel left = JBUI.Panels.simplePanel()
            .andTransparent()
            .addToLeft(middleItemComponent)
            .addToRight(myButtonSeparator);

        return JBUI.Panels.simplePanel(left)
            .addToRight(right)
            .andTransparent()
            .withBorder(getDefaultItemComponentBorder());
    }

    @Override
    protected void customizeComponent(JList<? extends E> list, E value, boolean isSelected) {
        ListPopupStep<Object> step = myPopup.getListStep();
        boolean isSelectable = step.isSelectable(value);
        myTextLabel.setEnabled(isSelectable);

        setSelected(myComponent, isSelected && isSelectable);
        setSelected(myTextLabel, isSelected && isSelectable);
        setSelected(myNextStepLabel, isSelected && isSelectable);

        if (step instanceof BaseListPopupStep) {
            Color bg = ((BaseListPopupStep<E>) step).getBackgroundFor(value);
            Color fg = ((BaseListPopupStep<E>) step).getForegroundFor(value);
            if (!isSelected && fg != null) {
                myTextLabel.setForeground(fg);
            }
            if (!isSelected && bg != null) {
                UIUtil.setBackgroundRecursively(myComponent, bg);
            }
        }

        if (step.isMnemonicsNavigationEnabled()) {
            MnemonicNavigationFilter<Object> filter = step.getMnemonicNavigationFilter();
            int pos = filter == null ? -1 : filter.getMnemonicPos(value);
            if (pos != -1) {
                String text = myTextLabel.getText();
                text = text.substring(0, pos) + text.substring(pos + 1);
                myTextLabel.setText(text);
                myTextLabel.setDisplayedMnemonicIndex(pos);
            }
        }
        else {
            myTextLabel.setDisplayedMnemonicIndex(-1);
        }

        boolean showNextStepLabel = step.hasSubstep(value) && !myPopup.getPopupInlineActionsSupport().hasExtraButtons(value);
        if (showNextStepLabel) {
            myNextStepLabel.setVisible(true);
            myNextStepLabel.setIcon(UIUtil.getMenuArrowIcon(isSelected));
            myNextStepLabel.getAccessibleContext()
                .setAccessibleName(isSelectable ? UILocalize.popupListItemRendererNextStepLabelAccessibleName().get() : null);
        }
        else {
            myNextStepLabel.setVisible(false);
            myNextStepLabel.getAccessibleContext().setAccessibleName(null);
        }

        boolean hasNextIcon = myNextStepLabel.isVisible();
        boolean hasInlineButtons = updateExtraButtons(list, value, step, isSelected, hasNextIcon);

        if (myShortcutLabel != null) {
            myShortcutLabel.setEnabled(isSelectable);
            myShortcutLabel.setText("");
            if (value instanceof ShortcutProvider) {
                ShortcutSet set = ((ShortcutProvider) value).getShortcut();
                if (set != null) {
                    Shortcut shortcut = ArrayUtil.getFirstElement(set.getShortcuts());
                    if (shortcut != null) {
                        myShortcutLabel.setText("     " + KeymapUtil.getShortcutText(shortcut));
                    }
                }
            }
            setSelected(myShortcutLabel, isSelected && isSelectable);
            myShortcutLabel.setForeground(isSelected && isSelectable ? UIManager.getColor("MenuItem.acceleratorSelectionForeground") : UIManager.getColor("MenuItem.acceleratorForeground"));
        }

        if (myValueLabel != null) {
            myValueLabel.setText(step instanceof ListPopupStepEx<?> ? ((ListPopupStepEx<E>) step).getValueFor(value) : null);
            setSelected(myValueLabel, isSelected && isSelectable);
        }
    }

    private boolean updateExtraButtons(JList<? extends E> list, E value, ListPopupStep<Object> step, boolean isSelected, boolean hasNextIcon) {
        GridBag gb = new GridBag().setDefaultFill(GridBagConstraints.BOTH)
            .setDefaultAnchor(GridBagConstraints.CENTER)
            .setDefaultWeightX(1.0)
            .setDefaultWeightY(1.0);

        PopupInlineActionsSupport inlineActionsSupport = myPopup.getPopupInlineActionsSupport();

        boolean isSelectable = step.isSelectable(value);
        Integer activeButtonIndex;
        java.util.List<JComponent> extraButtons;
        if (!isSelectable) {
            activeButtonIndex = null;
            extraButtons = Collections.emptyList();
        }
        else {
            activeButtonIndex = inlineActionsSupport.getActiveButtonIndex(list);
            extraButtons = inlineActionsSupport.createExtraButtons(
                value, isSelected, !isSelected || activeButtonIndex == null ? -1 : activeButtonIndex);
        }

        if (!extraButtons.isEmpty()) {
            myButtonPane.removeAll();
            myButtonSeparator.setVisible(true);
            extraButtons.forEach(comp -> myButtonPane.add(comp, gb.next()));
            // We ONLY need to update the tooltip if there's an active inline action button.
            // Otherwise, it's set earlier from the main action.
            // If there is an active button without a tooltip, we still need to set the tooltip
            // to null, otherwise it'll look ugly, as if the inline action button has the same
            // tooltip as the main action.
            if (activeButtonIndex != null && activeButtonIndex < extraButtons.size()) {
                String text = inlineActionsSupport.getToolTipText(value, activeButtonIndex);
                myRendererComponent.setToolTipText(text);
            }
            hasExtraButtons = true;
        }
        else if (!hasNextIcon && inlineActionsSupport.hasExtraButtons(value)) {
            myButtonPane.removeAll();
            myButtonSeparator.setVisible(false);
            myButtonPane.add(Box.createHorizontalStrut(buttonWidth()), gb.next());
            hasExtraButtons = true;
        }
        else if (hasExtraButtons == null || hasExtraButtons) {
            myButtonPane.removeAll();
            myButtonSeparator.setVisible(false);
            myButtonPane.add(myNextStepLabel, gb.next());
            hasExtraButtons = false;
        }

        return !extraButtons.isEmpty();
    }

    @Nonnull
    protected static JComponent createButtonsSeparator() {
        SeparatorComponent separator = new SeparatorComponent(JBCurrentTheme.Popup.separatorTextColor(), SeparatorOrientation.VERTICAL);
        return separator;
    }

    private static int buttonWidth() {
        return JBUIScale.scale(Image.DEFAULT_ICON_SIZE + JBCurrentTheme.List.buttonLeftRightInsets() * 2);
    }
}
