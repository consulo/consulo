/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.DialogUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Window;
import consulo.ui.impl.BaseAlert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author VISTALL
 * @since 2019-01-12
 */
class DesktopAlertImpl<V> extends BaseAlert<V> {
  class DialogImpl extends DialogWrapper {
    private V mySelectedValue;

    private DesktopCheckBoxImpl myRememberBox;

    DialogImpl(boolean canBeParent) {
      super(canBeParent);

      setTitle(myTitle);

      init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return doCreateCenterPanel();
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
      Action[] actions = new Action[myButtons.size()];
      for (int i = 0; i < myButtons.size(); i++) {
        ButtonImpl button = myButtons.get(i);
        final int exitCode = i;
        final String text = getText(button);

        actions[i] = new AbstractAction(UIUtil.replaceMnemonicAmpersand(text)) {
          @Override
          public void actionPerformed(ActionEvent e) {
            close(exitCode, true);

            mySelectedValue = button.myValue.get();
          }
        };

        if (button.myDefault) {
          actions[i].putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        UIUtil.assignMnemonic(text, actions[i]);

      }
      return actions;
    }

    @Nullable
    @Override
    @RequiredUIAccess
    protected JComponent createSouthPanel() {
      JPanel panel = (JPanel)super.createSouthPanel();

      if (myRemember != null) {
        myRememberBox = new DesktopCheckBoxImpl();
        myRememberBox.setText(myRemember.getMessageBoxText());
        myRememberBox.setValue(myRemember.isRememberByDefault());

        DialogUtil.registerMnemonic((JCheckBox)myRememberBox.toAWTComponent(), '&');

        JComponent southPanel = panel;

        panel = addDoNotShowCheckBox(southPanel, (JComponent)myRememberBox.toAWTComponent());

        panel.setBorder(IdeBorderFactory.createEmptyBorder(JBUI.insetsTop(8)));
      }
      return panel;
    }

    protected JComponent doCreateCenterPanel() {
      JPanel panel = new JPanel(new BorderLayout(15, 0));

      Icon icon = getIcon();
      if (icon != null) {
        JLabel iconLabel = new JLabel(icon);
        Container container = new Container();
        container.setLayout(new BorderLayout());
        container.add(iconLabel, BorderLayout.NORTH);
        panel.add(container, BorderLayout.WEST);
      }
      if (myText != null) {
        final JTextPane messageComponent = createMessageComponent(myText);

        final Dimension screenSize = messageComponent.getToolkit().getScreenSize();
        final Dimension textSize = messageComponent.getPreferredSize();
        if (myText.length() > 100) {
          final JScrollPane pane = ScrollPaneFactory.createScrollPane(messageComponent);
          pane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
          pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
          pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
          final int scrollSize = (int)new JScrollBar(Adjustable.VERTICAL).getPreferredSize().getWidth();
          final Dimension preferredSize = new Dimension(Math.min(textSize.width, screenSize.width / 2) + scrollSize, Math.min(textSize.height, screenSize.height / 3) + scrollSize);
          pane.setPreferredSize(preferredSize);
          panel.add(pane, BorderLayout.CENTER);
        }
        else {
          panel.add(messageComponent, BorderLayout.CENTER);
        }
      }
      return panel;
    }

    protected JTextPane createMessageComponent(final String message) {
      final JTextPane messageComponent = new JTextPane();
      return configureMessagePaneUi(messageComponent, message);
    }

    @Nonnull
    public JTextPane configureMessagePaneUi(JTextPane messageComponent, String message) {
      JTextPane pane = configureMessagePaneUi(messageComponent, message, null);
      if (UIUtil.HTML_MIME.equals(pane.getContentType())) {
        pane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
      }
      return pane;
    }

    @Nonnull
    public JTextPane configureMessagePaneUi(@Nonnull JTextPane messageComponent, @Nullable String message, @Nullable UIUtil.FontSize fontSize) {
      UIUtil.FontSize fixedFontSize = fontSize == null ? UIUtil.FontSize.NORMAL : fontSize;
      messageComponent.setFont(UIUtil.getLabelFont(fixedFontSize));
      if (BasicHTML.isHTMLString(message)) {
        HTMLEditorKit editorKit = new HTMLEditorKit();
        Font font = UIUtil.getLabelFont(fixedFontSize);
        editorKit.getStyleSheet().addRule(UIUtil.displayPropertiesToCSS(font, UIUtil.getLabelForeground()));
        messageComponent.setEditorKit(editorKit);
        messageComponent.setContentType(UIUtil.HTML_MIME);
      }
      messageComponent.setText(message);
      messageComponent.setEditable(false);
      if (messageComponent.getCaret() != null) {
        messageComponent.setCaretPosition(0);
      }

      messageComponent.setBackground(UIUtil.getOptionPaneBackground());
      messageComponent.setForeground(UIUtil.getLabelForeground());
      return messageComponent;
    }

    @Override
    public void dispose() {
      super.dispose();
    }
  }

  @Nullable
  private Icon getIcon() {
    switch (myType) {
      case INFO:
        return UIUtil.getInformationIcon();
      case WARNING:
        return UIUtil.getWarningIcon();
      case ERROR:
        return UIUtil.getErrorIcon();
      case QUESTION:
        return UIUtil.getQuestionIcon();
      default:
        throw new UnsupportedOperationException(myType.name());
    }
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<V> show(@Nullable Window component) {
    if (myButtons.isEmpty()) {
      throw new UnsupportedOperationException("Buttons empty");
    }

    if (myExitValue == null) {
      throw new UnsupportedOperationException("Exit value is not set. Use #asExitButton() or #exitValue()");
    }

    V value = myRemember != null ? myRemember.getValue() : null;
    if (value != null) {
      return AsyncResult.resolved(value);
    }

    AsyncResult<V> result = AsyncResult.undefined();
    DialogImpl dialog = new DialogImpl(false);
    AsyncResult<Void> async = dialog.showAsync();
    async.doWhenProcessed(() -> {
      V selectValue = dialog.mySelectedValue;
      if (myRemember != null) {
        if (dialog.myRememberBox.getValue()) {
          myRemember.setValue(selectValue);
        }
      }

      result.setDone(selectValue);
    });
    return result;
  }
}
