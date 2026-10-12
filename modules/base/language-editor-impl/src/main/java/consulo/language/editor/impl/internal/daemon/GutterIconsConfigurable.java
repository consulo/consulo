/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.daemon;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.internal.PluginIconHolder;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.gutter.GutterIconDescriptor;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.language.editor.gutter.LineMarkerSettings;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.CheckBox;
import consulo.ui.CheckBoxTree;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.Component;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class GutterIconsConfigurable implements SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
    public static final String ID = "editor.gutterIcons";

    public static final LocalizeValue DISPLAY_NAME = LocalizeValue.localizeTODO("Gutter Icons");

    private final Application myApplication;
    private final LineMarkerSettings myLineMarkerSettings;
    private final ProjectManager myProjectManager;
    private final EditorFactory myEditorFactory;

    private @Nullable CheckBox myShowGutterIcons;
    private @Nullable CheckBoxTree<Object> myTree;

    private Map<PluginDescriptor, List<GutterIconDescriptor>> myGroups = Map.of();
    private List<GutterIconDescriptor> myDescriptors = List.of();

    @Inject
    public GutterIconsConfigurable(
        Application application,
        LineMarkerSettings lineMarkerSettings,
        ProjectManager projectManager,
        EditorFactory editorFactory
    ) {
        myApplication = application;
        myLineMarkerSettings = lineMarkerSettings;
        myProjectManager = projectManager;
        myEditorFactory = editorFactory;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createUIComponent(Disposable uiDisposable) {
        collectDescriptors();

        CheckBox showGutterIcons = CheckBox.create(ApplicationLocalize.checkboxShowGutterIcons());
        myShowGutterIcons = showGutterIcons;

        CheckBoxTree<Object> tree = CheckBoxTree.create(createModel());
        Disposer.register(uiDisposable, tree.destroyHook());
        tree.setCheckPolicy(CheckBoxTreePolicy.FULL_SYNC);
        myTree = tree;

        showGutterIcons.addValueListener(event -> tree.setEnabled(showGutterIcons.getValueOrError()));

        reset();
        tree.expandAll();

        DockLayout root = DockLayout.create();
        root.top(showGutterIcons);
        root.center(ScrollableLayout.create(tree));
        return root;
    }

    private TreeModel<Object> createModel() {
        return (nodeFactory, parentValue) -> {
            if (parentValue == null) {
                for (PluginDescriptor plugin : myGroups.keySet()) {
                    TreeNode<Object> node = nodeFactory.apply(plugin);
                    node.setLeaf(false);
                    node.setRenderer((value, presentation) -> {
                        presentation.withIcon(PluginIds.isPlatformPlugin(plugin.getPluginId()) ? PlatformIconGroup.icon16() : PluginIconHolder.getSmall(plugin));
                        presentation.append(plugin.getName());
                    });
                }
            }
            else if (parentValue instanceof PluginDescriptor plugin) {
                for (GutterIconDescriptor descriptor : myGroups.getOrDefault(plugin, List.of())) {
                    TreeNode<Object> node = nodeFactory.apply(descriptor);
                    node.setLeaf(true);
                    node.setRenderer((value, presentation) -> {
                        Image icon = descriptor.getIcon();
                        presentation.withIcon(icon == null ? Image.empty(Image.DEFAULT_ICON_SIZE) : icon);
                        presentation.append(descriptor.getName());
                    });
                }
            }
        };
    }

    @Override
    public @Nullable Runnable enableSearch(String option) {
        String text = option.trim();
        CheckBoxTree<Object> tree = myTree;
        if (tree == null || text.isEmpty()) {
            return null;
        }

        Object match = findItem(text);
        return match == null ? null : () -> select(tree, match);
    }

    private @Nullable Object findItem(String text) {
        for (List<GutterIconDescriptor> descriptors : myGroups.values()) {
            for (GutterIconDescriptor descriptor : descriptors) {
                if (StringUtil.containsIgnoreCase(descriptor.getName().get(), text)) {
                    return descriptor;
                }
            }
        }

        for (PluginDescriptor plugin : myGroups.keySet()) {
            if (StringUtil.containsIgnoreCase(plugin.getName(), text)) {
                return plugin;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private static void select(CheckBoxTree<Object> tree, Object value) {
        TreeNode<Object> root = tree.getRootNode();
        if (root == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        root.findChildDeep(candidate -> candidate == value).whenCompleteAsync((node, error) -> {
            if (node != null) {
                tree.select(node);
            }
        }, uiAccess);
    }

    private void collectDescriptors() {
        Map<PluginDescriptor, List<GutterIconDescriptor>> groups = collectGroups(myApplication);

        List<GutterIconDescriptor> descriptors = new ArrayList<>();
        groups.values().forEach(descriptors::addAll);

        myGroups = groups;
        myDescriptors = List.copyOf(descriptors);
    }

    /**
     * The gutter icons the installed plugins provide, grouped by the plugin and ordered by name.
     */
    public static Map<PluginDescriptor, List<GutterIconDescriptor>> collectGroups(Application application) {
        Map<PluginId, PluginDescriptor> plugins = new HashMap<>();
        Map<PluginId, List<GutterIconDescriptor>> byPlugin = new HashMap<>();
        Set<String> ids = new HashSet<>();

        application.getExtensionPoint(LineMarkerProvider.class).forEach(provider -> {
            if (!(provider instanceof LineMarkerProviderDescriptor descriptor) || descriptor.getName().isEmpty()) {
                return;
            }

            PluginDescriptor plugin = PluginManager.getPlugin(provider.getClass());
            if (plugin == null) {
                return;
            }

            plugins.putIfAbsent(plugin.getPluginId(), plugin);
            List<GutterIconDescriptor> icons = byPlugin.computeIfAbsent(plugin.getPluginId(), id -> new ArrayList<>());

            GutterIconDescriptor[] options = descriptor.getOptions();
            for (GutterIconDescriptor icon : options.length > 0 ? options : new GutterIconDescriptor[]{descriptor}) {
                if (ids.add(icon.getId())) {
                    icons.add(icon);
                }
            }
        });

        Comparator<GutterIconDescriptor> byName = (o1, o2) -> StringUtil.naturalCompare(o1.getName().get(), o2.getName().get());

        List<PluginDescriptor> sortedPlugins = new ArrayList<>(plugins.values());
        sortedPlugins.sort((o1, o2) -> StringUtil.naturalCompare(o1.getName(), o2.getName()));

        Map<PluginDescriptor, List<GutterIconDescriptor>> groups = new LinkedHashMap<>();
        for (PluginDescriptor plugin : sortedPlugins) {
            List<GutterIconDescriptor> icons = byPlugin.get(plugin.getPluginId());
            if (icons == null || icons.isEmpty()) {
                continue;
            }

            icons.sort(byName);
            groups.put(plugin, List.copyOf(icons));
        }
        return groups;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        CheckBoxTree<Object> tree = myTree;
        CheckBox showGutterIcons = myShowGutterIcons;
        if (tree == null || showGutterIcons == null) {
            return false;
        }

        for (GutterIconDescriptor descriptor : myDescriptors) {
            if (tree.isChecked(descriptor) != myLineMarkerSettings.isEnabled(descriptor)) {
                return true;
            }
        }
        return showGutterIcons.getValueOrError() != EditorSettingsExternalizable.getInstance().areGutterIconsShown();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        CheckBoxTree<Object> tree = myTree;
        CheckBox showGutterIcons = myShowGutterIcons;
        if (tree == null || showGutterIcons == null) {
            return;
        }

        EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
        if (showGutterIcons.getValueOrError() != editorSettings.areGutterIconsShown()) {
            editorSettings.setGutterIconsShown(showGutterIcons.getValueOrError());
            myEditorFactory.refreshAllEditors();
        }

        for (GutterIconDescriptor descriptor : myDescriptors) {
            myLineMarkerSettings.setEnabled(descriptor, tree.isChecked(descriptor));
        }

        for (Project project : myProjectManager.getOpenProjects()) {
            DaemonCodeAnalyzer.getInstance(project).restart();
        }
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        CheckBoxTree<Object> tree = myTree;
        CheckBox showGutterIcons = myShowGutterIcons;
        if (tree == null || showGutterIcons == null) {
            return;
        }

        for (Map.Entry<PluginDescriptor, List<GutterIconDescriptor>> group : myGroups.entrySet()) {
            tree.setChecked(group.getKey(), group.getValue().stream().allMatch(myLineMarkerSettings::isEnabled));

            for (GutterIconDescriptor descriptor : group.getValue()) {
                tree.setChecked(descriptor, myLineMarkerSettings.isEnabled(descriptor));
            }
        }

        boolean gutterIconsShown = EditorSettingsExternalizable.getInstance().areGutterIconsShown();
        showGutterIcons.setValue(gutterIconsShown);
        tree.setEnabled(gutterIconsShown);
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myTree = null;
        myShowGutterIcons = null;
    }

    @Override
    public String getId() {
        return ID;
    }

    @TestOnly
    public List<GutterIconDescriptor> getDescriptors() {
        return myDescriptors;
    }
}
