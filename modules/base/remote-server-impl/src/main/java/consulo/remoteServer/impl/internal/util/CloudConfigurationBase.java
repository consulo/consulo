// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.util;

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.PasswordSafe;
import consulo.http.HttpProxyManager;
import consulo.remoteServer.agent.shared.CloudAgentConfigBase;
import consulo.remoteServer.agent.shared.CloudProxySettings;
import consulo.remoteServer.configuration.ServerConfigurationBase;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author michael.golubev
 */
public class CloudConfigurationBase<Self extends CloudConfigurationBase<Self>>
    extends ServerConfigurationBase<Self> implements CloudAgentConfigBase {

    private String myEmail;

    private String myPassword;

    @Override
    @Attribute("email")
    public String getEmail() {
        return myEmail;
    }

    public void setEmail(String email) {
        myEmail = email;
    }

    @Override
    @Attribute("password")
    public String getPassword() {
        return myPassword;
    }

    public void setPassword(String password) {
        myPassword = password;
    }

    @Transient
    @Override
    public CloudProxySettings getProxySettings() {
        final HttpProxyManager httpConfigurable = HttpProxyManager.getInstance();
        return new CloudProxySettings() {

            @Override
            public boolean useHttpProxy() {
                return httpConfigurable.isHttpProxyEnabled();
            }

            @Override
            public String getHost() {
                return httpConfigurable.getProxyHost();
            }

            @Override
            public int getPort() {
                return httpConfigurable.getProxyPort();
            }

            @Override
            public boolean useAuthentication() {
                return httpConfigurable.isProxyAuthenticationEnabled();
            }

            @Override
            public String getLogin() {
                return httpConfigurable.getProxyLogin();
            }

            @Override
            public String getPassword() {
                return httpConfigurable.getPlainProxyPassword();
            }
        };
    }

    @Transient
    public boolean isPasswordSafe() {
        CredentialAttributes credentialAttributes = createCredentialAttributes();
        return credentialAttributes != null && PasswordSafe.getInstance().get(credentialAttributes) != null;
    }

    protected @Nullable CredentialAttributes createCredentialAttributes() {
        return createCredentialAttributes(getServiceName(), getCredentialUser());
    }

    @Transient
    public void setPasswordSafe(String password) {
        doSetSafeValue(createCredentialAttributes(), getCredentialUser(), password, this::setPassword);
    }

    @Transient
    @Override
    public String getPasswordSafe() {
        return doGetSafeValue(createCredentialAttributes(), this::getPassword);
    }

    /**
     * Service name for {@link #getPassword()} when stored in the {@link PasswordSafe}
     */
    @Transient
    protected @Nullable String getServiceName() {
        return null;
    }

    @Transient
    protected @Nullable String getCredentialUser() {
        return getEmail();
    }

    protected static void doSetSafeValue(@Nullable CredentialAttributes credentialAttributes,
                                         @Nullable String credentialUser,
                                         @Nullable String secretValue,
                                         @Nonnull Consumer<? super String> unsafeSetter) {

        CloudConfigurationUtil.doSetSafeValue(credentialAttributes, credentialUser, secretValue, unsafeSetter);
    }

    protected static String doGetSafeValue(@Nullable CredentialAttributes credentialAttributes, @Nonnull Supplier<String> unsafeGetter) {
        return CloudConfigurationUtil.doGetSafeValue(credentialAttributes, unsafeGetter);
    }

    protected static boolean hasSafeCredentials(@Nullable CredentialAttributes credentialAttributes) {
        return CloudConfigurationUtil.hasSafeCredentials(credentialAttributes);
    }

    protected static @Nullable CredentialAttributes createCredentialAttributes(String serviceName, String credentialsUser) {
        return CloudConfigurationUtil.createCredentialAttributes(serviceName, credentialsUser);
    }

    public boolean shouldMigrateToPasswordSafe() {
        return !StringUtil.isEmpty(getPassword());
    }

    public void migrateToPasswordSafe() {
        final String unsafePassword = getPassword();
        if (!StringUtil.isEmpty(unsafePassword)) {
            setPasswordSafe(unsafePassword);
        }
    }
}
