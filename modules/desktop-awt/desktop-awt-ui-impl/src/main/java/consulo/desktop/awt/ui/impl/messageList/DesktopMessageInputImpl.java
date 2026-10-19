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
package consulo.desktop.awt.ui.impl.messageList;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBTextArea;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageInputStyle;
import consulo.ui.messageList.MessageInputSubmitEvent;
import consulo.ui.messageList.MessageInputTextChangeEvent;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
public class DesktopMessageInputImpl extends SwingComponentDelegate<JPanel> implements MessageInput {
    private static final int MAX_ROWS = 6;

    private class FieldPanel extends JPanel implements FromSwingComponentWrapper {
        FieldPanel() {
            super(new BorderLayout(JBUI.scale(4), 0));
            setOpaque(false);
            setBorder(JBUI.Borders.empty(4, 10, 4, 4));
        }

        @Override
        public Component toUIComponent() {
            return DesktopMessageInputImpl.this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (myInplace) {
                g.setColor(myTextArea.getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float arc = JBUI.scale(8f);
                float stroke = JBUI.scale(myTextArea.hasFocus() ? 2f : 1f);
                RoundRectangle2D shape = new RoundRectangle2D.Float(stroke / 2, stroke / 2, getWidth() - stroke, getHeight() - stroke, arc, arc);
                g2.setColor(myTextArea.getBackground());
                g2.fill(shape);
                g2.setColor(TargetAWT.to(myTextArea.hasFocus() ? ComponentColors.FOCUS_COLOR : ComponentColors.BORDER));
                g2.setStroke(new BasicStroke(stroke));
                g2.draw(shape);
            }
            finally {
                g2.dispose();
            }
        }
    }

    private final JBTextArea myTextArea = new JBTextArea(1, 20);
    private static final String DEFAULT_SEND_TEXT = "\u27A4";

    private final JButton mySendButton = new JButton(DEFAULT_SEND_TEXT);
    private LocalizeValue mySendText = LocalizeValue.empty();
    private @Nullable Image mySendIcon;
    private @Nullable Component mySuffixComponent;
    private boolean myInplace;
    private final JPanel myButtonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(4), 0));

    @Override
    protected JPanel createComponent() {
        FieldPanel panel = new FieldPanel();

        myTextArea.setLineWrap(true);
        myTextArea.setWrapStyleWord(true);
        myTextArea.setOpaque(false);
        myTextArea.setBorder(JBUI.Borders.empty(4, 0));

        JScrollPane scrollPane = new JScrollPane(myTextArea) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                int lineHeight = myTextArea.getFontMetrics(myTextArea.getFont()).getHeight();
                Insets insets = myTextArea.getInsets();
                int max = lineHeight * MAX_ROWS + insets.top + insets.bottom;
                return new Dimension(size.width, Math.min(myTextArea.getPreferredSize().height, max));
            }
        };
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        mySendButton.setBorderPainted(false);
        mySendButton.setContentAreaFilled(false);
        mySendButton.setFocusable(false);
        mySendButton.setForeground(TargetAWT.to(ComponentColors.LINK_FOREGROUND));
        mySendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mySendButton.addActionListener(e -> submit());
        mySendButton.setEnabled(false);

        myButtonRow.setOpaque(false);
        myButtonRow.add(mySendButton);
        JPanel buttonBox = new JPanel(new BorderLayout());
        buttonBox.setOpaque(false);
        buttonBox.add(myButtonRow, BorderLayout.SOUTH);
        panel.add(buttonBox, BorderLayout.EAST);

        InputMap inputMap = myTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "consulo.messageInput.submit");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "insert-break");
        myTextArea.getActionMap().put("consulo.messageInput.submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submit();
            }
        });

        myTextArea.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                mySendButton.setEnabled(myTextArea.isEnabled() && !myTextArea.getText().isBlank());
                panel.revalidate();
                getListenerDispatcher(MessageInputTextChangeEvent.class)
                    .onEvent(new MessageInputTextChangeEvent(DesktopMessageInputImpl.this, myTextArea.getText()));
            }
        });
        myTextArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                panel.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                panel.repaint();
            }
        });
        return panel;
    }

    @RequiredUIAccess
    private void submit() {
        String text = myTextArea.getText();
        if (!myTextArea.isEnabled() || text.isBlank()) {
            return;
        }
        myTextArea.setText("");
        getListenerDispatcher(MessageInputSubmitEvent.class).onEvent(new MessageInputSubmitEvent(this, text));
    }

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        toAWTComponent();
        myTextArea.setEnabled(value);
        mySendButton.setEnabled(value && !myTextArea.getText().isBlank());
    }

    @RequiredUIAccess
    @Override
    public void setPlaceholder(LocalizeValue placeholder) {
        myTextArea.getEmptyText().setText(placeholder.get());
    }

    @RequiredUIAccess
    @Override
    public void insertText(String text) {
        myTextArea.replaceSelection(text);
        myTextArea.requestFocusInWindow();
    }

    @RequiredUIAccess
    @Override
    public void setSendText(LocalizeValue sendText) {
        mySendText = sendText;
        updateSendButton();
    }

    @Override
    public void addStyle(MessageInputStyle style) {
        switch (style) {
            case INPLACE -> {
                myInplace = true;
                toAWTComponent().repaint();
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        toAWTComponent();
        Component old = mySuffixComponent;
        if (old != null) {
            myButtonRow.remove(TargetAWT.to(old));
        }
        mySuffixComponent = suffixComponent;
        if (suffixComponent != null) {
            java.awt.Component suffix = TargetAWT.to(suffixComponent);
            if (suffix instanceof JPanel panel) {
                panel.setOpaque(false);
            }
            myButtonRow.add(suffix, 0);
        }
        myButtonRow.revalidate();
        myButtonRow.repaint();
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @RequiredUIAccess
    @Override
    public void setSendIcon(@Nullable Image icon) {
        mySendIcon = icon;
        updateSendButton();
    }

    private void updateSendButton() {
        String text = mySendText == LocalizeValue.empty() ? null : mySendText.get();
        Image icon = mySendIcon;
        if (icon != null) {
            mySendButton.setIcon(TargetAWT.to(icon));
            mySendButton.setText(null);
            mySendButton.setToolTipText(text);
        }
        else {
            mySendButton.setIcon(null);
            mySendButton.setText(text == null ? DEFAULT_SEND_TEXT : text);
            mySendButton.setToolTipText(null);
        }
        mySendButton.getAccessibleContext().setAccessibleName(text);
    }
}
