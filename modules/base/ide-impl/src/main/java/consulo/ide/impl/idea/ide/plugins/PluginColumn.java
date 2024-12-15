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

import consulo.application.util.DateFormatUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.Comparator;

/**
* @author UNV
* @since 2024-11-14
*/
public enum PluginColumn {
    NAME(
        IdeLocalize.columnPluginsName(),
        (o1, o2) -> StringUtil.compare(o1.getName(), o2.getName(), true)
    ) {
        @Override
        public String valueOf(PluginDescriptor base) {
            return base.getName();
        }
    },
    DOWNLOADS(
        IdeLocalize.columnPluginsDownloads(),
        (o1, o2) -> {
            String count1 = o1.getDownloads();
            String count2 = o2.getDownloads();
            if (count1 != null && count2 != null) {
                return Long.valueOf(count1).compareTo(Long.valueOf(count2));
            }
            else if (count1 != null) {
                return -1;
            }
            else {
                return 1;
            }
        }
    ) {
        @Override
        public Class getValueClass() {
            return Integer.class;
        }

        @Override
        public String valueOf(PluginDescriptor base) {
            //  Base class IdeaPluginDescriptor does not declare this field.
            return base.getDownloads();
        }
    },
    RATING(
        IdeLocalize.columnPluginsRate(),
        (o1, o2) -> {
            String rating1 = ((PluginNode)o1).getRating();
            String rating2 = ((PluginNode)o2).getRating();
            return Comparing.compare(rating1, rating2);
        }
    ) {
        @Override
        public String valueOf(PluginDescriptor base) {
            return ((PluginNode)base).getRating();
        }
    },
    DATE(
        IdeLocalize.columnPluginsDate(),
        (o1, o2) -> {
            long date1 = o1 instanceof PluginNode pluginNode1 ? pluginNode1.getDate() : 0;
            long date2 = o2 instanceof PluginNode pluginNode2 ? pluginNode2.getDate() : 0;
            if (date1 < date2) {
                return -1;
            }
            else if (date1 > date2) {
                return 1;
            }
            return 0;
        }
    ) {
        @Override
        public String valueOf(PluginDescriptor base) {
            //  Base class IdeaPluginDescriptor does not declare this field.
            long date = base instanceof PluginNode pluginNode ? pluginNode.getDate() : 0;
            if (date != 0) {
                return DateFormatUtil.formatDate(date);
            }
            else {
                return IdeLocalize.pluginInfoNotAvailable().get();
            }
        }
    },
    CATEGORY(
        IdeLocalize.columnPluginsCategory(),
        (o1, o2) -> 0
    ) {
        @Override
        public String valueOf(PluginDescriptor base) {
            // For COLUMN_STATUS - set of icons show the actual state of installed plugins.
            return "";
        }
    };

    @Nonnull
    private final LocalizeValue myTitle;
    @Nonnull
    private final Comparator<PluginDescriptor> myComparator;

    PluginColumn(@Nonnull LocalizeValue title, @Nonnull Comparator<PluginDescriptor> comparator) {
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

    public Class getValueClass() {
        return String.class;
    }

    public abstract String valueOf(PluginDescriptor base);
}
