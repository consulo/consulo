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

import consulo.ide.impl.idea.ide.ui.search.SearchableOptionsRegistrar;
import consulo.container.plugin.PluginId;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.SortableColumnModel;
import consulo.container.plugin.PluginDescriptor;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author stathik
 * @author Konstantin Bulenkov
 */
public abstract class PluginTableModel extends AbstractTableModel implements SortableColumnModel {
    protected ColumnInfo[] columns;
    protected List<PluginDescriptor> view;
    private RowSorter.SortKey myDefaultSortKey;
    protected final List<PluginDescriptor> filtered = new ArrayList<>();
    private SortBy mySortBy = SortBy.NAME;

    protected PluginTableModel() {
    }

    public PluginTableModel(ColumnInfo... columns) {
        this.columns = columns;
    }

    public void setSortKey(final RowSorter.SortKey sortKey) {
        myDefaultSortKey = sortKey;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public ColumnInfo[] getColumnInfos() {
        return columns;
    }

    @Override
    public boolean isSortable() {
        return true;
    }

    @Override
    public void setSortable(boolean aBoolean) {
        // do nothing cause it's always sortable
    }

    @Override
    public String getColumnName(int column) {
        return columns[column].getName();
    }

    public PluginDescriptor getObjectAt(int row) {
        return view.get(row);
    }

    @Override
    public Object getRowValue(int row) {
        return getObjectAt(row);
    }

    @Override
    public RowSorter.SortKey getDefaultSortKey() {
        return myDefaultSortKey;
    }

    @Override
    public int getRowCount() {
        return view.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columns[columnIndex].valueOf(getObjectAt(rowIndex));
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columns[columnIndex].isCellEditable(getObjectAt(rowIndex));
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        columns[columnIndex].setValue(getObjectAt(rowIndex), aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public List<PluginDescriptor> dependent(PluginDescriptor plugin) {
        List<PluginDescriptor> list = new ArrayList<>();
        for (PluginDescriptor any : getAllPlugins()) {
            if (any.isLoaded()) {
                PluginId[] dep = any.getDependentPluginIds();
                for (PluginId id : dep) {
                    if (id == plugin.getPluginId()) {
                        list.add(any);
                        break;
                    }
                }
            }
        }
        return list;
    }

    public abstract void updatePluginsList(List<PluginDescriptor> list);

    public void filter(List<PluginDescriptor> filtered) {
        fireTableDataChanged();
    }

    protected void filter(String filter) {
        final SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
        final Set<String> search = optionsRegistrar.getProcessedWords(filter);

        final ArrayList<PluginDescriptor> desc = new ArrayList<>();

        final List<PluginDescriptor> toProcess = toProcess();
        for (PluginDescriptor descriptor : filtered) {
            if (!toProcess.contains(descriptor)) {
                toProcess.add(descriptor);
            }
        }
        filtered.clear();
        for (PluginDescriptor descriptor : toProcess) {
            if (isPluginDescriptorAccepted(descriptor) &&
                PluginManagerMain.isAccepted(filter, search, descriptor)) {
                desc.add(descriptor);
            }
            else {
                filtered.add(descriptor);
            }
        }
        filter(desc);
    }

    protected ArrayList<PluginDescriptor> toProcess() {
        return new ArrayList<>(view);
    }

    public abstract int getNameColumn();

    public abstract boolean isPluginDescriptorAccepted(PluginDescriptor descriptor);

    public void sort() {
        Collections.sort(view, columns[getNameColumn()].getComparator());
        fireTableDataChanged();
    }

    public SortBy getSortBy() {
        return mySortBy;
    }

    public void setSortBy(@Nonnull SortBy sortBy) {
        mySortBy = sortBy;
    }

    public List<PluginDescriptor> getAllPlugins() {
        final ArrayList<PluginDescriptor> list = new ArrayList<>();
        list.addAll(view);
        list.addAll(filtered);
        return list;
    }
}
