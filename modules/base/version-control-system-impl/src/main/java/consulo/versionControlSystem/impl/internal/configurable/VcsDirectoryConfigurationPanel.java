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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.SelectionMode;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.font.Font;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.io.FileUtil;
import consulo.util.io.UriUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.impl.internal.NewMappings;
import consulo.versionControlSystem.internal.DefaultVcsRootPolicy;
import consulo.versionControlSystem.internal.VcsRootErrorsFinder;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.*;

import static consulo.versionControlSystem.VcsConfiguration.getInstance;

/**
 * @author yole
 */
public class VcsDirectoryConfigurationPanel {
    private static final TextAttribute SEPARATOR_ATTRIBUTES = TextAttribute.REGULAR_BOLD;
    private static final TextAttribute INVALID_ATTRIBUTES = TextAttribute.ERROR;
    private static final TextAttribute UNREGISTERED_ATTRIBUTES = new TextAttribute(Font.BOLD, StandardColors.GRAY);

    private final Project myProject;
    private final LocalizeValue myProjectMessage;
    private final ProjectLevelVcsManager myVcsManager;
    private final List<ModuleVcsListener> myListeners = Lists.newLockFreeCopyOnWriteList();

    private final Map<String, VcsDescriptor> myAllVcss;
    private final boolean myIsDisabled;
    private final VcsConfiguration myVcsConfiguration;
    private final Map<String, VcsRootChecker> myCheckers;
    private final VcsUpdateInfoScopeFilterConfigurable myScopeFilterConfig;

    private final MutableFlatDataModel<MapInfo> myModel = FlatDataModel.of(List.of());

    private static class MapInfo {
        static final MapInfo SEPARATOR = new MapInfo(new VcsDirectoryMapping("SEPARATOR", "SEP"), Type.SEPARATOR);
        static final Comparator<MapInfo> COMPARATOR = (o1, o2) -> {
            if (o1.type.isRegistered() && o2.type.isRegistered() || o1.type == Type.UNREGISTERED && o2.type == Type.UNREGISTERED) {
                return NewMappings.MAPPINGS_COMPARATOR.compare(o1.mapping, o2.mapping);
            }
            return o1.type.ordinal() - o2.type.ordinal();
        };

        static MapInfo unregistered(VcsDirectoryMapping mapping) {
            return new MapInfo(mapping, Type.UNREGISTERED);
        }

        static MapInfo unregistered(String path, String vcs) {
            return new MapInfo(new VcsDirectoryMapping(path, vcs), Type.UNREGISTERED);
        }

        static MapInfo registered(VcsDirectoryMapping mapping, boolean valid) {
            return new MapInfo(mapping, valid ? Type.NORMAL : Type.INVALID);
        }

        enum Type {
            NORMAL,
            INVALID,
            SEPARATOR,
            UNREGISTERED;

            boolean isRegistered() {
                return this == NORMAL || this == INVALID;
            }
        }

        private final Type type;
        private final VcsDirectoryMapping mapping;

        private MapInfo(VcsDirectoryMapping mapping, Type type) {
            this.mapping = mapping;
            this.type = type;
        }

        boolean isSetApart() {
            return this == SEPARATOR || type == Type.UNREGISTERED;
        }
    }

    public VcsDirectoryConfigurationPanel(Project project) {
        myProject = project;
        myVcsConfiguration = getInstance(myProject);
        myProjectMessage = LocalizeValue.of(XmlStringUtil.wrapInHtml(
            XmlStringUtil.escapeText(VcsDirectoryMapping.PROJECT_CONSTANT) + " - " +
                DefaultVcsRootPolicy.getInstance(myProject).getProjectConfigurationMessage(myProject).replace('\n', ' ')
        ));
        myIsDisabled = myProject.isDefault();
        myVcsManager = ProjectLevelVcsManager.getInstance(project);

        myAllVcss = new HashMap<>();
        for (VcsDescriptor vcsDescriptor : myVcsManager.getAllVcss()) {
            myAllVcss.put(vcsDescriptor.getId(), vcsDescriptor);
        }

        myScopeFilterConfig = new VcsUpdateInfoScopeFilterConfigurable(myProject, myVcsConfiguration);

        myCheckers = new HashMap<>();
        updateRootCheckers();
    }

