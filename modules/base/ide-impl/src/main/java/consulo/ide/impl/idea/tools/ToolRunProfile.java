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
package consulo.ide.impl.idea.tools;

import consulo.dataContext.DataContext;
import consulo.execution.ExecutionManager;
import consulo.execution.ExecutionResult;
import consulo.execution.configuration.CommandLineState;
import consulo.execution.configuration.ModuleRunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.Executor;
import consulo.execution.process.ProcessTerminatedListener;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.ui.console.RegexpFilter;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.pathMacro.Macro;
import consulo.pathMacro.MacroManager;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.ProcessOutputTypes;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * @since 2005-03-30
 */
public class ToolRunProfile implements ModuleRunProfile {
    private static final Logger LOG = Logger.getInstance(ToolRunProfile.class);
    private final Tool myTool;
    private final DataContext myContext;
    private final GeneralCommandLine myCommandLine;

    public ToolRunProfile(Tool tool, DataContext context) {
        myTool = tool;
        myContext = context;
        myCommandLine = myTool.createCommandLine(context);
        //if (context instanceof DataManagerImpl.MyDataContext) {
        //  // hack: macro.expand() can cause UI events such as showing dialogs ('Prompt' macro) which may 'invalidate' the datacontext
        //  // since we know exactly that context is valid, we need to update its event count
        //  ((DataManagerImpl.MyDataContext)context).setEventCount(IdeEventQueue.getInstance().getEventCount());
        //}
    }

    @Override
    public String getName() {
        return expandMacrosInName(myTool, myContext);
    }

    public static String expandMacrosInName(Tool tool, DataContext context) {
        String name = tool.getName();
        try {
            return MacroManager.getInstance().expandMacrosInString(name, true, context);
        }
        catch (Macro.ExecutionCancelledException e) {
            LOG.info(e);
            return name;
        }
    }

    @Override
    public Image getIcon() {
        return null;
    }

    @Override
    @Nonnull
    public Module[] getModules() {
        return Module.EMPTY_ARRAY;
    }

    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull final ExecutionEnvironment env) {
        final Project project = env.getProject();
        if (myCommandLine == null) {
            // can return null if creation of cmd line has been cancelled
            return null;
        }

        CommandLineState commandLineState = new CommandLineState(env) {
            GeneralCommandLine createCommandLine() {
                return myCommandLine;
            }

            @Override
            @Nonnull
            protected ProcessHandler startProcess() throws ExecutionException {
                GeneralCommandLine commandLine = createCommandLine();
                ProcessHandler processHandler = ProcessHandlerBuilder.create(commandLine)
                    .colored()
                    .build();
                ProcessTerminatedListener.attach(processHandler);
                return processHandler;
            }

            @Override
            @Nonnull
            public ExecutionResult execute(@Nonnull final Executor executor, @Nonnull ProgramRunner runner) throws ExecutionException {
                ExecutionResult result = super.execute(executor, runner);
                final ProcessHandler processHandler = result.getProcessHandler();
                if (processHandler != null) {
                    processHandler.addProcessListener(new ToolProcessAdapter(project, myTool.synchronizeAfterExecution(), getName()));
                    processHandler.addProcessListener(new ProcessAdapter() {
                        @Override
                        public void onTextAvailable(ProcessEvent event, Key outputType) {
                            if ((outputType == ProcessOutputTypes.STDOUT && myTool.isShowConsoleOnStdOut())
                                || (outputType == ProcessOutputTypes.STDERR && myTool.isShowConsoleOnStdErr())) {
                                ExecutionManager.getInstance(project).getContentManager().toFrontRunContent(executor, processHandler);
                            }
                        }
                    });
                }
                return result;
            }
        };
        TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        FilterInfo[] outputFilters = myTool.getOutputFilters();
        for (FilterInfo outputFilter : outputFilters) {
            builder.addFilter(new RegexpFilter(project, outputFilter.getRegExp()));
        }

        commandLineState.setConsoleBuilder(builder);
        return commandLineState;
    }

}
