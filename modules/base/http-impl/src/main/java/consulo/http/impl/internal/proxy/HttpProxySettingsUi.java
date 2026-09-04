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

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import consulo.application.Application;
import consulo.http.HttpRequests;
import consulo.http.localize.HttpLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.Indenter;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.io.HostAndPort;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

class HttpProxySettingsUi implements Supplier<Layout> {
    private static final int PROXY_NONE = 0;
    private static final int PROXY_AUTO_DETECT = 1;
    private static final int PROXY_MANUAL = 2;

    private static final Pattern PROXY_EXCLUDES_DELIM_PATTERN = Pattern.compile(",\\s*");

    private Layout myRoot;

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
    private final RadioGroup<Integer> myProxyModeGroup = RadioGroup.create();
    private final RadioGroup<Boolean> myProxyTypeGroup = RadioGroup.create();

    private RadioButton myAutoDetectProxyRb;
    private RadioButton myUseHTTPProxyRb;
    private HtmlLabel mySystemProxyDefined;
    private RadioButton myNoProxyRb;
    private RadioButton myHTTP;
    private RadioButton mySocks;
    private Button myClearPasswordsButton;
    private Label myErrorLabel;
    private Button myCheckButton;
    private HtmlLabel myOtherWarning;
    private Label myProxyExceptionsLabel;
    private TextBoxWithExpandAction myProxyExceptions;
    private Label myNoProxyForLabel;
    private CheckBox myPacUrlCheckBox;
    private TextBox myPacUrlTextField;
    private volatile boolean myConnectionCheckInProgress;

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
        myRoot = buildLayout();

        myProxyModeGroup.setValue(PROXY_NONE);
        myProxyTypeGroup.setValue(false);

        Boolean property = Boolean.getBoolean(JavaProxyProperty.USE_SYSTEM_PROXY);
        mySystemProxyDefined.setVisible(Boolean.TRUE.equals(property));
        if (Boolean.TRUE.equals(property)) {
            mySystemProxyDefined.setImage(UIUtil.getWarningIcon());
//            RelativeFont.BOLD.install(mySystemProxyDefined);
        }

        myProxyAuthCheckBox.addValueListener(e -> enableProxyAuthentication(myProxyAuthCheckBox.getValue()));
        myPacUrlCheckBox.addValueListener(e -> myPacUrlTextField.setEnabled(myPacUrlCheckBox.getValue()));

        myProxyModeGroup.addValueListener(e -> enableProxy(myUseHTTPProxyRb.getValue()));

