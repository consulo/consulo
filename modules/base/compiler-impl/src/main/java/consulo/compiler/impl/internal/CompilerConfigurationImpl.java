/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.compiler.CompilerConfiguration;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2013-06-10
 */
@Singleton
@ServiceImpl
public class CompilerConfigurationImpl extends CompilerConfiguration {
    private static final String DEFAULT_OUTPUT_URL = "out";
    private static final String URL = "url";

    private final Project myProject;
    private final ModuleManager myModuleManager;
    private @Nullable String myOutputDirUrl;

    @Inject
    public CompilerConfigurationImpl(Project project, ModuleManager moduleManager) {
        myProject = project;
        myModuleManager = moduleManager;
    }

    @Override
    public String getCompilerOutputUrl() {
        if (myOutputDirUrl == null) {
            return VirtualFileManager.constructUrl(
                URLUtil.FILE_PROTOCOL,
                FileUtil.toSystemIndependentName(myProject.getBasePath()) + "/" + DEFAULT_OUTPUT_URL
            );
        }
        return myOutputDirUrl;
    }

    @Override
    public void setCompilerOutputUrl(@Nullable String compilerOutputUrl) {
        myOutputDirUrl = compilerOutputUrl;
    }

    @RequiredReadAction
    public void getState(Element stateElement) {
        if (myOutputDirUrl != null) {
            stateElement.setAttribute(URL, myOutputDirUrl);
        }

        for (Module module : myModuleManager.getModules()) {
            ModuleCompilerPathsManagerImpl moduleCompilerPathsManager =
                (ModuleCompilerPathsManagerImpl) ModuleCompilerPathsManager.getInstance(module);
            Element state = moduleCompilerPathsManager.getState();
            if (state != null) {
                stateElement.addContent(state);
            }
        }
    }

    public void loadState(Element element) {
        String url = element.getAttributeValue(URL);
        if (url != null) {
            setCompilerOutputUrl(url);
        }

        for (Element moduleElement : element.getChildren("module")) {
            String name = moduleElement.getAttributeValue("name");
            if (name == null) {
                continue;
            }
            Module module = ReadAction.compute(() -> myModuleManager.findModuleByName(name));
            if (module != null) {
                ModuleCompilerPathsManagerImpl moduleCompilerPathsManager =
                    (ModuleCompilerPathsManagerImpl) ModuleCompilerPathsManager.getInstance(module);
                moduleCompilerPathsManager.loadState(moduleElement);
            }
        }
    }
}
