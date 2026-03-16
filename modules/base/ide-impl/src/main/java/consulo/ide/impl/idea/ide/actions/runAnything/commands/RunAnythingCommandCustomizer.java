// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.commands;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;

/**
 * This class customizes 'Run Anything' command line and its data context.
 * E.g. it's possible to wrap command into a shell or/and patch environment variables.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RunAnythingCommandCustomizer {
    /**
     * Customizes command line to be executed
     *
     * @param workDirectory the working directory the command will be executed in
     * @param dataContext   {@link DataContext} to fetch module, project etc.
     * @param commandLine   command line to be customized
     * @return patched command line
     */
    
    @SuppressWarnings("unused")
    protected GeneralCommandLine customizeCommandLine(
        VirtualFile workDirectory,
        DataContext dataContext,
        GeneralCommandLine commandLine
    ) {
        return commandLine;
    }

    /**
     * Customizes data context command line to be executed on
     *
     * @param dataContext original {@link DataContext}
     * @return customized {@link DataContext}
     */
    
    protected DataContext customizeDataContext(DataContext dataContext) {
        return dataContext;
    }

    
    public static GeneralCommandLine customizeCommandLine(
        DataContext dataContext,
        VirtualFile workDirectory,
        GeneralCommandLine commandLine
    ) {
        SimpleReference<GeneralCommandLine> commandLineRef = SimpleReference.create(commandLine);
        Application.get().getExtensionPoint(RunAnythingCommandCustomizer.class).forEach(
            customizer -> commandLineRef.set(customizer.customizeCommandLine(workDirectory, dataContext, commandLineRef.get()))
        );
        return commandLineRef.get();
    }

    
    public static DataContext customizeContext(DataContext dataContext) {
        SimpleReference<DataContext> dataContextRef = SimpleReference.create(dataContext);
        Application.get().getExtensionPoint(RunAnythingCommandCustomizer.class)
            .forEach(customizer -> dataContextRef.set(customizer.customizeDataContext(dataContextRef.get())));
        return dataContextRef.get();
    }
}