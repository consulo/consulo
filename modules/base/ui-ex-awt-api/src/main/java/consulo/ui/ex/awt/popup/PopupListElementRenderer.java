// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.popup;

import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.ShortcutProvider;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;

public class PopupListElementRenderer<E> extends GroupedItemsListRenderer<E> {
    protected final ListPopup myPopup;
    private JLabel myShortcutLabel;
    @Nullable
    private JLabel myValueLabel;

    public PopupListElementRenderer(final AWTListPopup aPopup) {
        super(new ListItemDescriptorAdapter<E>() {
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
        panel.add(myTextLabel, BorderLayout.WEST);
        myValueLabel = new JLabel();
        myValueLabel.setEnabled(false);
        myValueLabel.setBorder(JBUI.Borders.empty(0, JBUIScale.scale(8), 1, 0));
        myValueLabel.setForeground(UIManager.getColor("MenuItem.acceleratorForeground"));
        panel.add(myValueLabel, BorderLayout.CENTER);
        myShortcutLabel = new JLabel();
        myShortcutLabel.setBorder(JBUI.Borders.empty(0, 0, 1, 3));
        myShortcutLabel.setForeground(UIManager.getColor("MenuItem.acceleratorForeground"));
        panel.add(myShortcutLabel, BorderLayout.EAST);
        return layoutComponent(panel);
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
            if (bg != null && mySeparatorComponent.isVisible() && myCurrentIndex > 0) {
                E prevValue = list.getModel().getElementAt(myCurrentIndex - 1);
                // separator between 2 colored items shall get color too
                if (Comparing.equal(bg, ((BaseListPopupStep<E>) step).getBackgroundFor(prevValue))) {
                    myRendererComponent.setBackground(bg);
                }
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

        if (step.hasSubstep(value) && isSelectable) {
            myNextStepLabel.setVisible(true);
            myNextStepLabel.setIcon(UIUtil.getMenuArrowIcon(isSelected));
        }
        else {
            myNextStepLabel.setVisible(false);
        }

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
}