        myClearPasswordsButton.addClickListener(e -> {
            settings.clearGenericPasswords();
            //noinspection DialogTitleCapitalization
            Messages.showMessageDialog(
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
                (Project) null,
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
            UIAccess uiAccess = UIAccess.current();
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

                uiAccess.give(() -> {
                    myConnectionCheckInProgress = false;
                    reset(settings);  // since password might have been set
                    myCheckButton.setText(HttpLocalize.proxyTestButton());
                    myCheckButton.setEnabled(canEnableConnectionCheck());

                    IOException exception = exceptionReference.get();
                    if (exception == null) {
                        Messages.showMessageDialog("Connection successful", title, UIUtil.getInformationIcon());
                    }
                    else {
                        String message = StringUtil.notNullize(exception.getMessage(), "N/A");
                        if (settings.getState().USE_HTTP_PROXY) {
                            settings.getState().LAST_ERROR = message;
                        }
                        Messages.showErrorDialog(errorText(message).get(), title);
                    }
                });
            });
        });
    }

    private boolean canEnableConnectionCheck() {
        return !myNoProxyRb.getValue() && !myConnectionCheckInProgress;
    }

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

    @RequiredUIAccess
    private Layout buildLayout() {
        VerticalLayout root = VerticalLayout.create();

        mySystemProxyDefined = HtmlLabel.create(HttpLocalize.proxySystemLabel());
        myOtherWarning = HtmlLabel.create(LocalizeValue.empty());
        root.add(mySystemProxyDefined);
        root.add(myOtherWarning);

        myNoProxyRb = myProxyModeGroup.newButton(HttpLocalize.proxyDirectRb(), PROXY_NONE);
        root.add(myNoProxyRb);

        myAutoDetectProxyRb = myProxyModeGroup.newButton(HttpLocalize.proxyPacRb(), PROXY_AUTO_DETECT);
        myAutoDetectProxyRb.setToolTipText(HttpLocalize.proxyPacRbTt());
        root.add(myAutoDetectProxyRb);

        myPacUrlCheckBox = CheckBox.create(HttpLocalize.proxyPacUrlLabel());
        myPacUrlTextField = TextBox.create();
        myClearPasswordsButton = Button.create(HttpLocalize.proxyPacPwClearButton());

        VerticalLayout autoDetect = VerticalLayout.create();
        autoDetect.add(DockLayout.create(Space.SMALL).left(myPacUrlCheckBox).center(myPacUrlTextField));
        autoDetect.add(DockLayout.create().left(myClearPasswordsButton));
        root.add(Indenter.indent(autoDetect));

        myUseHTTPProxyRb = myProxyModeGroup.newButton(HttpLocalize.proxyManualRb(), PROXY_MANUAL);
        root.add(myUseHTTPProxyRb);

        myHTTP = myProxyTypeGroup.newButton(HttpLocalize.proxyManualTypeHttp(), false);
        mySocks = myProxyTypeGroup.newButton(HttpLocalize.proxyManualTypeSocks(), true);

        myHostNameLabel = Label.create(HttpLocalize.proxyManualHost());
        myProxyHostTextField = TextBox.create();
        myPortNumberLabel = Label.create(HttpLocalize.proxyManualPort());
        myProxyPortTextField = IntBox.create().withRange(0, 65535);
        myNoProxyForLabel = Label.create(HttpLocalize.proxyManualExclude());
        myProxyExceptions = TextBoxWithExpandAction.create(
            PlatformIconGroup.generalExpandcomponent(),
            "",
            string -> Arrays.asList(PROXY_EXCLUDES_DELIM_PATTERN.split(string)),
            strings -> StringUtil.join(strings, ", ")
        );
        myProxyExceptionsLabel = Label.create(HttpLocalize.proxyManualExcludeExample());
        myProxyAuthCheckBox = CheckBox.create(HttpLocalize.proxyManualAuth());
        myProxyLoginLabel = Label.create(CommonLocalize.editboxLogin());
        myProxyLoginTextField = TextBox.create();
        myProxyPasswordLabel = Label.create(CommonLocalize.editboxPassword());
        myProxyPasswordTextField = PasswordBox.create();
        myRememberProxyPasswordCheckBox = CheckBox.create(CommonLocalize.checkboxRememberPassword());

        // one table rather than a row of layouts each sizing itself: the fields share a column, so the host
        // and the port are the same width, and so are the login and the password
        TableLayout fields = TableLayout.create(StaticPosition.TOP);
        fields.add(myHostNameLabel, TableLayout.cell(0, 0));
        fields.add(myProxyHostTextField, TableLayout.cell(0, 1).fill());
        fields.add(myPortNumberLabel, TableLayout.cell(1, 0));
        fields.add(myProxyPortTextField, TableLayout.cell(1, 1).fill());
        fields.add(myNoProxyForLabel, TableLayout.cell(2, 0));
        fields.add(myProxyExceptions, TableLayout.cell(2, 1).fill());
        fields.add(myProxyExceptionsLabel, TableLayout.cell(3, 1));
        fields.add(myProxyAuthCheckBox, TableLayout.cell(4, 0));
        fields.add(myProxyLoginLabel, TableLayout.cell(5, 0));
        fields.add(myProxyLoginTextField, TableLayout.cell(5, 1).fill());
        fields.add(myProxyPasswordLabel, TableLayout.cell(6, 0));
        fields.add(myProxyPasswordTextField, TableLayout.cell(6, 1).fill());
        fields.add(myRememberProxyPasswordCheckBox, TableLayout.cell(7, 1));

        VerticalLayout manual = VerticalLayout.create();
        manual.add(HorizontalLayout.create(Space.SMALL).add(myHTTP).add(mySocks));
        manual.add(fields);

        root.add(Indenter.indent(manual));

        myHostNameLabel.setTarget(myProxyHostTextField);
        myPortNumberLabel.setTarget(myProxyPortTextField);
        myProxyLoginLabel.setTarget(myProxyLoginTextField);
        myProxyPasswordLabel.setTarget(myProxyPasswordTextField);

        myCheckButton = Button.create(HttpLocalize.proxyTestButton());
        root.add(DockLayout.create().left(myCheckButton));

        myErrorLabel = Label.create();
        root.add(myErrorLabel);

        return root;
    }

    @Override
    public Layout get() {
        return myRoot;
    }
}
