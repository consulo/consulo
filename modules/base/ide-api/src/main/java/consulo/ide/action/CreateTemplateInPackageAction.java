/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package consulo.ide.action;

import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataContext;
import consulo.ide.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPackageSupportProvider;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public abstract class CreateTemplateInPackageAction<T extends PsiElement> extends CreateFromTemplateAction<T> {
    private final boolean myInSourceOnly;

    protected CreateTemplateInPackageAction(String text, String description, Image icon, boolean inSourceOnly) {
        super(text, description, icon);
        myInSourceOnly = inSourceOnly;
    }

    protected CreateTemplateInPackageAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon,
        boolean inSourceOnly
    ) {
        super(text, description, icon);
        myInSourceOnly = inSourceOnly;
    }

    @Override
    @Nullable
    protected T createFile(String name, String templateName, PsiDirectory dir) {
        return checkOrCreate(name, dir, templateName);
    }

    @Nullable
    protected abstract PsiElement getNavigationElement(@Nonnull T createdElement);

    @Override
    @SuppressWarnings("unchecked")
    protected boolean isAvailable(DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        IdeView view = dataContext.getData(IdeView.KEY);
        if (project == null || view == null || view.getDirectories().length == 0) {
            return false;
        }

        Module module = dataContext.getData(Module.KEY);
        if (module == null) {
            return false;
        }

        Class moduleExtensionClass = getModuleExtensionClass();
        if (moduleExtensionClass != null && ModuleUtilCore.getExtension(module, moduleExtensionClass) == null) {
            return false;
        }

        if (!myInSourceOnly) {
            return true;
        }

        ExtensionPoint<PsiPackageSupportProvider> extensionPoint =
            project.getApplication().getExtensionPoint(PsiPackageSupportProvider.class);

        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        for (PsiDirectory dir : view.getDirectories()) {
            boolean accepted = extensionPoint.anyMatchSafe(provider -> provider.acceptVirtualFile(module, dir.getVirtualFile()));

            if (accepted && projectFileIndex.isInSourceContent(dir.getVirtualFile()) && checkPackageExists(dir)) {
                return true;
            }
        }

        return false;
    }

    protected abstract boolean checkPackageExists(PsiDirectory directory);

    @Nullable
    private T checkOrCreate(String newName, PsiDirectory directory, String templateName) throws IncorrectOperationException {
        PsiDirectory dir = directory;
        String className = removeExtension(templateName, newName);

        if (className.contains(".")) {
            String[] names = className.split("\\.");

            for (int i = 0; i < names.length - 1; i++) {
                dir = CreateFileAction.findOrCreateSubdirectory(dir, names[i]);
            }

            className = names[names.length - 1];
        }

        DumbService service = DumbService.getInstance(dir.getProject());
        service.setAlternativeResolveEnabled(true);
        try {
            return doCreate(dir, className, templateName);
        }
        finally {
            service.setAlternativeResolveEnabled(false);
        }
    }

    protected String removeExtension(String templateName, String className) {
        String extension = StringUtil.getShortName(templateName);
        if (StringUtil.isNotEmpty(extension)) {
            className = StringUtil.trimEnd(className, "." + extension);
        }
        return className;
    }

    @Nullable
    protected abstract T doCreate(PsiDirectory dir, String className, String templateName) throws IncorrectOperationException;
}