    @RequiredUIAccess
    public Component createComponent(Disposable uiDisposable) {
        Table<MapInfo> table = Table.create(myModel);
        table.setSelectionMode(SelectionMode.MULTIPLE);
        table.setRowBackgroundGetter(VcsDirectoryConfigurationPanel::rowBackground);
        table.setEnabled(!myIsDisabled);

        table.addColumn(VcsLocalize.columnInfoConfigureVcsesDirectory(), info -> info)
            .setRender((presentation, value) -> renderDirectory(presentation, value.getValue()));

        table.addColumn(VcsLocalize.columnNameConfigureVcsesVcs(), info -> info.mapping.getVcs())
            .setRender((presentation, value, info) -> renderVcs(presentation, info))
            .setEditor(new VcsColumnEditor());

        Component decorated = ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new MappingAddAction())
            .addOrReplaceAction(new MappingEditAction())
            .addOrReplaceAction(new MappingRemoveAction())
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .build();

        DockLayout bottom = DockLayout.create();
        bottom.top(HtmlLabel.create(myProjectMessage));
        bottom.bottom(myScopeFilterConfig.createComponent(uiDisposable));

        DockLayout root = DockLayout.create();
        root.center(decorated);
        root.bottom(bottom);
        return root;
    }

    private static @Nullable ColorValue rowBackground(MapInfo info) {
        return info.isSetApart() ? ComponentColors.LAYOUT : null;
    }

    private static TextAttribute attributes(MapInfo info) {
        if (info == MapInfo.SEPARATOR) {
            return SEPARATOR_ATTRIBUTES;
        }
        if (info.type == MapInfo.Type.INVALID) {
            return INVALID_ATTRIBUTES;
        }
        if (info.type == MapInfo.Type.UNREGISTERED) {
            return UNREGISTERED_ATTRIBUTES;
        }
        return TextAttribute.REGULAR;
    }

    @RequiredUIAccess
    private void renderDirectory(TextItemPresentation presentation, @Nullable MapInfo info) {
        if (info == null) {
            return;
        }

        if (info == MapInfo.SEPARATOR) {
            presentation.append(VcsLocalize.settingsUnregisteredRoots(), attributes(info));
            return;
        }

        if (info.mapping.isDefaultMapping()) {
            presentation.append(VcsDirectoryMapping.PROJECT_CONSTANT, attributes(info));
            return;
        }

        String directory = info.mapping.getDirectory();
        VirtualFile baseDir = myProject.getBaseDir();
        if (baseDir == null) {
            return;
        }

        File directoryFile = new File(StringUtil.trimEnd(UriUtil.trimTrailingSlashes(directory), "\\") + "/");
        File ioBase = new File(baseDir.getPath());
        if (directoryFile.isAbsolute() && !FileUtil.isAncestor(ioBase, directoryFile, false)) {
            presentation.append(new File(directory).getPath(), attributes(info));
            return;
        }

        String relativePath = FileUtil.getRelativePath(ioBase, directoryFile);
        if (".".equals(relativePath) || relativePath == null) {
            presentation.append(ioBase.getPath(), attributes(info));
        }
        else {
            presentation.append(relativePath, attributes(info));
            presentation.append(" (" + ioBase + ")", TextAttribute.GRAYED);
        }
    }

    @RequiredUIAccess
    private void renderVcs(TextItemPresentation presentation, MapInfo info) {
        if (info == MapInfo.SEPARATOR) {
            return;
        }

        presentation.append(vcsPresentation(info.mapping.getVcs()), attributes(info));
    }

    private LocalizeValue vcsPresentation(String vcsName) {
        if (vcsName.isEmpty()) {
            return VcsLocalize.noneVcsPresentation();
        }

        VcsDescriptor vcs = myAllVcss.get(vcsName);
        return vcs != null ? vcs.getDisplayName() : VcsLocalize.unknownVcsPresentation(vcsName);
    }

    private class VcsColumnEditor implements TableItemEditor<MapInfo, String> {
        @RequiredUIAccess
        @Override
        public ValueComponent<String> createComponent(MapInfo item) {
            ComboBox.Builder<String> builder = ComboBox.builder();
            builder.add("", VcsLocalize.noneVcsPresentation());
            for (VcsDescriptor descriptor : myVcsManager.getAllVcss()) {
                builder.add(descriptor.getId(), descriptor.getDisplayName());
            }
            return builder.build();
        }

        @RequiredUIAccess
        @Override
        public void commit(MapInfo item, @Nullable String value) {
            Collection<AbstractVcs> activeVcses = getActiveVcses();
            item.mapping.setVcs(value == null ? "" : value);
            checkNotifyListeners(activeVcses);
        }

        @Override
        public boolean isEditable(MapInfo item) {
            return !myIsDisabled && item != MapInfo.SEPARATOR && item.type != MapInfo.Type.UNREGISTERED;
        }
    }

    private class MappingAddAction extends AddAction<MapInfo> {
        @RequiredUIAccess
        @Override
        protected void doAdd(AnActionEvent e) {
            if (myIsDisabled || !rootsOfOneKindInSelection(getSelectedValues(e))) {
                return;
            }

            List<MapInfo> unregistered = ContainerUtil.filter(getSelectedValues(e), info -> info.type == MapInfo.Type.UNREGISTERED);
            if (unregistered.isEmpty()) {
                addMapping();
            }
            else {
                addSelectedUnregisteredMappings(unregistered);
            }
            updateRootCheckers();
        }

    }

    private class MappingEditAction extends EditAction<MapInfo> {
        @RequiredUIAccess
        @Override
        protected void doEdit(MapInfo value, AnActionEvent e) {
            if (myIsDisabled || !onlyRegisteredRootsInSelection(getSelectedValues(e))) {
                return;
            }

            editMapping(value);
            updateRootCheckers();
        }

    }

    private class MappingRemoveAction extends RemoveAction<MapInfo> {
        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            if (myIsDisabled || !onlyRegisteredRootsInSelection(getSelectedValues(e))) {
                return;
            }

            removeMappings(getSelectedValues(e));
            updateRootCheckers();
        }

    }

    private static boolean rootsOfOneKindInSelection(List<MapInfo> selection) {
        if (selection.isEmpty()) {
            return true;
        }
        if (selection.size() == 1 && selection.get(0) == MapInfo.SEPARATOR) {
            return false;
        }
        int registered = ContainerUtil.filter(selection, info -> info.type.isRegistered()).size();
        return registered == selection.size() || registered == 0;
    }

    private static boolean onlyRegisteredRootsInSelection(List<MapInfo> selection) {
        return !selection.isEmpty() && ContainerUtil.filter(selection, info -> info.type.isRegistered()).size() == selection.size();
    }

    private void updateRootCheckers() {
        myCheckers.clear();
        for (VcsRootChecker checker : VcsRootChecker.EXTENSION_POINT_NAME.getExtensionList()) {
            VcsKey key = checker.getSupportedVcs();
            if (myVcsManager.findVcsByName(key.getName()) == null) {
                continue;
            }
            myCheckers.put(key.getName(), checker);
        }
    }

    @RequiredUIAccess
    private void initializeModel() {
        List<MapInfo> mappings = new ArrayList<>();
        for (VcsDirectoryMapping mapping : myVcsManager.getDirectoryMappings()) {
            mappings.add(MapInfo.registered(
                new VcsDirectoryMapping(mapping.getDirectory(), mapping.getVcs(), mapping.getRootSettings()),
                isMappingValid(mapping)
            ));
        }

        Collection<VcsRootError> errors = findUnregisteredRoots();
        if (!errors.isEmpty()) {
            mappings.add(MapInfo.SEPARATOR);
            for (VcsRootError error : errors) {
                mappings.add(MapInfo.unregistered(error.getMapping()));
            }
        }

        myModel.replaceAll(mappings);
        myScopeFilterConfig.reset();
    }

    private Collection<VcsRootError> findUnregisteredRoots() {
        return ContainerUtil.filter(
            VcsRootErrorsFinder.getInstance(myProject).find(),
            error -> error.getType() == VcsRootError.Type.UNREGISTERED_ROOT
        );
    }

    private boolean isMappingValid(VcsDirectoryMapping mapping) {
        String vcs = mapping.getVcs();
        VcsRootChecker checker = myCheckers.get(vcs);
        return checker == null
            || (mapping.isDefaultMapping() ? checker.isRoot(myProject.getBasePath()) : checker.isRoot(mapping.getDirectory()));
    }

    private List<MapInfo> currentItems() {
        List<MapInfo> items = new ArrayList<>();
        for (MapInfo info : myModel) {
            items.add(info);
        }
        return items;
    }

    @RequiredUIAccess
    private void addMapping() {
        VcsMappingConfigurationDialog dialog =
            new VcsMappingConfigurationDialog(myProject, VcsLocalize.directoryMappingAddTitle().get());
        // due to wonderful UI designer bug
        dialog.initProjectMessage();
        if (dialog.showAndGet()) {
            addMapping(dialog.getMapping());
        }
    }

    @RequiredUIAccess
    private void addMapping(VcsDirectoryMapping mapping) {
        List<MapInfo> items = currentItems();
        items.add(MapInfo.registered(
            new VcsDirectoryMapping(mapping.getDirectory(), mapping.getVcs(), mapping.getRootSettings()),
            isMappingValid(mapping)
        ));
        items.sort(MapInfo.COMPARATOR);
        myModel.replaceAll(items);
        checkNotifyListeners(getActiveVcses());
    }

    @RequiredUIAccess
    private void addSelectedUnregisteredMappings(List<MapInfo> infos) {
        List<MapInfo> items = currentItems();
        for (MapInfo info : infos) {
            items.remove(info);
            items.add(MapInfo.registered(info.mapping, isMappingValid(info.mapping)));
        }
        sortAndAddSeparatorIfNeeded(items);
        myModel.replaceAll(items);
        checkNotifyListeners(getActiveVcses());
    }

    private static void sortAndAddSeparatorIfNeeded(List<MapInfo> items) {
        boolean hasUnregistered = false;
        boolean hasSeparator = false;
        for (MapInfo item : items) {
            if (item.type == MapInfo.Type.UNREGISTERED) {
                hasUnregistered = true;
            }
            else if (item.type == MapInfo.Type.SEPARATOR) {
                hasSeparator = true;
            }
        }
        if (!hasUnregistered && hasSeparator) {
            items.remove(MapInfo.SEPARATOR);
        }
        else if (hasUnregistered && !hasSeparator) {
            items.add(MapInfo.SEPARATOR);
        }
        items.sort(MapInfo.COMPARATOR);
    }

    @RequiredUIAccess
    private void editMapping(MapInfo info) {
        VcsMappingConfigurationDialog dialog =
            new VcsMappingConfigurationDialog(myProject, VcsLocalize.directoryMappingRemoveTitle().get());
        dialog.setMapping(info.mapping);
        if (!dialog.showAndGet()) {
            return;
        }

        List<MapInfo> items = currentItems();
        int index = items.indexOf(info);
        if (index < 0) {
            return;
        }

        VcsDirectoryMapping mapping = dialog.getMapping();
        items.set(index, MapInfo.registered(mapping, isMappingValid(mapping)));
        items.sort(MapInfo.COMPARATOR);
        myModel.replaceAll(items);
        checkNotifyListeners(getActiveVcses());
    }

    @RequiredUIAccess
    private void removeMappings(List<MapInfo> selection) {
        if (selection.isEmpty()) {
            return;
        }

        Collection<AbstractVcs> activeVcses = getActiveVcses();
        List<MapInfo> items = currentItems();
        items.removeAll(selection);

        // a root the platform can still see is not gone, only unregistered - it comes back below the separator
        Collection<MapInfo> removedValidRoots = ContainerUtil.mapNotNull(
            selection,
            info -> info.type == MapInfo.Type.NORMAL && myCheckers.get(info.mapping.getVcs()) != null
                ? MapInfo.unregistered(info.mapping.getDirectory(), info.mapping.getVcs())
                : null
        );
        items.addAll(removedValidRoots);
        sortAndAddSeparatorIfNeeded(items);

        myModel.replaceAll(items);
        checkNotifyListeners(activeVcses);
    }

    @RequiredUIAccess
    public void reset() {
        initializeModel();
    }

    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        myVcsManager.setDirectoryMappings(getModelMappings());
        myScopeFilterConfig.apply();
        initializeModel();
    }

    @RequiredUIAccess
    public boolean isModified() {
        if (myScopeFilterConfig.isModified()) {
            return true;
        }
        return !getModelMappings().equals(myVcsManager.getDirectoryMappings());
    }

    private List<VcsDirectoryMapping> getModelMappings() {
        return ContainerUtil.mapNotNull(
            currentItems(),
            info -> info == MapInfo.SEPARATOR || info.type == MapInfo.Type.UNREGISTERED ? null : info.mapping
        );
    }

    public void addVcsListener(ModuleVcsListener moduleVcsListener) {
        myListeners.add(moduleVcsListener);
    }

    public void removeVcsListener(ModuleVcsListener moduleVcsListener) {
        myListeners.remove(moduleVcsListener);
    }

    private void checkNotifyListeners(Collection<AbstractVcs> oldVcses) {
        Collection<AbstractVcs> vcses = getActiveVcses();
        if (!vcses.equals(oldVcses)) {
            for (ModuleVcsListener listener : myListeners) {
                listener.activeVcsSetChanged(vcses);
            }
        }
    }

    public Collection<AbstractVcs> getActiveVcses() {
        Set<AbstractVcs> vcses = new HashSet<>();
        for (VcsDirectoryMapping mapping : getModelMappings()) {
            if (!mapping.getVcs().isEmpty()) {
                vcses.add(myVcsManager.findVcsByName(mapping.getVcs()));
            }
        }
        return vcses;
    }

    @RequiredUIAccess
    public void disposeUIResources() {
        myScopeFilterConfig.disposeUIResources();
    }
}
