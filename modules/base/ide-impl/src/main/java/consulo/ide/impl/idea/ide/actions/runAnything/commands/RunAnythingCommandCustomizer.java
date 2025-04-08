// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.commands;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

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
    @Nonnull
    @SuppressWarnings("unused")
    protected GeneralCommandLine customizeCommandLine(
        @Nonnull VirtualFile workDirectory,
        @Nonnull DataContext dataContext,
        @Nonnull GeneralCommandLine commandLine
    ) {
        return commandLine;
    }

    /**
     * Customizes data context command line to be executed on
     *
     * @param dataContext original {@link DataContext}
     * @return customized {@link DataContext}
     */
    @Nonnull
    protected DataContext customizeDataContext(@Nonnull DataContext dataContext) {
        return dataContext;
    }

    @Nonnull
    public static GeneralCommandLine customizeCommandLine(
        @Nonnull DataContext dataContext,
        @Nonnull VirtualFile workDirectory,
        @Nonnull GeneralCommandLine commandLine
    ) {
        SimpleReference<GeneralCommandLine> commandLineRef = SimpleReference.create(commandLine);
        Application.get().getExtensionPoint(RunAnythingCommandCustomizer.class).forEachExtensionSafe(
            customizer -> commandLineRef.set(customizer.customizeCommandLine(workDirectory, dataContext, commandLineRef.get()))
        );
        return commandLineRef.get();
    }

    @Nonnull
    public static DataContext customizeContext(@Nonnull DataContext dataContext) {
        SimpleReference<DataContext> dataContextRef = SimpleReference.create(dataContext);
        Application.get().getExtensionPoint(RunAnythingCommandCustomizer.class)
            .forEachExtensionSafe(customizer -> dataContextRef.set(customizer.customizeDataContext(dataContextRef.get())));
        return dataContextRef.get();
    }
}