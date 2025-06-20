/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.ui.ex.tree.SimpleTreeAnchorizerValue;
import consulo.ui.ex.tree.TreeAnchorizer;
import consulo.ui.ex.tree.TreeAnchorizerValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.function.Supplier;

/**
 * @author peter
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.PRODUCTION)
public class PsiTreeAnchorizer extends TreeAnchorizer {
    private final Application myApplication;

    @Inject
    public PsiTreeAnchorizer(Application application) {
        myApplication = application;
    }

    @Nonnull
    @Override
    public TreeAnchorizerValue<?> createAnchorValue(Object element) {
        if (element instanceof PsiElement psiElement) {
            return myApplication.runReadAction((Supplier<TreeAnchorizerValue<?>>) () -> {
                if (!psiElement.isValid()) {
                    return new SimpleTreeAnchorizerValue(psiElement);
                }

                SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager
                    .getInstance(psiElement.getProject())
                    .createSmartPsiElementPointer(psiElement);

                return new PsiTreeAnchorizerValue(myApplication, pointer);
            });
        }
        return super.createAnchorValue(element);
    }

    @Nonnull
    @Override
    public Object createAnchor(@Nonnull Object element) {
        if (element instanceof PsiElement psi) {
            return myApplication.runReadAction((Supplier<Object>) () -> {
                if (!psi.isValid()) {
                    return psi;
                }
                return SmartPointerManager.getInstance(psi.getProject()).createSmartPsiElementPointer(psi);
            });
        }
        return super.createAnchor(element);
    }

    @RequiredReadAction
    @Override
    @Nullable
    public Object retrieveElement(@Nonnull Object pointer) {
        if (pointer instanceof SmartPsiElementPointer smartPointer) {
            return myApplication.runReadAction((Supplier<Object>) () -> smartPointer.getElement());
        }

        return super.retrieveElement(pointer);
    }

    @Override
    public void freeAnchor(Object element) {
        if (element instanceof SmartPsiElementPointer pointer) {
            myApplication.runReadAction(() -> {
                Project project = pointer.getProject();
                if (!project.isDisposed()) {
                    SmartPointerManager.getInstance(project).removePointer(pointer);
                }
            });
        }
        else {
            super.freeAnchor(element);
        }
    }
}
