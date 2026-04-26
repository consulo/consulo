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
package consulo.http.impl.internal.proxy;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.io.HostAndPort;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.application.Application;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.configurable.IdeaConfigurableUi;
import consulo.disposer.Disposable;
import consulo.http.HttpRequests;
import consulo.http.localize.HttpLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Button;
import consulo.ui.Label;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.Component;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

class HttpProxySettingsUi implements IdeaConfigurableUi<HttpProxyManagerImpl> {
    private static final Pattern PROXY_EXCLUDES_DELIM_PATTERN = Pattern.compile(",\\s*");

    private JPanel myMainPanel;

    private TextBox myProxyLoginTextField;
    private PasswordBox myProxyPasswordTextField;
    private CheckBox myProxyAuthCheckBox;
    private IntBox myProxyPortTextField;
    private TextBox myProxyHostTextField;
    private CheckBox myRememberProxyPasswordCheckBox;

    private Label myProxyLoginLabel;
    private Label myProxyPasswordLabel;
    private Label myHostNameLabel;
    private Label myPortNumberLabel;
    private RadioButton myAutoDetectProxyRb;
    private RadioButton myUseHTTPProxyRb;
    private Label mySystemProxyDefined;
    private RadioButton myNoProxyRb;
    private RadioButton myHTTP;
    private RadioButton mySocks;
    private Button myClearPasswordsButton;
    private Label myErrorLabel;
    private Button myCheckButton;
    private Label myOtherWarning;
    private Label myProxyExceptionsLabel;
    private TextBoxWithExpandAction myProxyExceptions;
    private Label myNoProxyForLabel;
    private CheckBox myPacUrlCheckBox;
    private TextBox myPacUrlTextField;
    private volatile boolean myConnectionCheckInProgress;

    @Override
    public boolean isModified(HttpProxyManagerImpl settings) {
        if (!isValid()) {
            return false;
        }

        HttpProxyManagerState state = settings.getState();
        return !Objects.equals(StringUtil.trimToNull(myProxyExceptions.getValue()), StringUtil.trimToNull(state.PROXY_EXCEPTIONS))
            || state.USE_PROXY_PAC != myAutoDetectProxyRb.getValue()
            || state.USE_PAC_URL != myPacUrlCheckBox.getValue()
            || !Objects.equals(state.PAC_URL, myPacUrlTextField.getValue())
            || state.USE_HTTP_PROXY != myUseHTTPProxyRb.getValue()
            || state.PROXY_AUTHENTICATION != myProxyAuthCheckBox.getValue()
            || state.KEEP_PROXY_PASSWORD != myRememberProxyPasswordCheckBox.getValue()
            || state.PROXY_TYPE_IS_SOCKS != mySocks.getValue()
            || !Objects.equals(settings.getProxyLogin(), myProxyLoginTextField.getValue())
            || !Objects.equals(settings.getPlainProxyPassword(), myProxyPasswordTextField.getValue())
            || state.PROXY_PORT != myProxyPortTextField.getValue()
            || !Objects.equals(state.PROXY_HOST, myProxyHostTextField.getValue());
    }

    @RequiredUIAccess
    public HttpProxySettingsUi(HttpProxyManagerImpl settings) {
        $$$setupUI$$$();

        ValueGroup<Boolean> group = ValueGroup.createBool();
        group.add(myUseHTTPProxyRb);
        group.add(myAutoDetectProxyRb);
        group.add(myNoProxyRb);
        myNoProxyRb.setValue(true);

        ValueGroup<Boolean> proxyTypeGroup = ValueGroup.createBool();
        proxyTypeGroup.add(myHTTP);
        proxyTypeGroup.add(mySocks);
        myHTTP.setValue(true);

        Boolean property = Boolean.getBoolean(JavaProxyProperty.USE_SYSTEM_PROXY);
        mySystemProxyDefined.setVisible(Boolean.TRUE.equals(property));
        if (Boolean.TRUE.equals(property)) {
            mySystemProxyDefined.setImage(UIUtil.getWarningIcon());
//            RelativeFont.BOLD.install(mySystemProxyDefined);
        }

        myProxyAuthCheckBox.addClickListener(e -> enableProxyAuthentication(myProxyAuthCheckBox.getValue()));
        myPacUrlCheckBox.addClickListener(e -> myPacUrlTextField.setEnabled(myPacUrlCheckBox.getValue()));

        ComponentEventListener<consulo.ui.Component, ClickEvent> listener = e -> enableProxy(myUseHTTPProxyRb.getValue());
        myUseHTTPProxyRb.addClickListener(listener);
        myAutoDetectProxyRb.addClickListener(listener);
        myNoProxyRb.addClickListener(listener);

        myClearPasswordsButton.addClickListener(e -> {
            settings.clearGenericPasswords();
            //noinspection DialogTitleCapitalization
            Messages.showMessageDialog(
                myMainPanel,
                "Proxy passwords were cleared.",
                "Auto-detected Proxy",
                UIUtil.getInformationIcon()
            );
        });

        configureCheckButton();
    }

