/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.ide.projectView.impl.nodes;

import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.ui.ex.tree.TreeAnchorizerValue;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-02-12
 */
public class PsiTreeAnchorizerValue implements TreeAnchorizerValue<PsiElement> {
    private final Application myApplication;
    private final SmartPsiElementPointer myPointer;

    public PsiTreeAnchorizerValue(Application application, SmartPsiElementPointer pointer) {
        myApplication = application;
        myPointer = pointer;
    }

    @Nullable
    @Override
    public PsiElement extractValue() {
        return myApplication.runReadAction((Supplier<PsiElement>) () -> myPointer.getElement());
    }

    @Override
    public void dispose() {
        myApplication.runReadAction(() -> {
            Project project = myPointer.getProject();
            if (!project.isDisposed()) {
                SmartPointerManager.getInstance(project).removePointer(myPointer);
            }
        });
    }

    @Override
    public String toString() {
        return "PsiTreeAnchorizerValue{" +
            "myPointer=" + myPointer +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PsiTreeAnchorizerValue that = (PsiTreeAnchorizerValue) o;
        return Objects.equals(myPointer, that.myPointer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myPointer);
    }
}
