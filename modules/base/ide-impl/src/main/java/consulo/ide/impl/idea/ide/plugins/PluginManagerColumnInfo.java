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

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.impl.plugins.InstalledPluginsState;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.awt.ColumnInfo;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.util.Comparator;

/**
 * @author stathik
 * @since 2003-12-11
 */
public abstract class PluginManagerColumnInfo extends ColumnInfo<PluginDescriptor, String> {
    private static final PluginColumn[] PLUGIN_COLUMNS = PluginColumn.values();
    public static final int COLUMN_NAME = 0;

    private static final float KILOBYTE = 1024.0f;
    private static final float MEGABYTE = 1024 * KILOBYTE;

    @Nonnull
    private final PluginColumn myColumn;
    private final PluginTableModel myModel;

    public PluginManagerColumnInfo(int columnIdx, PluginTableModel model) {
        super(PLUGIN_COLUMNS[columnIdx].getTitle().get());
        myColumn = PluginColumn.values()[columnIdx];
        myModel = model;
    }

    @Override
    public String valueOf(PluginDescriptor base) {
        return myColumn.valueOf(base);
    }

    protected SortBy getSortBy() {
        return myModel.getSortBy();
    }

    public static boolean isDownloaded(@Nonnull PluginDescriptor node) {
        if (node instanceof PluginNode pluginNode && pluginNode.getInstallStatus() == PluginNode.STATUS_DOWNLOADED) {
            return true;
        }
        final PluginId pluginId = node.getPluginId();
        return !PluginManager.isPluginInstalled(pluginId) && InstalledPluginsState.getInstance().getInstalledPlugins().contains(pluginId);
    }

    @Override
    public Comparator<PluginDescriptor> getComparator() {
        final Comparator<PluginDescriptor> comparator = getColumnComparator();
        if (myModel.getSortBy() == SortBy.STATUS) {
            final RowSorter.SortKey defaultSortKey = myModel.getDefaultSortKey();
            final int up = defaultSortKey != null && defaultSortKey.getSortOrder() == SortOrder.ASCENDING ? -1 : 1;
            return (o1, o2) -> {
                if (o1 instanceof PluginNode pn1 && o2 instanceof PluginNode pn2) {
                    final int status1 = pn1.getInstallStatus();
                    final int status2 = pn2.getInstallStatus();
                    if (isDownloaded(pn1)) {
                        if (!isDownloaded(pn2)) {
                            return up;
                        }
                        return comparator.compare(pn1, pn2);
                    }
                    if (isDownloaded(pn2)) {
                        return -up;
                    }

                    if (status1 == PluginNode.STATUS_DELETED) {
                        if (status2 != PluginNode.STATUS_DELETED) {
                            return up;
                        }
                        return comparator.compare(pn1, pn2);
                    }
                    if (status2 == PluginNode.STATUS_DELETED) {
                        return -up;
                    }

                    if (status1 == PluginNode.STATUS_INSTALLED) {
                        if (status2 != PluginNode.STATUS_INSTALLED) {
                            return up;
                        }
                        final boolean hasNewerVersion1 = InstalledPluginsTableModel.hasNewerVersion(o1.getPluginId());
                        final boolean hasNewerVersion2 = InstalledPluginsTableModel.hasNewerVersion(o2.getPluginId());
                        if (hasNewerVersion1 != hasNewerVersion2) {
                            if (hasNewerVersion1) {
                                return up;
                            }
                            return -up;
                        }
                        return comparator.compare(pn1, pn2);
                    }
                    if (status2 == PluginNode.STATUS_INSTALLED) {
                        return -up;
                    }
                }
                return comparator.compare(o1, o2);
            };
        }

        return comparator;
    }

    protected Comparator<PluginDescriptor> getColumnComparator() {
        return myColumn.getComparator();
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public static String getFormattedSize(String size) {
        if (size.equals("-1")) {
            return IdeLocalize.pluginInfoUnknown().get();
        }
        else if (size.length() >= 4) {
            if (size.length() < 7) {
                return String.format("%.1f", (float)Integer.parseInt(size) / KILOBYTE) + " K";
            }
            else {
                return String.format("%.1f", (float)Integer.parseInt(size) / MEGABYTE) + " M";
            }
        }
        return size;
    }

    @Override
    public Class getColumnClass() {
        return myColumn.getValueClass();
    }

    @Override
    public abstract TableCellRenderer getRenderer(PluginDescriptor o);
}