    @RequiredUIAccess
    private void configureCheckButton() {
        if (HttpProxyManagerImpl.getInstance() == null) {
            myCheckButton.setVisible(false);
            return;
        }

        myCheckButton.addClickListener(e -> {
            String title = "Check Proxy Settings";
            String answer = Messages.showInputDialog(
                myMainPanel,
                "Warning: your settings will be saved.\n\nEnter any URL to check connection to:",
                title,
                UIUtil.getQuestionIcon(),
                "http://",
                null
            );
            if (StringUtil.isEmptyOrSpaces(answer)) {
                return;
            }

            HttpProxyManagerImpl settings = HttpProxyManagerImpl.getInstance();
            apply(settings);
            AtomicReference<IOException> exceptionReference = new AtomicReference<>();
            myCheckButton.setEnabled(false);
            myCheckButton.setText(LocalizeValue.localizeTODO("Check connection (in progress...)"));
            myConnectionCheckInProgress = true;
            Application.get().executeOnPooledThread(() -> {
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
                        myCheckButton.setText(HttpLocalize.proxyTestButton());
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
                    IOException exception = exceptionReference.get();
                    if (exception == null) {
                        Messages.showMessageDialog(parent, "Connection successful", title, UIUtil.getInformationIcon());
                    }
                    else {
                        String message = StringUtil.notNullize(exception.getMessage(), "N/A");
                        if (settings.getState().USE_HTTP_PROXY) {
                            settings.getState().LAST_ERROR = message;
                        }
                        Messages.showErrorDialog(parent, errorText(message).get());
                    }
                });
            });
        });
    }

    private boolean canEnableConnectionCheck() {
        return !myNoProxyRb.getValue() && !myConnectionCheckInProgress;
    }

    @Override
    @RequiredUIAccess
    public void reset(HttpProxyManagerImpl settings) {
        HttpProxyManagerState state = settings.getState();

        myNoProxyRb.setValue(true);  // default
        myAutoDetectProxyRb.setValue(state.USE_PROXY_PAC);
        myPacUrlCheckBox.setValue(state.USE_PAC_URL);
        myPacUrlTextField.setValue(state.PAC_URL);
        myUseHTTPProxyRb.setValue(state.USE_HTTP_PROXY);
        myProxyAuthCheckBox.setValue(state.PROXY_AUTHENTICATION);

        enableProxy(state.USE_HTTP_PROXY);

        myProxyLoginTextField.setValue(settings.getProxyLogin());
        myProxyPasswordTextField.setValue(settings.getPlainProxyPassword());

        myProxyPortTextField.setValue(state.PROXY_PORT);
        myProxyHostTextField.setValue(state.PROXY_HOST);
        myProxyExceptions.setValue(StringUtil.notNullize(state.PROXY_EXCEPTIONS));

        myRememberProxyPasswordCheckBox.setValue(state.KEEP_PROXY_PASSWORD);
        mySocks.setValue(state.PROXY_TYPE_IS_SOCKS);
        myHTTP.setValue(!state.PROXY_TYPE_IS_SOCKS);

        boolean showError = !StringUtil.isEmptyOrSpaces(state.LAST_ERROR);
        myErrorLabel.setVisible(showError);
        myErrorLabel.setText(showError ? errorText(state.LAST_ERROR) : LocalizeValue.empty());

        String oldStyleText = CommonProxy.getMessageFromProps(CommonProxy.getOldStyleProperties());
        myOtherWarning.setVisible(oldStyleText != null);
        if (oldStyleText != null) {
            myOtherWarning.setText(oldStyleText);
            myOtherWarning.setImage(UIUtil.getWarningIcon());
        }
    }

    private static LocalizeValue errorText(String s) {
        return LocalizeValue.join(LocalizeValue.of("Problem with connection: "), LocalizeValue.of(s));
    }

    private boolean isValid() {
        if (myUseHTTPProxyRb.getValue()) {
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

            if (myProxyAuthCheckBox.getValue()) {
                return !StringUtil.isEmptyOrSpaces(myProxyLoginTextField.getValue())
                    && StringUtil.isNotEmpty(myProxyPasswordTextField.getValue());
            }
        }
        return true;
    }

    @Override
    public void apply(HttpProxyManagerImpl settings) {
        if (!isValid()) {
            return;
        }

        if (isModified(settings)) {
            settings.AUTHENTICATION_CANCELLED = false;
        }

        HttpProxyManagerState state = settings.getState();
        state.USE_PROXY_PAC = myAutoDetectProxyRb.getValue();
        state.USE_PAC_URL = myPacUrlCheckBox.getValue();
        state.PAC_URL = getText(myPacUrlTextField);
        state.USE_HTTP_PROXY = myUseHTTPProxyRb.getValue();
        state.PROXY_TYPE_IS_SOCKS = mySocks.getValue();
        state.PROXY_AUTHENTICATION = myProxyAuthCheckBox.getValue();
        state.KEEP_PROXY_PASSWORD = myRememberProxyPasswordCheckBox.getValue();

        settings.setProxyLogin(getText(myProxyLoginTextField));
        settings.setPlainProxyPassword(myProxyPasswordTextField.getValue());
        state.PROXY_EXCEPTIONS = StringUtil.nullize(myProxyExceptions.getValue(), true);

        state.PROXY_PORT = myProxyPortTextField.getValue();
        state.PROXY_HOST = getText(myProxyHostTextField);
    }

    private static @Nullable String getText(TextBox textBox) {
        return StringUtil.nullize(textBox.getValue(), true);
    }

    private static @Nullable String getText(JTextField field) {
        return StringUtil.nullize(field.getText(), true);
    }

    @RequiredUIAccess
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
        enableProxyAuthentication(enabled && myProxyAuthCheckBox.getValue());
        myCheckButton.setEnabled(canEnableConnectionCheck());

        boolean autoDetectProxy = myAutoDetectProxyRb.getValue();
        myPacUrlCheckBox.setEnabled(autoDetectProxy);
        myClearPasswordsButton.setEnabled(autoDetectProxy);
        myPacUrlTextField.setEnabled(autoDetectProxy && myPacUrlCheckBox.getValue());
    }

    @RequiredUIAccess
    private void enableProxyAuthentication(boolean enabled) {
        myProxyPasswordLabel.setEnabled(enabled);
        myProxyLoginLabel.setEnabled(enabled);

        myProxyLoginTextField.setEnabled(enabled);
        myProxyPasswordTextField.setEnabled(enabled);

        myRememberProxyPasswordCheckBox.setEnabled(enabled);
    }

    @Override
    public JComponent getComponent(Disposable disposable) {
        return myMainPanel;
    }

    /**
     * Method generated by Consulo GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    @RequiredUIAccess
    private void $$$setupUI$$$() {
        myMainPanel = new JPanel();
        myMainPanel.setLayout(new GridLayoutManager(12, 1, JBUI.emptyInsets(), -1, -1));
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(8, 2, JBUI.emptyInsets(), -1, -1));
        myMainPanel.add(
            panel1,
            new GridConstraints(
                8,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myProxyPortTextField = IntBox.create().withRange(0, 65535);
        panel1.add(
            TargetAWT.to(myProxyPortTextField),
            new GridConstraints(
                1,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myProxyLoginTextField = TextBox.create("");
        panel1.add(
            TargetAWT.to(myProxyLoginTextField),
            new GridConstraints(
                5,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(150, -1),
                null,
                0,
                false
            )
        );
        myProxyPasswordTextField = PasswordBox.create();
        panel1.add(
            TargetAWT.to(myProxyPasswordTextField),
            new GridConstraints(
                6,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(150, -1),
                null,
                0,
                false
            )
        );
        myProxyExceptionsLabel = Label.create(HttpLocalize.proxyManualExcludeExample());
        panel1.add(
            TargetAWT.to(myProxyExceptionsLabel),
            new GridConstraints(
                3,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myProxyHostTextField = TextBox.create();
        panel1.add(
            TargetAWT.to(myProxyHostTextField),
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(150, -1),
                null,
                0,
                false
            )
        );
        myRememberProxyPasswordCheckBox = CheckBox.create(CommonLocalize.checkboxRememberPassword());
        panel1.add(
            TargetAWT.to(myRememberProxyPasswordCheckBox),
            new GridConstraints(
                7,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myHostNameLabel = Label.create(HttpLocalize.proxyManualHost());
        panel1.add(
            TargetAWT.to(myHostNameLabel),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        myPortNumberLabel = Label.create(HttpLocalize.proxyManualPort());
        panel1.add(
            TargetAWT.to(myPortNumberLabel),
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        myProxyAuthCheckBox = CheckBox.create(HttpLocalize.proxyManualAuth());
        myProxyAuthCheckBox.setValue(false);
        panel1.add(
            TargetAWT.to(myProxyAuthCheckBox),
            new GridConstraints(
                4,
                0,
                1,
                2,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        myProxyLoginLabel = Label.create(CommonLocalize.editboxLogin());
        panel1.add(
            TargetAWT.to(myProxyLoginLabel),
            new GridConstraints(
                5,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                4,
                false
            )
        );
        myProxyPasswordLabel = Label.create(CommonLocalize.editboxPassword());
        panel1.add(
            TargetAWT.to(myProxyPasswordLabel),
            new GridConstraints(
                6,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                4,
                false
            )
        );
        myProxyExceptions = TextBoxWithExpandAction.create(
            PlatformIconGroup.generalExpandcomponent(),
            "",
            string -> Arrays.asList(PROXY_EXCLUDES_DELIM_PATTERN.split(string)),
            strings -> StringUtil.join(strings, ", ")
        );
        panel1.add(
            TargetAWT.to(myProxyExceptions),
            new GridConstraints(
                2,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myNoProxyForLabel = Label.create(HttpLocalize.proxyManualExclude());
        panel1.add(
            TargetAWT.to(myNoProxyForLabel),
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        Spacer spacer1 = new Spacer();
        myMainPanel.add(
            spacer1,
            new GridConstraints(
                11,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myAutoDetectProxyRb = RadioButton.create(HttpLocalize.proxyPacRb());
        myAutoDetectProxyRb.setToolTipText(HttpLocalize.proxyPacRbTt());
        myMainPanel.add(
            TargetAWT.to(myAutoDetectProxyRb),
            new GridConstraints(
                3,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myUseHTTPProxyRb = RadioButton.create(HttpLocalize.proxyManualRb());
        myMainPanel.add(
            TargetAWT.to(myUseHTTPProxyRb),
            new GridConstraints(
                6,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        mySystemProxyDefined = Label.create(HttpLocalize.proxySystemLabel());
//        mySystemProxyDefined.setVerticalAlignment(0);
//        mySystemProxyDefined.setVerticalTextPosition(1);
        myMainPanel.add(
            TargetAWT.to(mySystemProxyDefined),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myNoProxyRb = RadioButton.create(HttpLocalize.proxyDirectRb());
        myMainPanel.add(
            TargetAWT.to(myNoProxyRb),
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
        myMainPanel.add(
            panel2,
            new GridConstraints(
                7,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_VERTICAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myHTTP = RadioButton.create(HttpLocalize.proxyManualTypeHttp());
        panel2.add(
            TargetAWT.to(myHTTP),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        mySocks = RadioButton.create(HttpLocalize.proxyManualTypeSocks());
        panel2.add(
            TargetAWT.to(mySocks),
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        myErrorLabel = Label.create();
        myMainPanel.add(
            TargetAWT.to(myErrorLabel),
            new GridConstraints(
                10,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myCheckButton = Button.create(HttpLocalize.proxyTestButton());
        myMainPanel.add(
            TargetAWT.to(myCheckButton),
            new GridConstraints(
                9,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myOtherWarning = Label.create();
//        myOtherWarning.setVerticalTextPosition(1);
        myMainPanel.add(
            TargetAWT.to(myOtherWarning),
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
        myMainPanel.add(
            panel3,
            new GridConstraints(
                4,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myPacUrlCheckBox = CheckBox.create(HttpLocalize.proxyPacUrlLabel());
        panel3.add(
            TargetAWT.to(myPacUrlCheckBox),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        myPacUrlTextField = TextBox.create();
        panel3.add(
            TargetAWT.to(myPacUrlTextField),
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(150, -1),
                null,
                0,
                false
            )
        );
        myClearPasswordsButton = Button.create(HttpLocalize.proxyPacPwClearButton());
        myMainPanel.add(
            TargetAWT.to(myClearPasswordsButton),
            new GridConstraints(
                5,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                2,
                false
            )
        );
        myHostNameLabel.setTarget(myProxyHostTextField);
        myPortNumberLabel.setTarget(myProxyPortTextField);
        myProxyLoginLabel.setTarget(myProxyLoginTextField);
        myProxyPasswordLabel.setTarget(myProxyPasswordTextField);
    }

    public JComponent $$$getRootComponent$$$() {
        return myMainPanel;
    }
}
