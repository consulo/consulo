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
package consulo.ide.impl.idea.packageDependencies;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.packageDependencies.ui.PatternDialectProvider;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
@State(name = "DependencyUISettings", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class DependencyUISettings implements PersistentStateComponent<DependencyUISettings> {
    public boolean UI_FLATTEN_PACKAGES = true;
    public boolean UI_SHOW_FILES = true;
    public boolean UI_SHOW_MODULES = true;
    public boolean UI_SHOW_MODULE_GROUPS = true;
    public boolean UI_FILTER_LEGALS = false;
    public boolean UI_FILTER_OUT_OF_CYCLE_PACKAGES = true;
    public boolean UI_GROUP_BY_SCOPE_TYPE = true;
    public boolean UI_COMPACT_EMPTY_MIDDLE_PACKAGES = true;
    public String SCOPE_TYPE;

    public static DependencyUISettings getInstance() {
        return ServiceManager.getService(DependencyUISettings.class);
    }

    @Override
    public DependencyUISettings getState() {
        return this;
    }

    @Override
    public void loadState(DependencyUISettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getScopeType() {
        if (SCOPE_TYPE == null) {
            // return last if not selected
            List<PatternDialectProvider> list = PatternDialectProvider.EP_NAME.getExtensionList();
            return list.get(list.size() - 1).getId();
        }
        return SCOPE_TYPE;
    }

    public void setScopeType(String scopeType) {
        SCOPE_TYPE = scopeType;
    }
}