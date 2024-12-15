/*
 * Copyright 2013-2024 consulo.io
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
import consulo.ide.impl.localize.PluginLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.Comparator;

import static consulo.ide.impl.idea.ide.plugins.PluginManagerColumnInfo.isDownloaded;

/**
* @author UNV
* @since 2024-11-13
*/
public enum SortBy {
    NAME(PluginLocalize.sortByName(), PluginColumn.NAME.getComparator()),
    STATUS(
        PluginLocalize.sortByStatus(),
        (o1, o2) -> {
            if (o1 instanceof PluginNode pn1 && o2 instanceof PluginNode pn2) {
                boolean downloaded1 = isDownloaded(pn1), downloaded2 = isDownloaded(pn2);
                if (downloaded1) {
                    return downloaded2 ? 0 : 1;
                }
                if (downloaded2) {
                    return -1;
                }

                int status1 = pn1.getInstallStatus(), status2 = pn2.getInstallStatus();
                if (status1 == PluginNode.STATUS_DELETED) {
                    return status2 == PluginNode.STATUS_DELETED ? 0 : 1;
                }
                if (status2 == PluginNode.STATUS_DELETED) {
                    return -1;
                }

                if (status1 == PluginNode.STATUS_INSTALLED) {
                    if (status2 != PluginNode.STATUS_INSTALLED) {
                        return 1;
                    }
                    final boolean hasNewerVersion1 = InstalledPluginsTableModel.hasNewerVersion(o1.getPluginId());
                    final boolean hasNewerVersion2 = InstalledPluginsTableModel.hasNewerVersion(o2.getPluginId());
                    if (hasNewerVersion1 != hasNewerVersion2) {
                        return hasNewerVersion1 ? 1: -1;
                    }
                    return 0;
                }
                if (status2 == PluginNode.STATUS_INSTALLED) {
                    return -1;
                }
            }
            return 0;
        }
    ),
    RATING(PluginLocalize.sortByRating(), PluginColumn.RATING.getComparator()),
    DOWNLOADS(PluginLocalize.sortByDownloads(), PluginColumn.DOWNLOADS.getComparator()),
    LAST_UPDATED(PluginLocalize.sortByLastUpdated(), PluginColumn.DATE.getComparator());

    @Nonnull
    private final LocalizeValue myTitle;
    @Nonnull
    private final Comparator<PluginDescriptor> myComparator;

    SortBy(@Nonnull LocalizeValue title, @Nonnull Comparator<PluginDescriptor> comparator) {
        myTitle = title;
        myComparator = comparator;
    }

    @Nonnull
    public LocalizeValue getTitle() {
        return myTitle;
    }

    @Nonnull
    public Comparator<PluginDescriptor> getComparator() {
        return myComparator;
    }
}
