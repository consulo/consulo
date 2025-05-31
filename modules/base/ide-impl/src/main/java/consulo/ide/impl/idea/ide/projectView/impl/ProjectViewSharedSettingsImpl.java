/*
 * Copyright 2013-2017 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License get distributed on an "AS get" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.ui.view.internal.ProjectViewSharedSettings;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * @author VISTALL
 * <p>
 * from kotlin by @author Konstantin Bulenkov
 */
@Singleton
@ServiceImpl
@State(name = "ProjectViewSharedSettings", storages = @Storage("projectView.xml"))
public class ProjectViewSharedSettingsImpl implements PersistentStateComponent<ProjectViewSharedSettingsImpl>, ProjectViewSharedSettings {
    private boolean myFlattenPackages = false;
    private boolean myShowMembers = false;
    private boolean mySortByType = false;
    private boolean myShowModules = true;
    private boolean myShowLibraryContents = true;
    private boolean myHideEmptyPackages = true;
    private boolean myAbbreviatePackages = false;
    private boolean myAutoscrollFromSource = false;
    private boolean myAutoscrollToSource = false;
    private boolean myFoldersAlwaysOnTop = true;

    private LinkedHashMap<String, Object> myProperties = new LinkedHashMap<>();

    @Override
    @Nonnull
    public ProjectViewSharedSettingsImpl getState() {
        return this;
    }

    @Override
    public void loadState(@Nonnull ProjectViewSharedSettingsImpl state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Override
    public boolean isFlattenPackages() {
        return myFlattenPackages;
    }

    @Override
    public boolean isShowLibraryContents() {
        return myShowLibraryContents;
    }

    @Override
    public <T> void setViewOption(@Nonnull KeyWithDefaultValue<T> key, @Nullable T value) {
        if (value == null || Objects.equals(value, key.getDefaultValue())) {
            myProperties.remove(key.toString());
        } else {
            myProperties.put(key.toString(), value);
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getViewOption(@Nonnull KeyWithDefaultValue<T> option) {
        Object o = myProperties.get(option.toString());
        if (o != null) {
            return (T) o;
        }
        return option.getDefaultValue();
    }

    @Override
    public boolean isHideEmptyMiddlePackages() {
        return myHideEmptyPackages;
    }

    @Override
    public boolean isAbbreviatePackageNames() {
        return myAbbreviatePackages;
    }

    @Override
    public void setFlattenPackages(boolean flattenPackages) {
        myFlattenPackages = flattenPackages;
    }

    @Override
    public boolean isShowMembers() {
        return myShowMembers;
    }

    @Override
    public void setShowMembers(boolean showMembers) {
        myShowMembers = showMembers;
    }

    @Override
    public boolean getSortByType() {
        return mySortByType;
    }

    @Override
    public void setSortByType(boolean sortByType) {
        mySortByType = sortByType;
    }

    @Override
    public boolean getShowModules() {
        return myShowModules;
    }

    @Override
    public void setShowModules(boolean showModules) {
        myShowModules = showModules;
    }

    @Override
    public void setShowLibraryContents(boolean showLibraryContents) {
        myShowLibraryContents = showLibraryContents;
    }

    @Override
    public boolean isHideEmptyPackages() {
        return myHideEmptyPackages;
    }

    @Override
    public void setHideEmptyPackages(boolean hideEmptyPackages) {
        myHideEmptyPackages = hideEmptyPackages;
    }

    @Override
    public boolean isAbbreviatePackages() {
        return myAbbreviatePackages;
    }

    @Override
    public void setAbbreviatePackages(boolean abbreviatePackages) {
        myAbbreviatePackages = abbreviatePackages;
    }

    @Override
    public boolean isAutoscrollFromSource() {
        return myAutoscrollFromSource;
    }

    @Override
    public void setAutoscrollFromSource(boolean autoscrollFromSource) {
        myAutoscrollFromSource = autoscrollFromSource;
    }

    @Override
    public boolean isAutoscrollToSource() {
        return myAutoscrollToSource;
    }

    @Override
    public void setAutoscrollToSource(boolean autoscrollToSource) {
        myAutoscrollToSource = autoscrollToSource;
    }

    @Override
    public boolean isFoldersAlwaysOnTop() {
        return myFoldersAlwaysOnTop;
    }

    @Override
    public void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop) {
        myFoldersAlwaysOnTop = foldersAlwaysOnTop;
    }
}
