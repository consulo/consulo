/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.ui.impl.action;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ComboBoxAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.JBPopup;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.ComboBoxUI;
import java.awt.*;

/**
 * A control which looks like a combo box but drops the popup of its action down.
 *
 * @author VISTALL
 * @since 2026-09-22
 */
public final class ComboBoxActionButton extends JComboBox<Object> {
    private static final String uiClassID = "ComboBoxActionButtonUI";

    private static final int POPUP_REOPEN_THRESHOLD_MS = 200;

    public static JComponent createCustomComponent(ComboBoxAction action, Presentation presentation) {
        NonOpaquePanel panel = new NonOpaquePanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(0, 4));
        panel.add(new ComboBoxActionButton(action, presentation), BorderLayout.CENTER);
        return panel;
    }

    private final ComboBoxAction myAction;
    private final Presentation myPresentation;

    private @Nullable Runnable myCurrentPopupCanceler;
    private long myPopupHiddenAt = 0;
    private @Nullable PropertyChangeListener myButtonSynchronizer;

    private @Nullable Runnable myOnClickListener;

    public ComboBoxActionButton(ComboBoxAction action, Presentation presentation) {
        myAction = action;
        myPresentation = presentation;

        setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
                if (myPresentation.isDisabledMnemonic()) {
                    append(myPresentation.getTextValue());
                }
                else {
                    append(myPresentation.getTextValue().map(Presentation.NO_MNEMONIC));
                }
                setIcon(myPresentation.getIcon());
            }
        });

        setFocusable(ScreenReader.isActive());

        revalidateValue();
        updateSize();
        updateTooltipText(presentation.getDescription());
    }

    private void revalidateValue() {
        Object oldValue = getSelectedItem();

        Object value = new Object();
        addItem(value);
        setSelectedItem(value);

        if (oldValue != null) {
            removeItem(oldValue);
        }
    }

    public void hidePopupImpl() {
        if (myCurrentPopupCanceler != null) {
            myCurrentPopupCanceler.run();
            myCurrentPopupCanceler = null;
        }
    }

    public void showPopupImpl() {
        if (myCurrentPopupCanceler != null) {
            hidePopupImpl();
            return;
        }

        if (System.currentTimeMillis() - myPopupHiddenAt < POPUP_REOPEN_THRESHOLD_MS) {
            return;
        }

        if (myOnClickListener != null) {
            myOnClickListener.run();
            return;
        }

        JBPopup popup = myAction.createPopup(getDataContext(), () -> {
            myCurrentPopupCanceler = null;
            myPopupHiddenAt = System.currentTimeMillis();
            updateSize();
        });
        popup.showUnderneathOf(this);

        myCurrentPopupCanceler = popup::cancel;
    }

    public @Nullable Runnable getCurrentPopupCanceler() {
        return myCurrentPopupCanceler;
    }

    public @Nullable Runnable getOnClickListener() {
        return myOnClickListener;
    }

    private void updateSize() {
        revalidateValue();

        invalidate();
        repaint();
    }

    private DataContext getDataContext() {
        return DataManager.getInstance().getDataContext(this);
    }

    @Override
    public void addNotify() {
        super.addNotify();

        if (myButtonSynchronizer == null) {
            myButtonSynchronizer = new MyButtonSynchronizer();
            myPresentation.addPropertyChangeListener(myButtonSynchronizer);
            myPresentation.fireAllProperties();
        }
    }

    @Override
    public void removeNotify() {
        if (myButtonSynchronizer != null) {
            myPresentation.removePropertyChangeListener(myButtonSynchronizer);
            myButtonSynchronizer = null;
        }

        super.removeNotify();
    }

    private class MyButtonSynchronizer implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (Presentation.PROP_TEXT.equals(propertyName)) {
                updateSize();
            }
            else if (Presentation.PROP_DESCRIPTION.equals(propertyName)) {
                updateTooltipText((LocalizeValue) evt.getNewValue());
            }
            else if (Presentation.PROP_ICON.equals(propertyName)) {
                updateSize();
            }
            else if (Presentation.PROP_ENABLED.equals(propertyName)) {
                setEnabled((Boolean) evt.getNewValue());
            }
            else if (ComboBoxAction.LIKE_BUTTON.equals(propertyName)) {
                setLikeButton((Runnable) evt.getNewValue());
            }
        }
    }

    private void updateTooltipText(LocalizeValue description) {
        String tooltip = KeymapUtil.createTooltipText(description.getValue(), myAction);
        setToolTipText(!tooltip.isEmpty() ? tooltip : null);
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    private void setLikeButton(@Nullable Runnable onClick) {
        myOnClickListener = onClick;

        ComboBoxUI ui = getUI();
        if (ui instanceof ComboBoxActionButtonUI comboBoxActionButtonUI) {
            comboBoxActionButtonUI.updateArrowState(onClick == null);
        }
    }
}
