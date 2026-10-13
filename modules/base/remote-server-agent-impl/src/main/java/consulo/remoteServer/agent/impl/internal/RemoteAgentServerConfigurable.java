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
package consulo.remoteServer.agent.impl.internal;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.remoteServer.RemoteServerConfigurable;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.PasswordBox;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;


class RemoteAgentServerConfigurable extends RemoteServerConfigurable {
    private final RemoteAgentServerConfiguration myConfiguration;
    private final RemoteAgentCredentials myCredentials;

    private @Nullable TextBox myHostField;
    private @Nullable TextBox myPortField;
    private @Nullable CheckBox myTlsBox;
    private @Nullable TextBox myFingerprintField;
    private @Nullable PasswordBox myTokenField;
    private String myStoredToken = "";

    RemoteAgentServerConfigurable(RemoteAgentServerConfiguration configuration, RemoteAgentCredentials credentials) {
        myConfiguration = configuration;
        myCredentials = credentials;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        TextBox hostField = TextBox.create();
        TextBox portField = TextBox.create();
        CheckBox tlsBox = CheckBox.create(LocalizeValue.localizeTODO("Use TLS"));
        TextBox fingerprintField = TextBox.create();
        fingerprintField.setPlaceholder(LocalizeValue.localizeTODO("SHA-256 printed by the agent, empty for a CA-signed certificate"));
        PasswordBox tokenField = PasswordBox.create();
        tlsBox.addValueListener(event -> fingerprintField.setEnabled(Boolean.TRUE.equals(event.getValue())));

        myHostField = hostField;
        myPortField = portField;
        myTlsBox = tlsBox;
        myFingerprintField = fingerprintField;
        myTokenField = tokenField;
        reset();

        return FormBuilder.create()
            .addLabeled(LocalizeValue.localizeTODO("Host:"), hostField)
            .addLabeled(LocalizeValue.localizeTODO("Port:"), portField)
            .addLabeled(LocalizeValue.localizeTODO("Transport:"), tlsBox)
            .addLabeled(LocalizeValue.localizeTODO("Certificate fingerprint:"), fingerprintField)
            .addLabeled(LocalizeValue.localizeTODO("Token:"), tokenField)
            .build();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (myHostField == null || myPortField == null || myTlsBox == null || myFingerprintField == null || myTokenField == null) {
            return false;
        }
        return !text(myHostField).equals(myConfiguration.getHost())
            || !text(myPortField).equals(String.valueOf(myConfiguration.getPort()))
            || myTlsBox.getValueOrError() != myConfiguration.isUseTls()
            || !text(myFingerprintField).equals(myConfiguration.getCertificateFingerprint())
            || !StringUtil.notNullize(myTokenField.getValue()).equals(myStoredToken);
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (myHostField == null || myPortField == null || myTlsBox == null || myFingerprintField == null || myTokenField == null) {
            return;
        }
        int port;
        try {
            port = Integer.parseInt(text(myPortField));
        }
        catch (NumberFormatException e) {
            throw new ConfigurationException(LocalizeValue.localizeTODO("Invalid port number"));
        }
        String fingerprint = RemoteAgentTrustManager.normalize(text(myFingerprintField));
        if (!fingerprint.isEmpty() && !fingerprint.matches("[0-9A-F]{64}")) {
            throw new ConfigurationException(LocalizeValue.localizeTODO("The certificate fingerprint must be a SHA-256 value"));
        }

        myConfiguration.setHost(text(myHostField));
        myConfiguration.setPort(port);
        myConfiguration.setUseTls(myTlsBox.getValueOrError());
        myConfiguration.setCertificateFingerprint(text(myFingerprintField));

        String newToken = StringUtil.notNullize(myTokenField.getValue());
        if (!newToken.equals(myStoredToken)) {
            myCredentials.setToken(myConfiguration, newToken);
            myStoredToken = newToken;
        }
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        if (myHostField == null || myPortField == null || myTlsBox == null || myFingerprintField == null || myTokenField == null) {
            return;
        }
        myHostField.setValue(myConfiguration.getHost());
        myPortField.setValue(String.valueOf(myConfiguration.getPort()));
        myTlsBox.setValue(myConfiguration.isUseTls());
        myFingerprintField.setValue(myConfiguration.getCertificateFingerprint());
        myFingerprintField.setEnabled(myConfiguration.isUseTls());

        PasswordBox tokenField = myTokenField;
        myCredentials.getTokenAsync(myConfiguration).whenCompleteAsync((token, error) -> {
            myStoredToken = token == null ? "" : token;
            tokenField.setValue(myStoredToken);
        }, UIAccess.current());
    }

    private static String text(TextBox textBox) {
        return StringUtil.notNullize(textBox.getValue()).trim();
    }
}
