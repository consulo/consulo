/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.task;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.action.PsiLocation;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * @author Denis Zhdanov
 * @since 6/5/13 8:11 PM
 */
public class ExternalSystemTaskLocation extends PsiLocation<PsiElement> {
    private final ExternalTaskExecutionInfo myTaskInfo;

    @RequiredReadAction
    public ExternalSystemTaskLocation(Project project, PsiElement psiElement, ExternalTaskExecutionInfo taskInfo) {
        super(project, psiElement);
        myTaskInfo = taskInfo;
    }

    public ExternalTaskExecutionInfo getTaskInfo() {
        return myTaskInfo;
    }

    @RequiredReadAction
    public static ExternalSystemTaskLocation create(Project project,
                                                    ProjectSystemId systemId,
                                                    @Nullable String projectPath,
                                                    ExternalTaskExecutionInfo taskInfo) {
        if (projectPath != null) {
            VirtualFile file = VirtualFileUtil.findFileByIoFile(new File(projectPath), false);
            if (file != null) {
                PsiDirectory psiFile = PsiManager.getInstance(project).findDirectory(file);
                if (psiFile != null) {
                    return new ExternalSystemTaskLocation(project, psiFile, taskInfo);
                }
            }
        }

        String name = systemId.getReadableName() + projectPath + StringUtil.join(taskInfo.getSettings().getTaskNames(), " ");
        // We create a dummy text file instead of re-using external system file in order to avoid clashing with other configuration producers.
        // For example gradle files are enhanced groovy scripts but we don't want to run them via regular IJ groovy script runners.
        // Gradle tooling api should be used for running gradle tasks instead. IJ execution sub-system operates on Location objects
        // which encapsulate PsiElement and groovy runners are automatically applied if that PsiElement IS-A GroovyFile.
        PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(name, PlainTextFileType.INSTANCE, "");
        return new ExternalSystemTaskLocation(project, psiFile, taskInfo);
    }
}
