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
package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.navigation.NavigatableWithText;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.Set;

public class ModuleNode extends PackageDependenciesNode implements NavigatableWithText {
    private final Module myModule;

    public ModuleNode(Module module) {
        super(module.getProject());
        myModule = module;
    }

    @Override
    public void fillFiles(Set<PsiFile> set, boolean recursively) {
        super.fillFiles(set, recursively);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            PackageDependenciesNode child = (PackageDependenciesNode) getChildAt(i);
            child.fillFiles(set, true);
        }
    }

    @Override
    public boolean canNavigate() {
        return myModule != null && !myModule.isDisposed();
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void navigate(boolean focus) {
        ProjectSettingsService.getInstance(myModule.getProject()).openModuleSettings(myModule);
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesModule();
    }

    @Override
    public String toString() {
        return myModule == null ? AnalysisScopeLocalize.unknownNodeText().get() : myModule.getName();
    }

    public String getModuleName() {
        return myModule.getName();
    }

    public Module getModule() {
        return myModule;
    }

    @Override
    public int getWeight() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (isEquals()) {
            return super.equals(o);
        }
        return this == o
            || o instanceof ModuleNode that
            && Objects.equals(myModule, that.myModule);
    }

    @Override
    public int hashCode() {
        return myModule == null ? 0 : myModule.hashCode();
    }

    @Override
    public boolean isValid() {
        return myModule != null && !myModule.isDisposed();
    }

    @Nonnull
    @Override
    public LocalizeValue getNavigateActionText(boolean focusEditor) {
        return ProjectUIViewLocalize.actionOpenModuleSettingsText();
    }
}
