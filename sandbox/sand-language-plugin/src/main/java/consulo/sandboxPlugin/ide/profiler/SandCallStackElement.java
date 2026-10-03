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
package consulo.sandboxPlugin.ide.profiler;

import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class SandCallStackElement extends BaseCallStackElement {
    private final String myClassName;
    private final String myMethodName;

    public SandCallStackElement(String className, String methodName) {
        myClassName = className;
        myMethodName = methodName;
    }

    public String getClassName() {
        return myClassName;
    }

    public String getMethodName() {
        return myMethodName;
    }

    public String getSourceFileName() {
        return (myClassName.isEmpty() ? myMethodName : myClassName) + "." + SandFileType.INSTANCE.getDefaultExtension();
    }

    @Override
    public String fullName() {
        return myClassName.isEmpty() ? myMethodName : myClassName + "." + myMethodName;
    }

    @Override
    public boolean isNavigatable() {
        return true;
    }

    @Override
    public NavigatablePsiElement[] calcNavigatables(Project project) {
        String fileName = getSourceFileName();
        return ReadAction.compute(() -> {
            if (project.isDisposed()) {
                return NavigatablePsiElement.EMPTY_ARRAY;
            }
            try {
                PsiFile[] files = FilenameIndex.getFilesByName(project, fileName, GlobalSearchScope.projectScope(project));
                if (files.length == 0) {
                    return NavigatablePsiElement.EMPTY_ARRAY;
                }
                return Arrays.copyOf(files, files.length, NavigatablePsiElement[].class);
            }
            catch (IndexNotReadyException e) {
                return NavigatablePsiElement.EMPTY_ARRAY;
            }
        });
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SandCallStackElement that)) {
            return false;
        }
        return myClassName.equals(that.myClassName) && myMethodName.equals(that.myMethodName);
    }

    @Override
    public int hashCode() {
        return 31 * myClassName.hashCode() + myMethodName.hashCode();
    }
}
