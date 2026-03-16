/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.internal.CompilerWorkspaceConfiguration;
import consulo.compiler.localize.CompilerLocalize;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SimpleConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.function.Supplier;

@ExtensionImpl
public class CompilerConfigurable extends SimpleConfigurable<CompilerConfigurable.Root> implements ProjectConfigurable {
    protected static class Root implements Supplier<Component> {
        private CheckBox myCbClearOutputDirectory;
        private CheckBox myCbAutoShowFirstError;

        private VerticalLayout myLayout;

        @RequiredUIAccess
        public Root() {
            myLayout = VerticalLayout.create();

            myCbClearOutputDirectory = CheckBox.create(CompilerLocalize.labelOptionClearOutputDirectoryOnRebuild());
            myLayout.add(myCbClearOutputDirectory);

            myCbAutoShowFirstError = CheckBox.create(CompilerLocalize.labelOptionAutoshowFirstError());
            myLayout.add(myCbAutoShowFirstError);
        }

        
        @Override
        public Component get() {
            return myLayout;
        }
    }

    private final Provider<CompilerWorkspaceConfiguration> myCompilerWorkspaceConfiguration;

    @Inject
    public CompilerConfigurable(Provider<CompilerWorkspaceConfiguration> compilerWorkspaceConfiguration) {
        myCompilerWorkspaceConfiguration = compilerWorkspaceConfiguration;
    }

    @RequiredUIAccess
    
    @Override
    protected Root createPanel(Disposable uiDisposable) {
        return new Root();
    }

    @RequiredUIAccess
    @Override
    protected boolean isModified(Root component) {
        CompilerWorkspaceConfigurationImpl compilerWorkspaceConfiguration = (CompilerWorkspaceConfigurationImpl) myCompilerWorkspaceConfiguration.get();

        boolean isModified = component.myCbClearOutputDirectory.getValue() != compilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY;
        isModified |= component.myCbAutoShowFirstError.getValue() != compilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR;
        return isModified;
    }

    @RequiredUIAccess
    @Override
    protected void reset(Root component) {
        CompilerWorkspaceConfigurationImpl compilerWorkspaceConfiguration = (CompilerWorkspaceConfigurationImpl) myCompilerWorkspaceConfiguration.get();

        component.myCbAutoShowFirstError.setValue(compilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR);
        component.myCbClearOutputDirectory.setValue(compilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY);
    }

    @RequiredUIAccess
    @Override
    protected void apply(Root component) throws ConfigurationException {
        CompilerWorkspaceConfigurationImpl compilerWorkspaceConfiguration = (CompilerWorkspaceConfigurationImpl) myCompilerWorkspaceConfiguration.get();

        compilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR = component.myCbAutoShowFirstError.getValue();
        compilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY = component.myCbClearOutputDirectory.getValue();
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return CompilerLocalize.compilerConfigurableDisplayName();
    }

    
    @Override
    public String getId() {
        return StandardConfigurableIds.COMPILER_GROUP;
    }
}
