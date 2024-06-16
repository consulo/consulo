// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.application.AllIcons;
import consulo.dataContext.DataContext;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingAction;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingCache;
import consulo.ide.impl.idea.ide.actions.runAnything.commands.RunAnythingCommandCustomizer;
import consulo.ide.impl.idea.ide.actions.runAnything.execution.RunAnythingRunProfile;
import consulo.platform.base.localize.IdeLocalize;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

import static consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil.*;

public abstract class RunAnythingCommandProvider extends RunAnythingProviderBase<String> {
  @Override
  public void execute(@Nonnull DataContext dataContext, @Nonnull String value) {
    VirtualFile workDirectory = dataContext.getData(VirtualFile.KEY);
    Executor executor = dataContext.getData(RunAnythingAction.EXECUTOR_KEY);
    LOG.assertTrue(workDirectory != null);
    LOG.assertTrue(executor != null);

    runCommand(workDirectory, value, executor, dataContext);
  }

  public static void runCommand(
    @Nonnull VirtualFile workDirectory,
    @Nonnull String commandString,
    @Nonnull Executor executor,
    @Nonnull DataContext dataContext
  ) {
    final Project project = dataContext.getData(Project.KEY);
    LOG.assertTrue(project != null);

    Collection<String> commands = RunAnythingCache.getInstance(project).getState().getCommands();
    commands.remove(commandString);
    commands.add(commandString);

    dataContext = RunAnythingCommandCustomizer.customizeContext(dataContext);

    GeneralCommandLine initialCommandLine = new GeneralCommandLine(ParametersListUtil.parse(commandString, false, true))
      .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
      .withWorkingDirectory(workDirectory.toNioPath());

    GeneralCommandLine commandLine = RunAnythingCommandCustomizer.customizeCommandLine(dataContext, workDirectory, initialCommandLine);
    try {
      RunAnythingRunProfile runAnythingRunProfile = new RunAnythingRunProfile(commandLine, commandString);
      ExecutionEnvironmentBuilder.create(project, executor, runAnythingRunProfile).dataContext(dataContext).buildAndExecute();
    }
    catch (ExecutionException e) {
      LOG.warn(e);
      Messages.showInfoMessage(project, e.getMessage(), IdeLocalize.runAnythingConsoleErrorTitle().get());
    }
  }

  @Nullable
  @Override
  public String getAdText() {
    return IdeLocalize.runAnythingAdRunInContext(PRESSED_ALT) + ", " +
      IdeLocalize.runAnythingAdRunWithDebug(SHIFT_SHORTCUT_TEXT) + ", " +
      IdeLocalize.runAnythingAdCommandDelete(SHIFT_BACK_SPACE);
  }

  @Nonnull
  @Override
  public String getCommand(@Nonnull String value) {
    return value;
  }

  @Nullable
  @Override
  public Image getIcon(@Nonnull String value) {
    return AllIcons.Actions.Run_anything;
  }
}