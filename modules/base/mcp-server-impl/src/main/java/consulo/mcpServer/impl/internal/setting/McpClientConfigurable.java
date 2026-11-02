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
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.mcpServer.impl.internal.oauth.McpOAuthService;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@ExtensionImpl
public class McpClientConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable, Configurable.NoScroll {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Provider<McpOAuthService> myOAuthService;

    @Inject
    public McpClientConfigurable(Provider<McpOAuthService> oAuthService) {
        myOAuthService = oAuthService;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        McpOAuthService oAuthService = myOAuthService.get();
        MutableFlatDataModel<McpOAuthClientState> model = FlatDataModel.of(oAuthService.getClients());

        Table<McpOAuthClientState> table = Table.create(model);
        table.addColumn(McpServerLocalize.columnClient(), client -> client.CLIENT_NAME).setWidth(260);
        table.addColumn(McpServerLocalize.columnApproved(), client -> formatTime(client.APPROVED_AT)).setWidth(160);
        table.addColumn(McpServerLocalize.columnLastUsed(), client -> formatTime(client.LAST_USED_AT));

        Button revokeButton = Button.create(McpServerLocalize.buttonRevokeClient(), event -> {
            McpOAuthClientState selected = table.getSelectedItem();
            if (selected != null) {
                oAuthService.revoke(selected.CLIENT_ID);
                model.remove(selected);
            }
        });

        VerticalLayout root = VerticalLayout.create();
        root.add(Label.create(McpServerLocalize.labelAuthorizedClients()));
        root.add(HorizontalLayout.create().add(revokeButton));
        root.add(ScrollableLayout.create(table));
        return root;
    }

    private static String formatTime(long millis) {
        return millis <= 0 ? "" : TIME_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    @Override
    public String getId() {
        return "mcp.server.clients";
    }

    @Override
    public String getParentId() {
        return McpServerConfigurable.ID;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return McpServerLocalize.configurableClientsDisplayName();
    }
}
