/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.plaf2.flat.InplaceComponent;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.util.TextWithMnemonic;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
class DesktopButtonImpl extends SwingComponentDelegate<DesktopButtonImpl.MyButton> implements Button {
    private static final int SUFFIX_GAP = 4;

    class MyButton extends JButton implements FromSwingComponentWrapper {
        private LocalizeValue myTextValue = LocalizeValue.empty();

        private Boolean myDefaultButton;

        private @Nullable String mySearchHighlight;

        private boolean myVertical;

        private boolean myPill;

        private java.awt.@Nullable Component mySuffix;

        private int myAlignmentWithoutSuffix = SwingConstants.CENTER;

        MyButton(LocalizeValue textValue) {
            super("");
            myTextValue = textValue;

            updateText();
        }

        @Override
        public boolean isDefaultButton() {
            if (Boolean.TRUE == myDefaultButton) {
                return true;
            }

            return super.isDefaultButton();
        }

        @Override
        public void updateUI() {
            if (myVertical) {
                setUI(new VerticalButtonUI());
            }
            else if (myPill) {
                setUI(new PillButtonUI());
            }
            else {
                super.updateUI();
            }

            // null if called from parent object before field initialize
            if (myTextValue != null) {
                updateText();
            }
        }

        String getPlainText() {
            return TextWithMnemonic.parse(myTextValue.get()).getText();
        }

        void setPill() {
            myPill = true;
            updateUI();
            setFocusable(false);
            revalidate();
            repaint();
        }

        void setVertical() {
            myVertical = true;
            updateUI();
            revalidate();
            repaint();
        }

        
        @Override
        public Component toUIComponent() {
            return DesktopButtonImpl.this;
        }

        void setSuffix(java.awt.@Nullable Component suffix) {
            java.awt.Component oldSuffix = mySuffix;
            if (oldSuffix != null) {
                remove(oldSuffix);
            }
            if (oldSuffix == null && suffix != null) {
                myAlignmentWithoutSuffix = getHorizontalAlignment();
                setHorizontalAlignment(SwingConstants.LEADING);
            }
            else if (oldSuffix != null && suffix == null) {
                setHorizontalAlignment(myAlignmentWithoutSuffix);
            }
            mySuffix = suffix;
            if (suffix != null) {
                add(suffix);
            }
            revalidate();
            repaint();
        }

        private int suffixWidth() {
            java.awt.Component suffix = mySuffix;
            return suffix == null || myVertical ? 0 : suffix.getPreferredSize().width + JBUI.scale(SUFFIX_GAP);
        }

        private Dimension withSuffix() {
            String text = getText();
            Rectangle iconRect = new Rectangle();
            Rectangle textRect = new Rectangle();
            Rectangle viewRect = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);
            SwingUtilities.layoutCompoundLabel(
                this,
                getFontMetrics(getFont()),
                text,
                getIcon(),
                getVerticalAlignment(),
                getHorizontalAlignment(),
                getVerticalTextPosition(),
                getHorizontalTextPosition(),
                viewRect,
                iconRect,
                textRect,
                text == null || text.isEmpty() ? 0 : getIconTextGap()
            );
            Rectangle content = iconRect.union(textRect);
            Insets insets = getInsets();

            java.awt.Component suffix = mySuffix;
            int suffixHeight = suffix == null ? 0 : suffix.getPreferredSize().height;
            int height = Math.max(content.height, suffixHeight) + insets.top + insets.bottom;
            return new Dimension(content.width + insets.left + insets.right + suffixWidth(), height);
        }

        @Override
        public Dimension getPreferredSize() {
            if (mySuffix == null || myVertical || isPreferredSizeSet()) {
                return super.getPreferredSize();
            }
            return withSuffix();
        }

        @Override
        public Dimension getMinimumSize() {
            if (mySuffix == null || myVertical || isMinimumSizeSet()) {
                return super.getMinimumSize();
            }
            return withSuffix();
        }

        @Override
        public Dimension getMaximumSize() {
            if (mySuffix == null || myVertical || isMaximumSizeSet()) {
                return super.getMaximumSize();
            }
            return withSuffix();
        }

        @Override
        public void doLayout() {
            super.doLayout();

            java.awt.Component suffix = mySuffix;
            if (suffix != null) {
                Dimension size = suffix.getPreferredSize();
                Insets insets = getInsets();
                suffix.setBounds(getWidth() - insets.right - size.width, (getHeight() - size.height) / 2, size.width, size.height);
            }
        }

        public void setSearchHighlight(@Nullable String query) {
            mySearchHighlight = query;
            updateText();
        }

        private void updateText() {
            updateTextForButton(this, myTextValue, mySearchHighlight);
        }
    }

    private final LocalizeValue myInitText;

    private @Nullable Component mySuffixComponent;

    public DesktopButtonImpl(LocalizeValue text) {
        myInitText = text;
    }

    protected MyButton createButton(LocalizeValue text) {
        return new MyButton(text);
    }

    @Override
    protected MyButton createComponent() {
        MyButton button = createButton(myInitText);
        button.addActionListener(e -> invoke(DesktopAWTInputDetails.convert(toAWTComponent(), e)));
        return button;
    }

    
    @Override
    public Disposable addClickListener(ComponentEventListener<Component, ClickEvent> clickListener) {
        return addListener(ClickEvent.class, clickListener);
    }

    @Override
    public void addStyle(ButtonStyle style) {
        switch (style) {
            case BORDERLESS:
                toAWTComponent().putClientProperty("JButton.buttonType", "borderless");
                break;
            case TOOLBAR:
                toAWTComponent().putClientProperty("JButton.buttonType", "toolBarButton");
                break;
            case INPLACE:
                InplaceComponent.prepareLeadingOrTrailingComponent(toAWTComponent());
                break;
            case PRIMARY:
                toAWTComponent().myDefaultButton = true;
                break;
            case HELP:
                toAWTComponent().putClientProperty("JButton.buttonType", "help");
                break;
            case VERTICAL:
                toAWTComponent().setVertical();
                break;
            case PILL:
                toAWTComponent().setPill();
                break;
        }
    }

    @Override
    @RequiredUIAccess
    public void invoke(InputDetails inputDetails) {
        ComponentEventListener<Component, ClickEvent> listener = getListenerDispatcher(ClickEvent.class);

        listener.onEvent(new ClickEvent(this, inputDetails));
    }

    
    @Override
    public LocalizeValue getText() {
        MyButton button = toAWTComponent();
        return button.myTextValue;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        MyButton button = toAWTComponent();
        button.myTextValue = text;
        button.updateText();
    }

    @Override
    public @Nullable Image getIcon() {
        return TargetAWT.from(toAWTComponent().getIcon());
    }

    @RequiredUIAccess
    @Override
    public void setIcon(@Nullable Image image) {
        toAWTComponent().setIcon(TargetAWT.to(image));
    }

    @RequiredUIAccess
    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
        toAWTComponent().setSuffix(suffixComponent == null ? null : TargetAWT.to(suffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @RequiredUIAccess
    @Override
    public void setSearchHighlight(@Nullable String query) {
        toAWTComponent().setSearchHighlight(SearchHighlightUtil.normalizeQuery(query));
    }

    @Override
    public @Nullable String getSearchHighlight() {
        return toAWTComponent().mySearchHighlight;
    }
}
