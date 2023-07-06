/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.net;

import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import consulo.application.ApplicationManager;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.configurable.IdeaConfigurableUi;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.http.HttpRequests;
import consulo.http.impl.internal.proxy.CommonProxy;
import consulo.http.impl.internal.proxy.HttpProxyManagerImpl;
import consulo.http.impl.internal.proxy.HttpProxyManagerState;
import consulo.http.impl.internal.proxy.JavaProxyProperty;
import consulo.ide.impl.idea.ui.PortField;
import consulo.ui.ex.awt.RelativeFont;
import consulo.ui.ex.awt.JBRadioButton;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

class HttpProxySettingsUi implements IdeaConfigurableUi<HttpProxyManagerImpl> {
  private JPanel myMainPanel;

  private JTextField myProxyLoginTextField;
  private JPasswordField myProxyPasswordTextField;
  private JCheckBox myProxyAuthCheckBox;
  private PortField myProxyPortTextField;
  private JTextField myProxyHostTextField;
  private JCheckBox myRememberProxyPasswordCheckBox;

  private JLabel myProxyLoginLabel;
  private JLabel myProxyPasswordLabel;
  private JLabel myHostNameLabel;
  private JLabel myPortNumberLabel;
  private JBRadioButton myAutoDetectProxyRb;
  private JBRadioButton myUseHTTPProxyRb;
  private JLabel mySystemProxyDefined;
  private JBRadioButton myNoProxyRb;
  private JBRadioButton myHTTP;
  private JBRadioButton mySocks;
  private JButton myClearPasswordsButton;
  private JLabel myErrorLabel;
  private JButton myCheckButton;
  private JLabel myOtherWarning;
  private JLabel myProxyExceptionsLabel;
  private RawCommandLineEditor myProxyExceptions;
  private JLabel myNoProxyForLabel;
  private JCheckBox myPacUrlCheckBox;
  private JTextField myPacUrlTextField;
  private volatile boolean myConnectionCheckInProgress;

  @Override
  public boolean isModified(@Nonnull HttpProxyManagerImpl settings) {
    if (!isValid()) {
      return false;
    }

    HttpProxyManagerState state = settings.getState();
    return !Comparing.strEqual(myProxyExceptions.getText().trim(), state.PROXY_EXCEPTIONS) ||
           state.USE_PROXY_PAC != myAutoDetectProxyRb.isSelected() ||
           state.USE_PAC_URL != myPacUrlCheckBox.isSelected() ||
           !Comparing.strEqual(state.PAC_URL, myPacUrlTextField.getText()) ||
           state.USE_HTTP_PROXY != myUseHTTPProxyRb.isSelected() ||
           state.PROXY_AUTHENTICATION != myProxyAuthCheckBox.isSelected() ||
           state.KEEP_PROXY_PASSWORD != myRememberProxyPasswordCheckBox.isSelected() ||
           state.PROXY_TYPE_IS_SOCKS != mySocks.isSelected() ||
           !Comparing.strEqual(settings.getProxyLogin(), myProxyLoginTextField.getText()) ||
           !Comparing.strEqual(settings.getPlainProxyPassword(), new String(myProxyPasswordTextField.getPassword())) ||
           state.PROXY_PORT != myProxyPortTextField.getNumber() ||
           !Comparing.strEqual(state.PROXY_HOST, myProxyHostTextField.getText());
  }

