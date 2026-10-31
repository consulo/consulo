/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.codeBlock;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.dataContext.DataContext;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runAnything.codeBlock.CodeBlockCommandRunner;
import consulo.execution.runAnything.codeBlock.CodeBlockRunner;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingAction;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingCommandProvider;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingRecentProjectProvider;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingRunConfigurationProvider;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.execution.runAnything.RunAnythingContext;
import consulo.execution.runAnything.RunAnythingProvider;
import consulo.language.Language;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceImpl
public class CodeBlockCommandRunnerImpl implements CodeBlockCommandRunner {
    /**
     * What Run Anything finds for a command and how to run it
     */
    private record Match(RunAnythingProvider<Object> provider, Object value, DataContext dataContext) {
    }

    private final Project myProject;

    @Inject
    public CodeBlockCommandRunnerImpl(Project project) {
        myProject = project;
    }

    @Override
    @RequiredReadAction
    public boolean canRunLine(String line, @Nullable VirtualFile workingDirectory, boolean inlineCode) {
        return findMatch(line, workingDirectory, inlineCode) != null;
    }

    @Override
    @RequiredUIAccess
    public void runLine(String line, @Nullable VirtualFile workingDirectory, boolean inlineCode) {
        Match match = ReadAction.compute(() -> findMatch(line, workingDirectory, inlineCode));
        if (match != null) {
            match.provider().execute(match.dataContext(), match.value());
        }
    }

    @Override
    public @Nullable CodeBlockRunner findBlockRunner(String languageName, @Nullable Language language) {
        return myProject.getApplication()
            .getExtensionPoint(CodeBlockRunner.class)
            .findFirstSafe(runner -> runner.isApplicable(languageName, language));
    }

    @SuppressWarnings("unchecked")
    private @Nullable Match findMatch(String line, @Nullable VirtualFile workingDirectory, boolean inlineCode) {
        String command = CodeBlockCommandRunner.trimPrompt(line).trim();
        if (command.isEmpty() || command.contains("\n")) {
            return null;
        }

        DataContext dataContext = createDataContext(workingDirectory);
        return myProject.getApplication()
            .getExtensionPoint(RunAnythingProvider.class)
            .computeSafeIfAny(provider -> {
                if (!isDetecting(provider, inlineCode)) {
                    return null;
                }
                Object value = provider.findMatchingValue(dataContext, command);
                return value == null ? null : new Match((RunAnythingProvider<Object>) provider, value, dataContext);
            });
    }

    /**
     * A provider which takes any text as a command to a shell, or a recent project by its name, matches nearly every line of a
     * document; a run configuration is named only in inline code
     */
    private static boolean isDetecting(RunAnythingProvider<?> provider, boolean inlineCode) {
        if (provider instanceof RunAnythingCommandProvider || provider instanceof RunAnythingRecentProjectProvider) {
            return false;
        }
        return inlineCode || !(provider instanceof RunAnythingRunConfigurationProvider);
    }

    private DataContext createDataContext(@Nullable VirtualFile workingDirectory) {
        SimpleDataContext.Builder builder = SimpleDataContext.builder()
            .add(Project.KEY, myProject)
            .add(RunAnythingAction.EXECUTOR_KEY, DefaultRunExecutor.getRunExecutorInstance());
        if (workingDirectory != null) {
            builder.add(VirtualFile.KEY, workingDirectory);
            builder.add(RunAnythingProvider.EXECUTING_CONTEXT, new RunAnythingContext.RecentDirectoryContext(workingDirectory.getPath()));
        }
        return builder.build();
    }
}
