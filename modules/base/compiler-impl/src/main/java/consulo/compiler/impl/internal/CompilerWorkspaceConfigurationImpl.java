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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.compiler.internal.CompilerWorkspaceConfiguration;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

/**
 * @author Eugene Zhuravlev
 */
@Singleton
@ServiceImpl
@State(name = "CompilerWorkspaceConfiguration", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class CompilerWorkspaceConfigurationImpl implements CompilerWorkspaceConfiguration, PersistentStateComponent<CompilerWorkspaceConfigurationImpl> {
    public boolean AUTO_SHOW_ERRORS_IN_EDITOR = true;
    public boolean CLEAR_OUTPUT_DIRECTORY = true;

    @Override
    public boolean isAutoShowErrorsInEditor() {
        return AUTO_SHOW_ERRORS_IN_EDITOR;
    }

    @Override
    public boolean isClearOutputDirectory() {
        return CLEAR_OUTPUT_DIRECTORY;
    }

    @Override
    public CompilerWorkspaceConfigurationImpl getState() {
        return this;
    }

    @Override
    public void loadState(CompilerWorkspaceConfigurationImpl state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
