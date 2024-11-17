/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.plugins;

import consulo.application.impl.internal.plugin.PluginsLoader;
import consulo.container.impl.PluginDescriptorImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.container.plugin.PluginId;
import consulo.ide.impl.localize.PluginLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.io.zip.ZipUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * User: anna
 */
public class InstalledPluginsManagerMain extends PluginManagerMain {
    public InstalledPluginsManagerMain() {
        super();
        init();

        final StatusText emptyText = myPluginTable.getEmptyText();
        emptyText.setText(PluginLocalize.messageNothingToShowClickButton().get());
    }

    @Override
    protected void addCustomFilters(Consumer<JComponent> adder) {
        LabelPopup showPopupLabel = new LabelPopup(PluginLocalize.enabledFilterLabel(), this::createShowGroupPopup);

        adder.accept(showPopupLabel);

        updateShowPopupText(showPopupLabel);
    }

    private void updateShowPopupText(LabelPopup labelPopup) {
        labelPopup.setPrefixedText(((InstalledPluginsTableModel)myPluginsModel).getEnabledFilter().getTitle());
    }

    private DefaultActionGroup createShowGroupPopup(LabelPopup labelPopup) {
        final DefaultActionGroup gr = new DefaultActionGroup();
        for (EnabledFilter enabledValue : EnabledFilter.values()) {
            gr.addAction(new DumbAwareAction(enabledValue.getTitle()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    final PluginDescriptor selection = myPluginTable.getSelectedObject();
                    final String filter = myFilter.getFilter().toLowerCase(Locale.ROOT);
                    ((InstalledPluginsTableModel)myPluginsModel).setEnabledFilter(enabledValue, filter);
                    if (selection != null) {
                        select(selection.getPluginId());
                    }

                    updateShowPopupText(labelPopup);
                }
            });
        }
        return gr;
    }

    @Nullable
    public static PluginDescriptor loadDescriptorFromArchive(final File file) throws IOException {
        PluginDescriptor descriptor = null;
        final File outputDir = consulo.ide.impl.idea.openapi.util.io.FileUtil.createTempDirectory("plugin", "");
        try {
            ZipUtil.extract(file, outputDir, null);
            final File[] files = outputDir.listFiles();
            if (files != null && files.length == 1) {
                descriptor = PluginsLoader.loadPluginDescriptor(files[0]);
            }
        }
        finally {
            FileUtil.delete(outputDir);
        }
        return descriptor;
    }

    @RequiredUIAccess
    public void checkInstalledPluginDependencies(PluginDescriptor pluginDescriptor) {
        final Set<PluginId> notInstalled = new HashSet<>();
        final Set<PluginId> disabledIds = new HashSet<>();
        final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
        final PluginId[] optionalDependentPluginIds = pluginDescriptor.getOptionalDependentPluginIds();
        for (PluginId id : dependentPluginIds) {
            if (ArrayUtil.find(optionalDependentPluginIds, id) > -1) {
                continue;
            }
            final boolean disabled = ((InstalledPluginsTableModel)myPluginsModel).isDisabled(id);
            final boolean enabled = ((InstalledPluginsTableModel)myPluginsModel).isEnabled(id);
            if (!enabled && !disabled) {
                notInstalled.add(id);
            }
            else if (disabled) {
                disabledIds.add(id);
            }
        }
        if (!notInstalled.isEmpty()) {
            Messages.showWarningDialog(
                PluginLocalize.messagePluginDependsOnUnknownPlugins(
                    pluginDescriptor.getName(),
                    StringUtil.join(notInstalled, PluginId::toString, ", "),
                    notInstalled.size()
                ).get(),
                CommonLocalize.titleWarning().get()
            );
        }
        if (!disabledIds.isEmpty()) {
            final Set<PluginDescriptor> dependencies = new HashSet<>();
            for (PluginDescriptor ideaPluginDescriptor : myPluginsModel.getAllPlugins()) {
                if (disabledIds.contains(ideaPluginDescriptor.getPluginId())) {
                    dependencies.add(ideaPluginDescriptor);
                }
            }

            if (Messages.showOkCancelDialog(
                getMainPanel(),
                PluginLocalize.messagePluginDependsOnDisabledPlugins(
                    pluginDescriptor.getName(),
                    StringUtil.join(dependencies, PluginDescriptor::getName, ", "),
                    dependencies.size()
                ).get(),
                CommonLocalize.titleWarning().get(),
                UIUtil.getWarningIcon()
            ) == Messages.OK) {
                ((InstalledPluginsTableModel)myPluginsModel).enableRows(
                    dependencies.toArray(new PluginDescriptor[dependencies.size()]),
                    Boolean.TRUE
                );
            }
        }
    }

    @Override
    protected void propagateUpdates(List<PluginDescriptor> list) {
    }

    @Nonnull
    @Override
    protected PluginTable createTable() {
        myPluginsModel = new InstalledPluginsTableModel();

        PluginTable table = new PluginTable(myPluginsModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.registerKeyboardAction(e -> {
            final int column = InstalledPluginsTableModel.getCheckboxColumn();
            final int[] selectedRows = table.getSelectedRows();
            boolean currentlyMarked = true;
            for (final int selectedRow : selectedRows) {
                if (selectedRow < 0 || !table.isCellEditable(selectedRow, column)) {
                    return;
                }
                final Boolean enabled = (Boolean)table.getValueAt(selectedRow, column);
                currentlyMarked &= enabled == null || enabled;
            }
            final PluginDescriptor[] selected = new PluginDescriptor[selectedRows.length];
            for (int i = 0, selectedLength = selected.length; i < selectedLength; i++) {
                selected[i] = myPluginsModel.getObjectAt(table.convertRowIndexToModel(selectedRows[i]));
            }
            ((InstalledPluginsTableModel)myPluginsModel)
                .enableRows(selected, currentlyMarked ? Boolean.FALSE : Boolean.TRUE);
            table.repaint();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);
        table.setExpandableItemsEnabled(false);
        return table;
    }

    @Override
    public ActionGroup getActionGroup() {
        final DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.addAction(new RefreshAction());
        actionGroup.addAction(new InstallPluginFromDiskAction(this));
        return actionGroup;
    }

    @Override
    public boolean isModified() {
        final boolean modified = super.isModified();
        if (modified) {
            return true;
        }
        for (int i = 0; i < myPluginsModel.getRowCount(); i++) {
            final PluginDescriptor pluginDescriptor = myPluginsModel.getObjectAt(i);
            if (pluginDescriptor.isEnabled() != ((InstalledPluginsTableModel)myPluginsModel).isEnabled(pluginDescriptor.getPluginId())) {
                return true;
            }
        }
        for (PluginDescriptor descriptor : myPluginsModel.filtered) {
            if (descriptor.isEnabled() != ((InstalledPluginsTableModel)myPluginsModel).isEnabled(descriptor.getPluginId())) {
                return true;
            }
        }
        final Set<PluginId> disabledPlugins = consulo.container.plugin.PluginManager.getDisabledPlugins();
        for (Map.Entry<PluginId, Boolean> entry : ((InstalledPluginsTableModel)myPluginsModel).getEnabledMap().entrySet()) {
            final Boolean enabled = entry.getValue();
            if (enabled != null && !enabled && !disabledPlugins.contains(entry.getKey())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String apply() {
        final String apply = super.apply();
        if (apply != null) {
            return apply;
        }
        for (int i = 0; i < myPluginTable.getRowCount(); i++) {
            final PluginDescriptor pluginDescriptor = myPluginsModel.getObjectAt(i);
            final Boolean enabled = (Boolean)myPluginsModel.getValueAt(i, InstalledPluginsTableModel.getCheckboxColumn());
            setEnabled(pluginDescriptor, enabled != null && enabled);
        }

        for (PluginDescriptor descriptor : myPluginsModel.filtered) {
            setEnabled(descriptor, ((InstalledPluginsTableModel)myPluginsModel).isEnabled(descriptor.getPluginId()));
        }

        final Set<PluginId> ids = new LinkedHashSet<>();
        for (Map.Entry<PluginId, Boolean> entry : ((InstalledPluginsTableModel)myPluginsModel).getEnabledMap().entrySet()) {
            final Boolean value = entry.getValue();
            if (value != null && !value) {
                ids.add(entry.getKey());
            }
        }

        consulo.container.plugin.PluginManager.replaceDisabledPlugins(ids);

        return null;
    }

    private void setEnabled(PluginDescriptor descriptor, boolean enabled) {
        if (descriptor instanceof PluginNode pluginNode) {
            pluginNode.setStatus(enabled ? PluginDescriptorStatus.OK : PluginDescriptorStatus.DISABLED_BY_USER);
        }
        else if (descriptor instanceof PluginDescriptorImpl pluginDescriptor) {
            pluginDescriptor.setStatus(enabled ? PluginDescriptorStatus.OK : PluginDescriptorStatus.DISABLED_BY_USER);
        }
        else {
            throw new IllegalArgumentException("Unknown plugin class " + descriptor.getPluginId());
        }
    }

    @Override
    protected String canApply() {
        final Map<PluginId, Set<PluginId>> dependentToRequiredListMap =
            new HashMap<>(((InstalledPluginsTableModel)myPluginsModel).getDependentToRequiredListMap());
        for (Iterator<PluginId> iterator = dependentToRequiredListMap.keySet().iterator(); iterator.hasNext(); ) {
            iterator.next(); // ignore
            iterator.remove();
        }
        if (!dependentToRequiredListMap.isEmpty()) {
            return "<html><body style=\"padding: 5px;\">" +
                PluginLocalize.messagePluginWontLoad(
                    dependentToRequiredListMap.size(),
                    StringUtil.join(
                        dependentToRequiredListMap.keySet(),
                        pluginId -> {
                            PluginDescriptor ideaPluginDescriptor = PluginManager.getPlugin(pluginId);
                            return "\"" + (ideaPluginDescriptor != null ? ideaPluginDescriptor.getName() : pluginId.getIdString()) + "\"";
                        },
                        ", "
                    )
                ) +
                "</body></html>";
        }
        return super.canApply();
    }
}
