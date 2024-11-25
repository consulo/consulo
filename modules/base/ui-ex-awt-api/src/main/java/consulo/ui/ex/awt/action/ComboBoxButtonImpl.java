/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.ex.awt.action;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.plaf.ComboBoxUI;

/**
 * @author VISTALL
 * @since 2018-07-12
 * <p>
 * Component which looks like ComboBox but will show action popup.
 */
public final class ComboBoxButtonImpl extends JComboBox<Object> implements ComboBoxButton {
    private static final String uiClassID = "ComboBoxButtonUI";

    private final ComboBoxAction myComboBoxAction;
    private final Presentation myPresentation;

    private Runnable myCurrentPopupCanceler;
    private PropertyChangeListener myButtonSynchronizer;

    private Runnable myOnClickListener;

    public ComboBoxButtonImpl(ComboBoxAction comboBoxAction, Presentation presentation) {
        myComboBoxAction = comboBoxAction;
        myPresentation = presentation;

        setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@Nonnull JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
                append(StringUtil.notNullize(myPresentation.getText()));
                setIcon(myPresentation.getIcon());
            }
        });

        // add and select one value
        revalidateValue();
        updateSize();
        updateTooltipText(presentation.getDescriptionValue());
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
        hidePopupImpl();

        if (myOnClickListener != null) {
            myOnClickListener.run();

            myCurrentPopupCanceler = null;
            return;
        }

        JBPopup popup = createPopup(() -> {
            myCurrentPopupCanceler = null;

            updateSize();
        });
        popup.showUnderneathOf(this);

        myCurrentPopupCanceler = popup::cancel;
    }

    public Runnable getCurrentPopupCanceler() {
        return myCurrentPopupCanceler;
    }

    public Runnable getOnClickListener() {
        return myOnClickListener;
    }

    private JBPopup createPopup(Runnable onDispose) {
        return myComboBoxAction.createPopup(this, getDataContext(), myPresentation, onDispose);
    }

    protected void updateSize() {
        revalidateValue();

        invalidate();
        repaint();
    }

    protected DataContext getDataContext() {
        return DataManager.getInstance().getDataContext(this);
    }

    @Override
    public String getToolTipText() {
        String text = myComboBoxAction.getTooltipText(this);
        if (text != null) {
            return text;
        }
        return super.getToolTipText();
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
                setEnabled(((Boolean) evt.getNewValue()).booleanValue());
            }
            else if (ComboBoxButton.LIKE_BUTTON.equals(propertyName)) {
                setLikeButton((Runnable) evt.getNewValue());
            }
        }
    }

    private void updateTooltipText(@Nonnull LocalizeValue description) {
        String tooltip = KeymapUtil.createTooltipText(description.getValue(), myComboBoxAction);
        setToolTipText(!tooltip.isEmpty() ? tooltip : null);
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    @Nonnull
    @Override
    public ComboBoxAction getComboBoxAction() {
        return myComboBoxAction;
    }

    private void setLikeButton(@Nullable Runnable onClick) {
        myOnClickListener = onClick;

        ComboBoxUI ui = getUI();
        if (ui instanceof ComboBoxButtonUI comboBoxButtonUI) {
            comboBoxButtonUI.updateArrowState(onClick == null);
        }
    }
}