  public HttpProxySettingsUi(@Nonnull final HttpProxyManagerImpl settings) {
    ButtonGroup group = new ButtonGroup();
    group.add(myUseHTTPProxyRb);
    group.add(myAutoDetectProxyRb);
    group.add(myNoProxyRb);
    myNoProxyRb.setSelected(true);

    ButtonGroup proxyTypeGroup = new ButtonGroup();
    proxyTypeGroup.add(myHTTP);
    proxyTypeGroup.add(mySocks);
    myHTTP.setSelected(true);

    Boolean property = Boolean.getBoolean(JavaProxyProperty.USE_SYSTEM_PROXY);
    mySystemProxyDefined.setVisible(Boolean.TRUE.equals(property));
    if (Boolean.TRUE.equals(property)) {
      mySystemProxyDefined.setIcon(TargetAWT.to(Messages.getWarningIcon()));
      RelativeFont.BOLD.install(mySystemProxyDefined);
    }

    myProxyAuthCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull ActionEvent e) {
        enableProxyAuthentication(myProxyAuthCheckBox.isSelected());
      }
    });
    myPacUrlCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull ActionEvent e) {
        myPacUrlTextField.setEnabled(myPacUrlCheckBox.isSelected());
      }
    });

    ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull ActionEvent e) {
        enableProxy(myUseHTTPProxyRb.isSelected());
      }
    };
    myUseHTTPProxyRb.addActionListener(listener);
    myAutoDetectProxyRb.addActionListener(listener);
    myNoProxyRb.addActionListener(listener);

    myClearPasswordsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull ActionEvent e) {
        settings.clearGenericPasswords();
        //noinspection DialogTitleCapitalization
        Messages.showMessageDialog(myMainPanel, "Proxy passwords were cleared.", "Auto-detected Proxy", Messages.getInformationIcon());
      }
    });

    configureCheckButton();
  }

  private void configureCheckButton() {
    if (HttpProxyManagerImpl.getInstance() == null) {
      myCheckButton.setVisible(false);
      return;
    }

    myCheckButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull ActionEvent e) {
        final String title = "Check Proxy Settings";
        final String answer = Messages.showInputDialog(myMainPanel, "Warning: your settings will be saved.\n\nEnter any URL to check connection to:",
                                                       title, Messages.getQuestionIcon(), "http://", null);
        if (StringUtil.isEmptyOrSpaces(answer)) {
          return;
        }

        final HttpProxyManagerImpl settings = HttpProxyManagerImpl.getInstance();
        apply(settings);
        final AtomicReference<IOException> exceptionReference = new AtomicReference<>();
        myCheckButton.setEnabled(false);
        myCheckButton.setText("Check connection (in progress...)");
        myConnectionCheckInProgress = true;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            //already checked for null above
            //noinspection ConstantConditions
            HttpRequests.request(answer)
                    .readTimeout(3 * 1000)
                    .tryConnect();
          }
          catch (IOException e1) {
            exceptionReference.set(e1);
          }

          //noinspection SSBasedInspection
          SwingUtilities.invokeLater(() -> {
            myConnectionCheckInProgress = false;
            reset(settings);  // since password might have been set
            Component parent;
            if (myMainPanel.isShowing()) {
              parent = myMainPanel;
              myCheckButton.setText("Check connection");
              myCheckButton.setEnabled(canEnableConnectionCheck());
            }
            else {
              FocusableFrame frame = IdeFocusManager.findInstance().getLastFocusedFrame();
              if (frame == null) {
                return;
              }
              parent = frame.getComponent();
            }
            //noinspection ThrowableResultOfMethodCallIgnored
            final IOException exception = exceptionReference.get();
            if (exception == null) {
              Messages.showMessageDialog(parent, "Connection successful", title, Messages.getInformationIcon());
            }
            else {
              final String message = exception.getMessage();
              if (settings.getState().USE_HTTP_PROXY) {
                settings.getState().LAST_ERROR = message;
              }
              Messages.showErrorDialog(parent, errorText(message));
            }
          });
        });
      }
    });
  }

  private boolean canEnableConnectionCheck() {
    return !myNoProxyRb.isSelected() && !myConnectionCheckInProgress;
  }

  @Override
  public void reset(@Nonnull HttpProxyManagerImpl settings) {
    HttpProxyManagerState state = settings.getState();

    myNoProxyRb.setSelected(true);  // default
    myAutoDetectProxyRb.setSelected(state.USE_PROXY_PAC);
    myPacUrlCheckBox.setSelected(state.USE_PAC_URL);
    myPacUrlTextField.setText(state.PAC_URL);
    myUseHTTPProxyRb.setSelected(state.USE_HTTP_PROXY);
    myProxyAuthCheckBox.setSelected(state.PROXY_AUTHENTICATION);

    enableProxy(state.USE_HTTP_PROXY);

    myProxyLoginTextField.setText(settings.getProxyLogin());
    myProxyPasswordTextField.setText(settings.getPlainProxyPassword());

    myProxyPortTextField.setNumber(state.PROXY_PORT);
    myProxyHostTextField.setText(state.PROXY_HOST);
    myProxyExceptions.setText(StringUtil.notNullize(state.PROXY_EXCEPTIONS));

    myRememberProxyPasswordCheckBox.setSelected(state.KEEP_PROXY_PASSWORD);
    mySocks.setSelected(state.PROXY_TYPE_IS_SOCKS);
    myHTTP.setSelected(!state.PROXY_TYPE_IS_SOCKS);

    boolean showError = !StringUtil.isEmptyOrSpaces(state.LAST_ERROR);
    myErrorLabel.setVisible(showError);
    myErrorLabel.setText(showError ? errorText(state.LAST_ERROR) : null);

    final String oldStyleText = CommonProxy.getMessageFromProps(CommonProxy.getOldStyleProperties());
    myOtherWarning.setVisible(oldStyleText != null);
    if (oldStyleText != null) {
      myOtherWarning.setText(oldStyleText);
      myOtherWarning.setIcon(TargetAWT.to(Messages.getWarningIcon()));
    }
  }

  @Nonnull
  private static String errorText(@Nonnull String s) {
    return "Problem with connection: " + s;
  }

  private boolean isValid() {
    if (myUseHTTPProxyRb.isSelected()) {
      String host = getText(myProxyHostTextField);
      if (host == null) {
        return false;
      }

      try {
        HostAndPort parsedHost = HostAndPort.fromString(host);
        if (parsedHost.hasPort()) {
          return false;
        }
        host = parsedHost.getHost();

        try {
          InetAddresses.forString(host);
          return true;
        }
        catch (IllegalArgumentException e) {
          // it is not an IPv4 or IPv6 literal
        }

        InternetDomainName.from(host);
      }
      catch (IllegalArgumentException e) {
        return false;
      }

      if (myProxyAuthCheckBox.isSelected()) {
        return !StringUtil.isEmptyOrSpaces(myProxyLoginTextField.getText()) && myProxyPasswordTextField.getPassword().length > 0;
      }
    }
    return true;
  }

  @Override
  public void apply(@Nonnull HttpProxyManagerImpl settings) {
    if (!isValid()) {
      return;
    }

    if (isModified(settings)) {
      settings.AUTHENTICATION_CANCELLED = false;
    }

    HttpProxyManagerState state = settings.getState();
    state.USE_PROXY_PAC = myAutoDetectProxyRb.isSelected();
    state.USE_PAC_URL = myPacUrlCheckBox.isSelected();
    state.PAC_URL = getText(myPacUrlTextField);
    state.USE_HTTP_PROXY = myUseHTTPProxyRb.isSelected();
    state.PROXY_TYPE_IS_SOCKS = mySocks.isSelected();
    state.PROXY_AUTHENTICATION = myProxyAuthCheckBox.isSelected();
    state.KEEP_PROXY_PASSWORD = myRememberProxyPasswordCheckBox.isSelected();

    settings.setProxyLogin(getText(myProxyLoginTextField));
    settings.setPlainProxyPassword(new String(myProxyPasswordTextField.getPassword()));
    state.PROXY_EXCEPTIONS = StringUtil.nullize(myProxyExceptions.getText(), true);

    state.PROXY_PORT = myProxyPortTextField.getNumber();
    state.PROXY_HOST = getText(myProxyHostTextField);
  }

  @Nullable
  private static String getText(@Nonnull JTextField field) {
    return StringUtil.nullize(field.getText(), true);
  }

  private void enableProxy(boolean enabled) {
    myHostNameLabel.setEnabled(enabled);
    myPortNumberLabel.setEnabled(enabled);
    myProxyHostTextField.setEnabled(enabled);
    myProxyPortTextField.setEnabled(enabled);
    mySocks.setEnabled(enabled);
    myHTTP.setEnabled(enabled);
    myProxyExceptions.setEnabled(enabled);
    myProxyExceptionsLabel.setEnabled(enabled);
    myNoProxyForLabel.setEnabled(enabled);

    myProxyAuthCheckBox.setEnabled(enabled);
    enableProxyAuthentication(enabled && myProxyAuthCheckBox.isSelected());
    myCheckButton.setEnabled(canEnableConnectionCheck());

    final boolean autoDetectProxy = myAutoDetectProxyRb.isSelected();
    myPacUrlCheckBox.setEnabled(autoDetectProxy);
    myClearPasswordsButton.setEnabled(autoDetectProxy);
    myPacUrlTextField.setEnabled(autoDetectProxy && myPacUrlCheckBox.isSelected());
  }

  private void enableProxyAuthentication(boolean enabled) {
    myProxyPasswordLabel.setEnabled(enabled);
    myProxyLoginLabel.setEnabled(enabled);

    myProxyLoginTextField.setEnabled(enabled);
    myProxyPasswordTextField.setEnabled(enabled);

    myRememberProxyPasswordCheckBox.setEnabled(enabled);
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return myMainPanel;
  }
}
