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
package consulo.ui.desktop.internal.alert;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.DialogUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.NotificationType;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.DesktopCheckBoxImpl;
import consulo.ui.image.Image;
import consulo.ui.impl.BaseAlert;
import consulo.util.concurrent.AsyncResult;

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
public class DesktopPlainAlertImpl<V> extends BaseAlert<V> {
  class DialogImpl extends DialogWrapper {
    private V mySelectedValue;

    private DesktopCheckBoxImpl myRememberBox;

    DialogImpl() {
      super(false);
      setTitle(myTitle.getValue());
      init();
    }

    DialogImpl(@Nonnull java.awt.Component parentComponent) {
      super(parentComponent, false);
      setTitle(myTitle.getValue());
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
        final LocalizeValue localizeValue = getText(button);

        actions[i] = new DialogWrapperAction(localizeValue) {
          @Override
          protected void doAction(ActionEvent e) {
            close(exitCode, true);

            mySelectedValue = button.myValue.get();
          }
        };

        if (button.myDefault) {
          actions[i].putValue(DEFAULT_ACTION, Boolean.TRUE);
        }
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
        myRememberBox.setLabelText(LocalizeValue.of(myRemember.getMessageBoxText()));
        myRememberBox.setValue(myRemember.isRememberByDefault());

        DialogUtil.registerMnemonic(myRememberBox.toAWTComponent(), '&');

        JComponent southPanel = panel;

        panel = addDoNotShowCheckBox(southPanel, myRememberBox.toAWTComponent());

        panel.setBorder(IdeBorderFactory.createEmptyBorder(JBUI.insetsTop(8)));
      }
      return panel;
    }

    protected JComponent doCreateCenterPanel() {
      JPanel panel = new JPanel(new BorderLayout(15, 0));

      Image icon = getIcon(myType);
      JLabel iconLabel = new JBLabel(icon);
      Container container = new Container();
      container.setLayout(new BorderLayout());
      container.add(iconLabel, BorderLayout.NORTH);
      panel.add(container, BorderLayout.WEST);

      String textValue = myText.getValue();
      if (!textValue.isEmpty()) {
        final JTextPane messageComponent = createMessageComponent(textValue);

        final Dimension screenSize = messageComponent.getToolkit().getScreenSize();
        final Dimension textSize = messageComponent.getPreferredSize();
        if (textValue.length() > 100) {
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

  @Nonnull
  public static Image getIcon(NotificationType type) {
    switch (type) {
      case INFO:
        return UIUtil.getInformationIcon();
      case WARNING:
        return UIUtil.getWarningIcon();
      case ERROR:
        return UIUtil.getErrorIcon();
      case QUESTION:
        return UIUtil.getQuestionIcon();
      default:
        throw new UnsupportedOperationException(type.name());
    }
  }

  @Override
  @RequiredUIAccess
  @Nonnull
  public AsyncResult<V> showAsync(@Nullable Component component) {
    return showAsync(TargetAWT.to(component));
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<V> showAsync(@Nullable Window component) {
    return showAsync(TargetAWT.to(component));
  }

  @RequiredUIAccess
  private AsyncResult<V> showAsync(@Nullable java.awt.Component component) {
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
    DialogImpl dialog = component == null ? new DialogImpl() : new DialogImpl(component);
    AsyncResult<Void> async = dialog.showAsync();
    async.doWhenProcessed(() -> {
      V selectValue = dialog.mySelectedValue;
      // null of if dialog closed via X button, not target buttons
      if(selectValue == null) {
        selectValue = myExitValue.get();
      }

      if (myRemember != null) {
        if (dialog.myRememberBox.getValueOrError()) {
          myRemember.setValue(selectValue);
        }
      }

      result.setDone(selectValue);
    });
    return result;
  }
}
