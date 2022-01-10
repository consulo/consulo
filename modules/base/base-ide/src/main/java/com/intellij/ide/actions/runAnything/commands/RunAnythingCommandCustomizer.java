// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.commands;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;

/**
 * This class customizes 'Run Anything' command line and its data context.
 * E.g. it's possible to wrap command into a shell or/and patch environment variables.
 */
public abstract class RunAnythingCommandCustomizer {
  public static final ExtensionPointName<RunAnythingCommandCustomizer> EP_NAME = ExtensionPointName.create("com.intellij.runAnything.commandCustomizer");

  /**
   * Customizes command line to be executed
   *
   * @param workDirectory the working directory the command will be executed in
   * @param dataContext   {@link DataContext} to fetch module, project etc.
   * @param commandLine   command line to be customized
   * @return patched command line
   */
  @Nonnull
  protected GeneralCommandLine customizeCommandLine(@Nonnull VirtualFile workDirectory, @Nonnull DataContext dataContext, @Nonnull GeneralCommandLine commandLine) {
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
  public static GeneralCommandLine customizeCommandLine(@Nonnull DataContext dataContext, @Nonnull VirtualFile workDirectory, @Nonnull GeneralCommandLine commandLine) {
    for (RunAnythingCommandCustomizer customizer : EP_NAME.getExtensionList()) {
      commandLine = customizer.customizeCommandLine(workDirectory, dataContext, commandLine);
    }
    return commandLine;
  }

  @Nonnull
  public static DataContext customizeContext(@Nonnull DataContext dataContext) {
    for (RunAnythingCommandCustomizer customizer : EP_NAME.getExtensionList()) {
       dataContext = customizer.customizeDataContext(dataContext);
    }
    return dataContext;
  }
}