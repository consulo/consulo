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
package consulo.compiler.execution;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.compiler.CompileStatusNotification;
import consulo.compiler.CompilerManager;
import consulo.compiler.scope.CompileScope;
import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfileWithCompileBeforeLaunchOption;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import jakarta.inject.Inject;

/**
 * @author spleaner
 */
@ExtensionImpl(id = "compileBeforeRun")
public class CompileStepBeforeRun extends CompileStepBeforeRunBase<CompileStepBeforeRun.MakeBeforeRunTask> {
    /**
     * Marked for disable adding CompileStepBeforeRun
     */
    public static interface Suppressor {
    }

    private static final Logger LOG = Logger.getInstance(CompileStepBeforeRun.class);
    public static final Key<MakeBeforeRunTask> ID = Key.create("Make");

    protected static final String MAKE_PROJECT_ON_RUN_KEY = "makeProjectOnRun";

    @Inject
    public CompileStepBeforeRun(Project project) {
        super(project);
    }

    @Override
    public Key<MakeBeforeRunTask> getId() {
        return ID;
    }

    @Override
    public LocalizeValue getName() {
        return ExecutionLocalize.beforeLaunchCompileStep();
    }

    @Override
    public LocalizeValue getDescription(MakeBeforeRunTask task) {
        return ExecutionLocalize.beforeLaunchCompileStep();
    }

    @Override
    public MakeBeforeRunTask createTask(RunConfiguration configuration) {
        MakeBeforeRunTask task = null;

        if (!(configuration instanceof Suppressor) && configuration instanceof RunProfileWithCompileBeforeLaunchOption) {
            task = new MakeBeforeRunTask();
            if (configuration instanceof RunConfigurationBase runConfiguration) {
                task.setEnabled(runConfiguration.isCompileBeforeLaunchAddedByDefault());
            }
        }
        return task;
    }

    @Override
    public AsyncResult<Void> executeTaskAsync(
        UIAccess uiAccess,
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment env,
        MakeBeforeRunTask task
    ) {
        return doMake(uiAccess, myProject, configuration, env, false);
    }

    static AsyncResult<Void> doMake(
        UIAccess uiAccess,
        Project myProject,
        RunConfiguration configuration,
        ExecutionEnvironment env,
        boolean ignoreErrors
    ) {
        if (!(configuration instanceof RunProfileWithCompileBeforeLaunchOption runConfiguration)) {
            return AsyncResult.rejected();
        }

        if (configuration instanceof RunConfigurationBase rcb && rcb.excludeCompileBeforeLaunchOption()) {
            return AsyncResult.resolved();
        }

        AsyncResult<Void> result = AsyncResult.undefined();
        try {
            CompileStatusNotification callback = (aborted, errors, warnings, compileContext) -> {
                if ((errors == 0 || ignoreErrors) && !aborted) {
                    result.setDone();
                }
                else {
                    result.setRejected();
                }
            };

            boolean[] isTestCompile = new boolean[]{true};
            try {
                isTestCompile[0] = DumbService.getInstance(myProject)
                    .runWithAlternativeResolveEnabled(runConfiguration::includeTestScope);
            }
            catch (IndexNotReadyException ignored) {
            }

            CompileScope scope;
            CompilerManager compilerManager = CompilerManager.getInstance(myProject);
            if (Comparing.equal(Boolean.TRUE.toString(), System.getProperty(MAKE_PROJECT_ON_RUN_KEY))) {
                // user explicitly requested whole-project make
                scope = ReadAction.computeNotNull(() -> compilerManager.createProjectCompileScope(isTestCompile[0]));
            }
            else {
                Module[] modules = runConfiguration.getModules();
                if (modules.length > 0) {
                    for (Module module : modules) {
                        if (module == null) {
                            LOG.error(
                                "RunConfiguration should not return null modules. Configuration=" + runConfiguration.getName() +
                                    "; class=" + runConfiguration.getClass().getName()
                            );
                        }
                    }
                    scope = ReadAction.computeNotNull(() -> compilerManager.createModulesCompileScope(modules, true, isTestCompile[0]));
                }
                else if (runConfiguration.isBuildProjectOnEmptyModuleList()) {
                    scope = ReadAction.computeNotNull(() -> compilerManager.createProjectCompileScope(isTestCompile[0]));
                }
                else {
                    result.setDone();
                    return result;
                }
            }

            scope.putUserData(RunConfiguration.KEY, configuration);
            scope.putUserData(ExecutionEnvironment.KEY, env);

            uiAccess.give(() -> compilerManager.make(scope, callback));
        }
        catch (Exception e) {
            result.rejectWithThrowable(e);
        }

        return result;
    }

    public static class MakeBeforeRunTask extends BeforeRunTask<MakeBeforeRunTask> {
        private MakeBeforeRunTask() {
            super(ID);
            setEnabled(true);
        }
    }
}
