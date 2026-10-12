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
package consulo.externalService.impl.internal.startup;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.internal.PluginIconHolder;
import consulo.externalService.impl.internal.plugin.ui.PluginTagUtil;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeStep;
import consulo.externalService.internal.startup.StartupCustomizeUI;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.CheckBoxTree;
import consulo.ui.Component;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Every plugin of the repository, grouped by its tags. The check state belongs to the plugin, so a plugin listed under
 * several tags shows the same state everywhere; checking a plugin checks what it needs, unchecking it unchecks what needs
 * it.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class PluginStartupCustomizeStep implements StartupCustomizeStep {
    private final StartupCustomizePlugins myPlugins;
    private final Map<String, List<PluginDescriptor>> myByTag = new TreeMap<>();
    private final List<PluginDescriptor> myUntagged = new ArrayList<>();

    private @Nullable CheckBoxTree<Object> myTree;
    private @Nullable Component myComponent;
    private boolean mySpreading;

    public PluginStartupCustomizeStep(StartupCustomizeContext context) {
        myPlugins = new StartupCustomizePlugins(context.getRepositoryPlugins());

        for (PluginDescriptor descriptor : context.getRepositoryPlugins()) {
            Set<String> tags = descriptor.getTags();
            if (tags.isEmpty()) {
                myUntagged.add(descriptor);
            }
            for (String tag : tags) {
                myByTag.computeIfAbsent(tag, it -> new ArrayList<>()).add(descriptor);
            }
        }
    }

    @Override
    public LocalizeValue getTitle() {
        return ExternalServiceLocalize.startupCustomizePluginsTitle();
    }

    @Override
    public boolean isVisible(StartupCustomizeContext context) {
        return !context.getRepositoryPlugins().isEmpty();
    }

    @RequiredUIAccess
    @Override
    public Component getComponent(StartupCustomizeContext context, Disposable uiDisposable) {
        Component component = myComponent;
        if (component != null) {
            return component;
        }

        TreeModel<Object> model = (nodeFactory, parentValue) -> {
            for (Object child : children(parentValue)) {
                TreeNode<Object> node = nodeFactory.apply(child);
                node.setLeaf(child instanceof PluginDescriptor);
                node.setRenderer((value, presentation) -> {
                    if (value instanceof PluginDescriptor descriptor) {
                        presentation.withIcon(PluginIconHolder.getSmall(descriptor));
                        presentation.append(descriptor.getName());
                    }
                    else if (value instanceof LocalizeValue text) {
                        presentation.withIcon(PlatformIconGroup.nodesFolder());
                        presentation.append(text);
                    }
                    else if (value instanceof String tag) {
                        presentation.withIcon(PlatformIconGroup.nodesFolder());
                        presentation.append(PluginTagUtil.getTagLocalizeValue(tag));
                    }
                });
            }
        };

        CheckBoxTree<Object> tree = CheckBoxTree.create(model);
        Disposer.register(uiDisposable, tree.destroyHook());
        tree.setCheckableFilter(value -> value instanceof PluginDescriptor);
        tree.setCheckEnabledFilter(value -> !(value instanceof PluginDescriptor descriptor)
            || !StartupCustomizePlugins.isInstalled(descriptor.getPluginId()));
        tree.addCheckListener(event -> {
            if (!mySpreading && event.getValue() instanceof PluginDescriptor descriptor) {
                spread(descriptor.getPluginId(), event.isChecked());
            }
        });
        myTree = tree;

        syncFrom(context.getPluginsToInstall());
        tree.expandAll();

        component = StartupCustomizeUI.page(ExternalServiceLocalize.startupCustomizePluginsHeader(), ScrollableLayout.create(tree));
        myComponent = component;
        return component;
    }

    private List<Object> children(@Nullable Object parentValue) {
        List<Object> result = new ArrayList<>();
        if (parentValue == null) {
            myByTag.keySet()
                .stream()
                .sorted(Comparator.comparing(tag -> PluginTagUtil.getTagLocalizeValue(tag).get(), String.CASE_INSENSITIVE_ORDER))
                .forEach(result::add);
            if (!myUntagged.isEmpty()) {
                result.add(ExternalServiceLocalize.startupCustomizePluginsGroupOther());
            }
        }
        else if (parentValue instanceof String tag) {
            result.addAll(sorted(myByTag.getOrDefault(tag, List.of())));
        }
        else if (parentValue instanceof LocalizeValue) {
            result.addAll(sorted(myUntagged));
        }
        return result;
    }

    private static List<PluginDescriptor> sorted(Collection<PluginDescriptor> plugins) {
        List<PluginDescriptor> result = new ArrayList<>(plugins);
        result.sort((first, second) -> String.CASE_INSENSITIVE_ORDER.compare(first.getName(), second.getName()));
        return result;
    }

    @RequiredUIAccess
    private void spread(PluginId id, boolean checked) {
        CheckBoxTree<Object> tree = myTree;
        if (tree == null) {
            return;
        }

        Set<PluginId> affected = checked ? myPlugins.withDependencies(id) : myPlugins.dependents(id);

        mySpreading = true;
        try {
            for (PluginId other : affected) {
                PluginDescriptor descriptor = myPlugins.find(other);
                if (descriptor != null && !StartupCustomizePlugins.isInstalled(other)) {
                    tree.setChecked(descriptor, checked);
                }
            }
        }
        finally {
            mySpreading = false;
        }
    }

    @RequiredUIAccess
    private void syncFrom(Set<PluginId> toInstall) {
        CheckBoxTree<Object> tree = myTree;
        if (tree == null) {
            return;
        }

        mySpreading = true;
        try {
            for (List<PluginDescriptor> plugins : myByTag.values()) {
                for (PluginDescriptor descriptor : plugins) {
                    PluginId id = descriptor.getPluginId();
                    tree.setChecked(descriptor, StartupCustomizePlugins.isInstalled(id) || toInstall.contains(id));
                }
            }
            for (PluginDescriptor descriptor : myUntagged) {
                PluginId id = descriptor.getPluginId();
                tree.setChecked(descriptor, StartupCustomizePlugins.isInstalled(id) || toInstall.contains(id));
            }
        }
        finally {
            mySpreading = false;
        }
    }

    @Override
    public void onStepEnter(StartupCustomizeContext context) {
        if (myTree != null) {
            syncFrom(context.getPluginsToInstall());
        }
    }

    @Override
    public void onStepLeave(StartupCustomizeContext context) {
        CheckBoxTree<Object> tree = myTree;
        if (tree == null) {
            return;
        }

        Set<PluginId> checked = new LinkedHashSet<>();
        for (Object value : tree.getCheckedValues()) {
            if (value instanceof PluginDescriptor descriptor && !StartupCustomizePlugins.isInstalled(descriptor.getPluginId())) {
                checked.add(descriptor.getPluginId());
            }
        }

        Set<PluginId> toInstall = context.getPluginsToInstall();
        toInstall.clear();
        toInstall.addAll(checked);
    }
}
