/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.usage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.inject.Singleton;

import java.io.File;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "UsageViewSettings", storages = @Storage("other.xml"))
public class UsageViewSettings implements PersistentStateComponent<UsageViewSettings> {
    public String EXPORT_FILE_NAME = "report.txt";
    public boolean IS_EXPANDED = false;
    public boolean IS_SHOW_PACKAGES = true;
    public boolean IS_SHOW_METHODS = false;
    public boolean IS_AUTOSCROLL_TO_SOURCE = false;
    public boolean IS_FILTER_DUPLICATED_LINE = false;
    public boolean IS_SHOW_MODULES = false;
    public boolean IS_PREVIEW_USAGES = false;
    public boolean IS_SORT_MEMBERS_ALPHABETICALLY = true;
    public boolean IS_REPLACE_PREVIEW_USAGES = true;
    public float PREVIEW_USAGES_SPLITTER_PROPORTIONS = 0.5f;

    public boolean GROUP_BY_USAGE_TYPE = true;
    public boolean GROUP_BY_MODULE = true;
    public boolean GROUP_BY_PACKAGE = true;
    public boolean GROUP_BY_FILE_STRUCTURE = true;
    public boolean GROUP_BY_SCOPE = false;

    public static UsageViewSettings getInstance() {
        return Application.get().getInstance(UsageViewSettings.class);
    }

    public boolean isReplacePreviewUsages() {
        return IS_REPLACE_PREVIEW_USAGES;
    }

    public void setReplacePreviewUsages(boolean val) {
        IS_REPLACE_PREVIEW_USAGES = val;
    }

    public boolean isPreviewUsages() {
        return IS_PREVIEW_USAGES;
    }

    public void setPreviewUsages(boolean val) {
        IS_PREVIEW_USAGES = val;
    }

    public float getPreviewUsagesSplitterProportion() {
        return PREVIEW_USAGES_SPLITTER_PROPORTIONS;
    }

    public void setPreviewUsagesSplitterProportion(float val) {
        PREVIEW_USAGES_SPLITTER_PROPORTIONS = val;
    }

    public boolean isExpanded() {
        return IS_EXPANDED;
    }

    public void setExpanded(boolean val) {
        IS_EXPANDED = val;
    }

    public boolean isShowPackages() {
        return IS_SHOW_PACKAGES;
    }

    public void setShowPackages(boolean val) {
        IS_SHOW_PACKAGES = val;
    }

    public boolean isShowMethods() {
        return IS_SHOW_METHODS;
    }

    public boolean isShowModules() {
        return IS_SHOW_MODULES;
    }

    public void setShowMethods(boolean val) {
        IS_SHOW_METHODS = val;
    }

    public void setShowModules(boolean val) {
        IS_SHOW_MODULES = val;
    }

    public boolean isFilterDuplicatedLine() {
        return IS_FILTER_DUPLICATED_LINE;
    }

    public void setFilterDuplicatedLine(boolean val) {
        IS_FILTER_DUPLICATED_LINE = val;
    }

    public void setAutoScrollToSource(boolean val) {
        IS_AUTOSCROLL_TO_SOURCE = val;
    }

    public boolean isAutoScrollToSource() {
        return IS_AUTOSCROLL_TO_SOURCE;
    }

    public boolean isSortAlphabetically() {
        return IS_SORT_MEMBERS_ALPHABETICALLY;
    }

    public void setSortAlphabetically(boolean val) {
        IS_SORT_MEMBERS_ALPHABETICALLY = val;
    }

    @Transient
    public String getExportFileName() {
        return EXPORT_FILE_NAME != null ? EXPORT_FILE_NAME.replace('/', File.separatorChar) : null;
    }

    public void setExportFileName(String s) {
        if (s != null) {
            s = s.replace(File.separatorChar, '/');
        }
        EXPORT_FILE_NAME = s;
    }

    @Override
    public UsageViewSettings getState() {
        return this;
    }

    @Override
    public void loadState(UsageViewSettings object) {
        XmlSerializerUtil.copyBean(object, this);
    }
}
