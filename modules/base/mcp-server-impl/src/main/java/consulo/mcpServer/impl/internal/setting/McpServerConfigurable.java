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
package consulo.mcpServer.impl.internal.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.builtinWebServer.custom.CustomPortServerManager;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.mcp.tool.McpToolDescriptor;
import consulo.mcpServer.impl.internal.McpAuthTokenManager;
import consulo.mcpServer.impl.internal.McpPrincipal;
import consulo.mcpServer.impl.internal.McpSessionManager;
import consulo.mcpServer.impl.internal.oauth.McpOAuthService;
import consulo.mcpServer.impl.internal.McpToolRegistration;
import consulo.mcpServer.impl.internal.McpToolRegistry;
import consulo.mcpServer.impl.internal.http.McpCustomPortServerManager;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.TextArea;
import consulo.ui.TextBox;
import consulo.ui.TableItemEditor;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
public class McpServerConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable, Configurable.NoScroll {
    public static final String ID = "mcp.server";

    private final Provider<McpServerSettings> mySettings;
    private final Provider<McpAuthTokenManager> myTokenManager;
    private final Provider<McpSessionManager> mySessionManager;
    private final Application myApplication;

    @Inject
    public McpServerConfigurable(Provider<McpServerSettings> settings,
                                 Provider<McpAuthTokenManager> tokenManager,
                                 Provider<McpSessionManager> sessionManager,
                                 Application application) {
        mySettings = settings;
        myTokenManager = tokenManager;
        mySessionManager = sessionManager;
        myApplication = application;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        McpServerSettings settings = mySettings.get();

        VerticalLayout root = VerticalLayout.create();

        CheckBox enabledBox = CheckBox.create(McpServerLocalize.checkboxEnableMcpServer());
        propertyBuilder.add(enabledBox, settings::isEnabled, settings::setEnabled);
        root.add(enabledBox);

        IntBox portBox = IntBox.create(McpServerSettings.DEFAULT_PORT);
        propertyBuilder.add(portBox, settings::getPort, settings::setPort);
        root.add(DockLayout.create().left(Label.create(McpServerLocalize.labelPort())).right(portBox));

        CheckBox approvalBox = CheckBox.create(McpServerLocalize.checkboxRequireApproval());
        propertyBuilder.add(approvalBox, settings::isRequireApproval, settings::setRequireApproval);
        Button forgetButton = Button.create(McpServerLocalize.buttonForgetApprovals(), event -> settings.forgetApprovals());
        root.add(DockLayout.create().left(approvalBox).right(forgetButton));

        root.add(buildClientConfiguration(propertyBuilder, settings));

        return root;
    }

    @RequiredUIAccess
    private Component buildClientConfiguration(PropertyBuilder propertyBuilder, McpServerSettings settings) {
        McpAuthTokenManager tokenManager = myTokenManager.get();
        McpSessionManager sessionManager = mySessionManager.get();
        UIAccess uiAccess = UIAccess.current();

        String serverUrl = "http://127.0.0.1:" + settings.getPort() + "/mcp";
        TextBox urlBox = TextBox.create(serverUrl);
        urlBox.setEditable(false);
        Button copyUrlButton = Button.create(McpServerLocalize.buttonCopy(), event -> uiAccess.getClipboard().setText(serverUrl));

        VerticalLayout layout = VerticalLayout.create();
        layout.add(DockLayout.create()
            .left(Label.create(McpServerLocalize.labelServerUrl()))
            .center(urlBox)
            .right(copyUrlButton));
        layout.add(Label.create(McpServerLocalize.labelSignInHint(serverUrl)));

        CheckBox staticTokenBox = CheckBox.create(McpServerLocalize.checkboxStaticToken());
        propertyBuilder.add(staticTokenBox, settings::isStaticTokenEnabled, settings::setStaticTokenEnabled);
        layout.add(staticTokenBox);

        TextArea configurationArea = TextArea.create();
        configurationArea.setEditable(false);
        Button copyButton = Button.create(McpServerLocalize.buttonCopyClientConfiguration(),
            event -> uiAccess.getClipboard().setText(StringUtil.notNullize(configurationArea.getValue())));
        Button regenerateButton = Button.create(McpServerLocalize.buttonRegenerateToken(), event -> {
            configurationArea.setValue(clientConfiguration(serverUrl, tokenManager.regenerate()));
            sessionManager.terminateFor(McpPrincipal.STATIC_TOKEN.id());
        });
        tokenManager.getToken().thenAcceptAsync(token -> configurationArea.setValue(clientConfiguration(serverUrl, token)), uiAccess);

        VerticalLayout staticTokenLayout = VerticalLayout.create();
        staticTokenLayout.add(Label.create(McpServerLocalize.labelClientConfiguration()));
        staticTokenLayout.add(configurationArea);
        staticTokenLayout.add(HorizontalLayout.create().add(copyButton).add(regenerateButton));
        staticTokenLayout.setVisible(settings.isStaticTokenEnabled());
        staticTokenBox.addValueListener(event -> staticTokenLayout.setVisible(Boolean.TRUE.equals(event.getValue())));
        layout.add(staticTokenLayout);
        return layout;
    }



    private static String clientConfiguration(String serverUrl, String token) {
        return "{\n"
            + "  \"mcpServers\": {\n"
            + "    \"consulo\": {\n"
            + "      \"type\": \"http\",\n"
            + "      \"url\": \"" + serverUrl + "\",\n"
            + "      \"headers\": {\n"
            + "        \"" + McpAuthTokenManager.AUTHORIZATION_HEADER + "\": \"" + McpAuthTokenManager.toAuthorizationValue(token) + "\"\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";
    }


    /**
     * Enabling, disabling or moving the port only takes effect once the listener is rebound.
     */
    @Override
    protected void afterApply() {
        McpServerSettings settings = mySettings.get();
        if (!settings.isEnabled()) {
            mySessionManager.get().clear();
        }
        else if (!settings.isStaticTokenEnabled()) {
            mySessionManager.get().terminateFor(McpPrincipal.STATIC_TOKEN.id());
        }
        // the port manager is an extension, not a service, so it has to come from its extension point
        myApplication.getExtensionPoint(CustomPortServerManager.class)
            .findExtensionOrFail(McpCustomPortServerManager.class)
            .portChanged();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.AI_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return McpServerLocalize.configurableDisplayName();
    }
}
