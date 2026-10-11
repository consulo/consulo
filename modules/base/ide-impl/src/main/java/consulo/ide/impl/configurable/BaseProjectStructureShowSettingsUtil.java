/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.configurable;

import consulo.configurable.ConfigurableSession;
import consulo.configurable.internal.ConfigurableSessionHolder;
import consulo.content.bundle.SdkTable;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.DefaultSdksModel;
import consulo.ide.impl.roots.ui.configuration.impl.DefaultLibrariesConfigurator;
import consulo.ide.setting.bundle.SettingsSdksModel;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.project.Project;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public abstract class BaseProjectStructureShowSettingsUtil extends BaseShowSettingsUtil {
    private final Provider<SdkTable> mySdkTableProvider;
    private final DefaultSdksModel mySdksModel;

    private @Nullable ModulesConfiguratorImpl myModulesConfigurator;
    private @Nullable LibrariesConfigurator myLibrariesConfigurator;

    protected BaseProjectStructureShowSettingsUtil(Provider<SdkTable> sdkTableProvider) {
        mySdkTableProvider = sdkTableProvider;
        mySdksModel = new DefaultSdksModel(sdkTableProvider);
    }

    @Override
    public SettingsSdksModel getSdksModel() {
        ConfigurableSession session = findSession(null);
        DefaultSdksModel model = session == null
            ? mySdksModel
            : session.get(DefaultSdksModel.class, project -> new DefaultSdksModel(mySdkTableProvider));
        model.initializeIfNeed();
        return model;
    }

    protected void clearCaches() {
        ModulesConfiguratorImpl modulesConfigurator = myModulesConfigurator;
        if (modulesConfigurator != null) {
            modulesConfigurator.disposeWithTree();
            myModulesConfigurator = null;
        }

        LibrariesConfigurator librariesConfigurator = myLibrariesConfigurator;
        if (librariesConfigurator != null) {
            librariesConfigurator.disposeWithTree();
            myLibrariesConfigurator = null;
        }
    }

    @Override
    public @Nullable ModulesConfigurator getModulesModel(Project project) {
        ConfigurableSession session = findSession(project);
        if (session != null) {
            LibrariesConfigurator librariesConfigurator = getLibrariesModel(project);
            return session.get(ModulesConfigurator.class, it -> createModulesModel(it, () -> librariesConfigurator));
        }

        ModulesConfiguratorImpl modulesConfigurator = myModulesConfigurator;
        if (modulesConfigurator == null) {
            modulesConfigurator = createModulesModel(project, () -> getLibrariesModel(project));
            myModulesConfigurator = modulesConfigurator;
        }
        return modulesConfigurator;
    }

    @Override
    public @Nullable LibrariesConfigurator getLibrariesModel(Project project) {
        ConfigurableSession session = findSession(project);
        if (session != null) {
            return session.get(LibrariesConfigurator.class, BaseProjectStructureShowSettingsUtil::createLibrariesModel);
        }

        LibrariesConfigurator librariesConfigurator = myLibrariesConfigurator;
        if (librariesConfigurator == null) {
            librariesConfigurator = createLibrariesModel(project);
            myLibrariesConfigurator = librariesConfigurator;
        }
        return librariesConfigurator;
    }

    private static ModulesConfiguratorImpl createModulesModel(Project project, Supplier<LibrariesConfigurator> librariesConfigurator) {
        ModulesConfiguratorImpl modulesConfigurator = new ModulesConfiguratorImpl(project, librariesConfigurator);
        modulesConfigurator.reset();
        return modulesConfigurator;
    }

    private static LibrariesConfigurator createLibrariesModel(Project project) {
        DefaultLibrariesConfigurator librariesConfigurator = new DefaultLibrariesConfigurator(project);
        librariesConfigurator.reset();
        return librariesConfigurator;
    }

    private static @Nullable ConfigurableSession findSession(@Nullable Project project) {
        ConfigurableSession session = ConfigurableSessionHolder.getNullable();
        if (session == null || project != null && session.getProject() != project) {
            return null;
        }
        return session;
    }
}
