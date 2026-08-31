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
import consulo.mcpServer.impl.internal.McpToolRegistration;
import consulo.mcpServer.impl.internal.McpToolRegistry;
import consulo.mcpServer.impl.internal.http.McpCustomPortServerManager;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.ui.CheckBox;
import consulo.ui.ComponentItemRender;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
public class McpServerConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable, Configurable.NoScroll {
    private final Provider<McpServerSettings> mySettings;
    private final Provider<McpToolRegistry> myToolRegistry;
    private final Application myApplication;

    @Inject
    public McpServerConfigurable(Provider<McpServerSettings> settings,
                                 Provider<McpToolRegistry> toolRegistry,
                                 Application application) {
        mySettings = settings;
        myToolRegistry = toolRegistry;
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

        root.add(ScrollableLayout.create(buildToolTable(settings)));

        return root;
    }

    @RequiredUIAccess
    private Component buildToolTable(McpServerSettings settings) {
        // the registry hands them out grouped by contributing toolset, which is not a useful order here
        List<McpToolDescriptor> descriptors = new ArrayList<>();
        for (McpToolRegistration registration : myToolRegistry.get().getTools()) {
            descriptors.add(registration.getDescriptor());
        }
        descriptors.sort(Comparator.comparing(McpToolDescriptor::getName));

        Table<McpToolDescriptor> table = Table.create(FlatDataModel.of(descriptors));

        table.addColumn(McpServerLocalize.columnEnabled(), descriptor -> settings.isToolEnabled(descriptor.getName()))
            .setWidth(70)
            .setRender(ComponentItemRender.reusable(
                () -> CheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(Boolean.TRUE.equals(item.getValue()))))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<Boolean> createComponent(McpToolDescriptor descriptor) {
                    return CheckBox.create(LocalizeValue.empty(), settings.isToolEnabled(descriptor.getName()));
                }

                @RequiredUIAccess
                @Override
                public void commit(McpToolDescriptor descriptor, @Nullable Boolean value) {
                    settings.setToolEnabled(descriptor.getName(), Boolean.TRUE.equals(value));
                }
            });

        table.addColumn(McpServerLocalize.columnTool(), McpToolDescriptor::getName)
            .setWidth(220)
            .setSortable(Comparator.naturalOrder());
        table.addColumn(McpServerLocalize.columnDescription(), McpToolDescriptor::getDescription);

        return table;
    }

    /**
     * Enabling, disabling or moving the port only takes effect once the listener is rebound.
     */
    @Override
    protected void afterApply() {
        // the port manager is an extension, not a service, so it has to come from its extension point
        myApplication.getExtensionPoint(CustomPortServerManager.class)
            .findExtensionOrFail(McpCustomPortServerManager.class)
            .portChanged();
    }

    @Override
    public String getId() {
        return "mcp.server";
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
