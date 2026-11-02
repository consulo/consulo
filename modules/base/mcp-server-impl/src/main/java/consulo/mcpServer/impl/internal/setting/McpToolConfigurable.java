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
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.mcp.tool.McpToolDescriptor;
import consulo.mcpServer.McpToolGroup;
import consulo.mcpServer.impl.internal.McpToolRegistration;
import consulo.mcpServer.impl.internal.McpToolRegistry;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.CheckBoxTree;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.Component;
import consulo.ui.TextAttribute;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@ExtensionImpl
public class McpToolConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable, Configurable.NoScroll {
    private static final int MAX_DESCRIPTION_LENGTH = 110;

    private final Provider<McpServerSettings> mySettings;
    private final Provider<McpToolRegistry> myToolRegistry;

    @Inject
    public McpToolConfigurable(Provider<McpServerSettings> settings, Provider<McpToolRegistry> toolRegistry) {
        mySettings = settings;
        myToolRegistry = toolRegistry;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        McpServerSettings settings = mySettings.get();
        Map<McpToolGroup, List<McpToolDescriptor>> groups = collectGroups();
        List<McpToolDescriptor> descriptors = new ArrayList<>();
        groups.values().forEach(descriptors::addAll);

        CheckBoxTree<Object> tree = CheckBoxTree.create(createModel(groups));
        Disposer.register(uiDisposable, tree.destroyHook());
        tree.setCheckPolicy(CheckBoxTreePolicy.FULL_SYNC);

        propertyBuilder.add(() -> {
            Set<String> disabled = new HashSet<>();
            for (McpToolDescriptor descriptor : descriptors) {
                if (!tree.isChecked(descriptor)) {
                    disabled.add(descriptor.getName());
                }
            }
            return disabled;
        }, disabled -> {
            for (Map.Entry<McpToolGroup, List<McpToolDescriptor>> group : groups.entrySet()) {
                tree.setChecked(group.getKey(), group.getValue().stream().noneMatch(descriptor -> disabled.contains(descriptor.getName())));
                for (McpToolDescriptor descriptor : group.getValue()) {
                    tree.setChecked(descriptor, !disabled.contains(descriptor.getName()));
                }
            }
        }, settings::copyDisabledTools, settings::replaceDisabledTools);

        tree.expandAll();
        return ScrollableLayout.create(tree);
    }

    private Map<McpToolGroup, List<McpToolDescriptor>> collectGroups() {
        Map<String, McpToolGroup> groupsById = new LinkedHashMap<>();
        Map<String, List<McpToolDescriptor>> toolsById = new LinkedHashMap<>();
        for (McpToolRegistration registration : myToolRegistry.get().getTools()) {
            McpToolGroup group = registration.getGroup();
            groupsById.putIfAbsent(group.id(), group);
            toolsById.computeIfAbsent(group.id(), id -> new ArrayList<>()).add(registration.getDescriptor());
        }

        List<McpToolGroup> ordered = new ArrayList<>(groupsById.values());
        ordered.sort(Comparator.<McpToolGroup>comparingInt(McpToolConfigurable::standardIndex).thenComparing(group -> group.name().get()));

        Map<McpToolGroup, List<McpToolDescriptor>> result = new LinkedHashMap<>();
        for (McpToolGroup group : ordered) {
            List<McpToolDescriptor> tools = new ArrayList<>(toolsById.getOrDefault(group.id(), List.of()));
            tools.sort(Comparator.comparing(McpToolDescriptor::getName));
            result.put(group, tools);
        }
        return result;
    }

    private static int standardIndex(McpToolGroup group) {
        for (int i = 0; i < McpToolGroup.STANDARD.size(); i++) {
            if (McpToolGroup.STANDARD.get(i).id().equals(group.id())) {
                return i;
            }
        }
        return McpToolGroup.STANDARD.size();
    }

    private static TreeModel<Object> createModel(Map<McpToolGroup, List<McpToolDescriptor>> groups) {
        return (nodeFactory, parentValue) -> {
            if (parentValue == null) {
                for (McpToolGroup group : groups.keySet()) {
                    TreeNode<Object> node = nodeFactory.apply(group);
                    node.setLeaf(false);
                    node.setRenderer((value, presentation) -> presentation.append(group.name()));
                }
            }
            else if (parentValue instanceof McpToolGroup group) {
                for (McpToolDescriptor descriptor : groups.getOrDefault(group, List.of())) {
                    TreeNode<Object> node = nodeFactory.apply(descriptor);
                    node.setLeaf(true);
                    node.setRenderer((value, presentation) -> {
                        presentation.withIcon(needsApproval(descriptor) ? PlatformIconGroup.generalWarning() : Image.empty(Image.DEFAULT_ICON_SIZE));
                        presentation.append(descriptor.getName());
                        presentation.append("   " + StringUtil.first(descriptor.getDescription(), MAX_DESCRIPTION_LENGTH, true), TextAttribute.GRAYED);
                    });
                }
            }
        };
    }

    private static boolean needsApproval(McpToolDescriptor descriptor) {
        return !Boolean.TRUE.equals(descriptor.getReadOnlyHint())
            && (Boolean.TRUE.equals(descriptor.getDestructiveHint()) || Boolean.TRUE.equals(descriptor.getOpenWorldHint()));
    }

    @Override
    public String getId() {
        return "mcp.server.tools";
    }

    @Override
    public String getParentId() {
        return McpServerConfigurable.ID;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return McpServerLocalize.configurableToolsDisplayName();
    }
}
