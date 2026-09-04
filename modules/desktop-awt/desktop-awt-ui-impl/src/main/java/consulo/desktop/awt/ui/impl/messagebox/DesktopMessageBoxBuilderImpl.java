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
package consulo.desktop.awt.ui.impl.messagebox;

import consulo.desktop.awt.ui.impl.DesktopCheckBoxImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.MessageTextFormat;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.BrowserHyperlinkListener;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.DialogUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.impl.BaseMessageBoxBuilder;
import consulo.ui.impl.MessagePresentation;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.CompletableFuture;

/**
 * A message box drawn as a {@link DialogWrapper}, so it keeps ide modality, the cancel action and
 * the dimension service rather than reimplementing them.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class DesktopMessageBoxBuilderImpl<V> extends BaseMessageBoxBuilder<V> {
    private static final int SCROLL_THRESHOLD = 100;
    private static final int DETAIL_ROWS = 8;

    private class DialogImpl extends DialogWrapper {
        private @Nullable ButtonImpl<V> myPressed;
        private @Nullable DesktopCheckBoxImpl myRememberBox;
        private @Nullable JComponent myDetailPane;

        DialogImpl() {
            super(false);
            setTitle(MessagePresentation.title(myTitle).get());
            init();
        }

        DialogImpl(java.awt.Component parent) {
            super(parent, false);
            setTitle(MessagePresentation.title(myTitle).get());
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(15, 0));

            Image icon = myIcon != null ? myIcon : MessagePresentation.icon(mySeverity);
            if (icon != null) {
                Container container = new Container();
                container.setLayout(new BorderLayout());
                container.add(new JBLabel(icon), BorderLayout.NORTH);
                panel.add(container, BorderLayout.WEST);
            }

            if (myText.isNotEmpty()) {
                panel.add(buildMessage(myText.get()), BorderLayout.CENTER);
            }

            if (myDetail.isNotEmpty()) {
                myDetailPane = buildDetail();
                myDetailPane.setVisible(false);
                panel.add(myDetailPane, BorderLayout.SOUTH);
            }

            return panel;
        }

        private JComponent buildMessage(String text) {
            JTextPane pane = configureMessagePane(text);

            if (text.length() <= SCROLL_THRESHOLD) {
                return pane;
            }

            Dimension screenSize = pane.getToolkit().getScreenSize();
            Dimension textSize = pane.getPreferredSize();

            JScrollPane scroll = ScrollPaneFactory.createScrollPane(pane);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            int scrollSize = (int)new JScrollBar(Adjustable.VERTICAL).getPreferredSize().getWidth();
            scroll.setPreferredSize(new Dimension(Math.min(textSize.width, screenSize.width / 2) + scrollSize,
                                                  Math.min(textSize.height, screenSize.height / 3) + scrollSize));
            return scroll;
        }

        private JTextPane configureMessagePane(String text) {
            JTextPane pane = new JTextPane();
            pane.setFont(UIUtil.getLabelFont());

            if (myTextFormat == MessageTextFormat.RICH || BasicHTML.isHTMLString(text)) {
                HTMLEditorKit editorKit = new HTMLEditorKit();
                editorKit.getStyleSheet().addRule(UIUtil.displayPropertiesToCSS(UIUtil.getLabelFont(), UIUtil.getLabelForeground()));
                pane.setEditorKit(editorKit);
                pane.setContentType(UIUtil.HTML_MIME);
                pane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
            }

            pane.setText(text);
            pane.setEditable(false);
            if (pane.getCaret() != null) {
                pane.setCaretPosition(0);
            }
            pane.setBackground(UIUtil.getOptionPaneBackground());
            pane.setForeground(UIUtil.getLabelForeground());
            return pane;
        }

        /**
         * Sits apart from the answers: it reveals the detail rather than closing the box.
         */
        @Override
        protected Action[] createLeftSideActions() {
            if (myDetail.isEmpty()) {
                return new Action[0];
            }

            return new Action[]{
                new DialogWrapperAction(CommonLocalize.buttonDetails()) {
                    @Override
                    protected void doAction(ActionEvent e) {
                        JComponent detailPane = myDetailPane;
                        if (detailPane == null) {
                            return;
                        }

                        detailPane.setVisible(!detailPane.isVisible());

                        // the box was sized without the detail, so it has to grow to make room
                        pack();
                    }
                }
            };
        }

        private JComponent buildDetail() {
            JTextArea area = new JTextArea(myDetail.get());
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setRows(DETAIL_ROWS);
            area.setBackground(UIUtil.getOptionPaneBackground());
            area.setForeground(UIUtil.getLabelForeground());
            area.setFont(UIUtil.getLabelFont());

            JScrollPane scroll = ScrollPaneFactory.createScrollPane(area);
            scroll.setBorder(IdeBorderFactory.createEmptyBorder(JBUI.insetsTop(8)));
            return scroll;
        }

        @Override
        protected Action[] createActions() {
            Action[] actions = new Action[myButtons.size()];

            for (int i = 0; i < myButtons.size(); i++) {
                ButtonImpl<V> button = myButtons.get(i);
                int exitCode = i;

                actions[i] = new DialogWrapperAction(button.label()) {
                    @Override
                    protected void doAction(ActionEvent e) {
                        // help is the one answer which leaves the box standing here
                        if (runHelpIfNeeded(button)) {
                            return;
                        }

                        // recorded before the close, so the close handler already knows the answer
                        myPressed = button;

                        close(exitCode, MessagePresentation.isAccept(button.myRole));
                    }
                };

                if (button.myDefault) {
                    actions[i].putValue(DEFAULT_ACTION, Boolean.TRUE);
                }
            }

            return actions;
        }

        @Override
        @RequiredUIAccess
        protected @Nullable JComponent createSouthPanel() {
            JPanel panel = (JPanel)super.createSouthPanel();

            if (myRemember != null && myRemember.isVisible() && panel != null) {
                myRememberBox = new DesktopCheckBoxImpl();
                myRememberBox.setLabelText(myRemember.getMessageText());
                myRememberBox.setValue(myRemember.isRememberByDefault());

                DialogUtil.registerMnemonic(myRememberBox.toAWTComponent(), '&');

                panel = addDoNotShowCheckBox(panel, myRememberBox.toAWTComponent());
                panel.setBorder(IdeBorderFactory.createEmptyBorder(JBUI.insetsTop(8)));
            }

            return panel;
        }

        @Nullable
        ButtonImpl<V> pressed() {
            return myPressed;
        }

        boolean rememberChecked() {
            DesktopCheckBoxImpl box = myRememberBox;
            if (box != null) {
                return Boolean.TRUE.equals(box.getValue());
            }
            return myRemember != null && myRemember.isRememberByDefault();
        }
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        prepare();

        V remembered = rememberedValue();
        if (remembered != null) {
            return CompletableFuture.completedFuture(remembered);
        }

        java.awt.Window awtOwner = owner != null ? TargetAWT.to(owner) : null;
        DialogImpl dialog = awtOwner != null ? new DialogImpl(awtOwner) : new DialogImpl();

        CompletableFuture<V> result = new CompletableFuture<>();

        // resolved here, on the ui thread - a cancel arrives on whichever thread asked for it
        UIAccess uiAccess = UIAccess.current();
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) {
                uiAccess.give(dialog::doCancelAction);
            }
        });

        dialog.showAsync().whenComplete((ignored, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }

            ButtonImpl<V> pressed = dialog.pressed();
            V value = pressed != null ? pressed.myValue.get() : exitValueOrNull();

            storeRemembered(pressed, dialog.rememberChecked(), value);

            result.complete(value);
        });

        return result;
    }
}
