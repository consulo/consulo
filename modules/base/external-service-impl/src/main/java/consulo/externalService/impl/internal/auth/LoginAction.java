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
package consulo.externalService.impl.internal.auth;

import consulo.application.dumb.DumbAware;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.externalService.impl.internal.ExternalServiceConfigurationImpl;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Alerts;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.RightAlignedToolbarAction;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Provider;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2017-03-01
 */
public class LoginAction extends AnAction implements RightAlignedToolbarAction, DumbAware {
    private final Provider<ExternalServiceConfiguration> myExternalServiceConfigurationProvider;

    public LoginAction(Provider<ExternalServiceConfiguration> externalServiceConfigurationProvider) {
        super(LocalizeValue.localizeTODO("Login"), LocalizeValue.empty(), PlatformIconGroup.actionsLoginavatar());
        myExternalServiceConfigurationProvider = externalServiceConfigurationProvider;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        ExternalServiceConfiguration configuration = myExternalServiceConfigurationProvider.get();

        Presentation presentation = e.getPresentation();

        String email = configuration.getEmail();
        if (email == null) {
            presentation.setText("Not authorized...");
            presentation.setIcon(PlatformIconGroup.actionsLoginavatar());
        }
        else {
            presentation.setTextValue(LocalizeValue.of(email));

            Image userIcon = configuration.getUserIcon();
            presentation.setIcon(ObjectUtil.notNull(userIcon, PlatformIconGroup.actionsLoginavatar()));
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ExternalServiceConfiguration configuration = myExternalServiceConfigurationProvider.get();

        if (configuration.getEmail() != null) {
            Alerts.yesNo().asWarning().text(LocalizeValue.localizeTODO("Do logout?")).showAsync().doWhenDone(value -> {
                if (value) {
                    // call internal implementation
                    ((ExternalServiceConfigurationImpl) configuration).reset();
                }
            });
        }
        else {
            String tokenForAuth = RandomStringUtils.randomAlphabetic(48);

            int localPort = BuiltInServerManager.getInstance().getPort();
            String redirectUrl = "http://localhost:" + localPort + "/redirectAuth";

            try {
                URIBuilder builder0 = new URIBuilder(WebServiceApi.LINK_CONSULO.buildUrl(), StandardCharsets.UTF_8)
                    .addParameter("token", tokenForAuth)
                    .addParameter("host", getHostName())
                    .addParameter("redirect", redirectUrl);
                Platform.current().openInBrowser(builder0.toString());
            }
            catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private String getHostName() {
        String hostname = "Unknown";

        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        }
        catch (UnknownHostException ignored) {
        }

        return hostname;
    }
}
