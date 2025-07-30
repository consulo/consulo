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
package consulo.project.ui.view.tree;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.UserHomeFileUtil;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.pom.NavigatableWithText;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiPackageHelper;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.library.util.ModuleContentLibraryUtil;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

public class PsiDirectoryNode extends BasePsiNode<PsiDirectory> implements NavigatableWithText {
    private final PsiFileSystemItemFilter myFilter;

    public PsiDirectoryNode(Project project, @Nonnull PsiDirectory value, ViewSettings viewSettings) {
        this(project, value, viewSettings, null);
    }

    public PsiDirectoryNode(
        Project project,
        @Nonnull PsiDirectory value,
        ViewSettings viewSettings,
        @Nullable PsiFileSystemItemFilter filter
    ) {
        super(project, value, viewSettings);
        myFilter = filter;
    }

    @Nullable
    public PsiFileSystemItemFilter getFilter() {
        return myFilter;
    }

    @Override
    @RequiredReadAction
    protected void updateImpl(@Nonnull PresentationData data) {
        Project project = getProject();
        PsiDirectory psiDirectory = getValue();
        VirtualFile directoryFile = psiDirectory.getVirtualFile();

        Object parentValue = getParentValue();
        if (ProjectRootsUtil.isModuleContentRoot(directoryFile, project)) {
            ProjectFileIndex fi = ProjectRootManager.getInstance(project).getFileIndex();
            Module module = fi.getModuleForFile(directoryFile);

            data.setPresentableText(directoryFile.getName());
            if (module != null) {
                if (!(parentValue instanceof Module)) {
                    if (Comparing.equal(module.getName(), directoryFile.getName())) {
                        data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    }
                    else {
                        data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                        data.addText("[" + module.getName() + "]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    }
                }
                else {
                    data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }

                if (parentValue instanceof Module || parentValue instanceof Project) {
                    String location = UserHomeFileUtil.getLocationRelativeToUserHome(directoryFile.getPresentableUrl());
                    data.addText(" (" + location + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                else if (ProjectRootsUtil.isSourceOrTestRoot(directoryFile, project)) {
                    if (ProjectRootsUtil.isInTestSource(directoryFile, project)) {
                        data.addText(" (test source root)", SimpleTextAttributes.GRAY_ATTRIBUTES);
                    }
                    else {
                        data.addText(" (source root)", SimpleTextAttributes.GRAY_ATTRIBUTES);
                    }
                }
                return;
            }
        }

        String name = parentValue instanceof Project
            ? psiDirectory.getVirtualFile().getPresentableUrl()
            : BaseProjectViewDirectoryHelper.getNodeName(getSettings(), parentValue, psiDirectory);

        data.setPresentableText(name);
        if (ProjectRootsUtil.isLibraryRoot(directoryFile, project)) {
            data.setLocationString("library home");
        }
        else {
            data.setLocationString(BaseProjectViewDirectoryHelper.getLocationString(psiDirectory));
        }
    }

    @Override
    @RequiredReadAction
    public Collection<AbstractTreeNode> getChildrenImpl() {
        return BaseProjectViewDirectoryHelper.getDirectoryChildren(getValue(), getSettings(), true);
    }

    @Override
    @RequiredReadAction
    @SuppressWarnings("deprecation")
    public String getTestPresentation() {
        return "PsiDirectory: " + getValue().getName();
    }

    @RequiredReadAction
    public boolean isFQNameShown() {
        return BaseProjectViewDirectoryHelper.isShowFQName(getProject(), getSettings(), getParentValue(), getValue());
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        PsiDirectory value = getValue();
        if (value == null) {
            return false;
        }

        VirtualFile directory = value.getVirtualFile();
        if (directory.getFileSystem() instanceof LocalFileSystem) {
            file = VirtualFilePathUtil.getLocalFile(file);
        }

        if (!VirtualFileUtil.isAncestor(directory, file, false)) {
            return false;
        }

        return !FileTypeRegistry.getInstance().isFileIgnored(file);
    }

    @Override
    public boolean canRepresent(Object element) {
        if (super.canRepresent(element)) {
            return true;
        }
        PsiDirectory directory = getValue();
        if (directory == null) {
            return false;
        }
        return BaseProjectViewDirectoryHelper.canRepresent(element, directory);
    }

    @Override
    public boolean canNavigate() {
        VirtualFile file = getVirtualFile();
        Project project = getProject();

        ProjectSettingsService service = ProjectSettingsService.getInstance(myProject);
        return file != null &&
            ((ProjectRootsUtil.isModuleContentRoot(file, project) && service.canOpenModuleSettings())
                || (ProjectRootsUtil.isModuleSourceRoot(file, project) && service.canOpenContentEntriesSettings())
                || (ProjectRootsUtil.isLibraryRoot(file, project) && service.canOpenModuleLibrarySettings()));
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void navigate(boolean requestFocus) {
        Module module = ModuleUtilCore.findModuleForPsiElement(getValue());
        if (module != null) {
            VirtualFile file = getVirtualFile();
            Project project = getProject();
            ProjectSettingsService service = ProjectSettingsService.getInstance(myProject);
            if (ProjectRootsUtil.isModuleContentRoot(file, project)) {
                service.openModuleSettings(module);
            }
            else if (ProjectRootsUtil.isLibraryRoot(file, project)) {
                OrderEntry orderEntry = ModuleContentLibraryUtil.findLibraryEntry(file, module.getProject());
                if (orderEntry != null) {
                    service.openLibraryOrSdkSettings(orderEntry);
                }
            }
            else {
                service.openContentEntriesSettings(module);
            }
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getNavigateActionText(boolean focusEditor) {
        VirtualFile file = getVirtualFile();
        Project project = getProject();

        if (file != null) {
            if (ProjectRootsUtil.isModuleContentRoot(file, project) || ProjectRootsUtil.isSourceOrTestRoot(file, project)) {
                return ProjectUIViewLocalize.actionOpenModuleSettingsText();
            }
            if (ProjectRootsUtil.isLibraryRoot(file, project)) {
                return ProjectUIViewLocalize.actionOpenLibrarySettingsText();
            }
        }

        return LocalizeValue.empty();
    }

    @Override
    @RequiredReadAction
    public int getWeight() {
        ProjectView projectView = ProjectView.getInstance(myProject);
        if (projectView.isFoldersAlwaysOnTop()) {
            return 20;
        }
        return isFQNameShown() ? 70 : 0;
    }

    @Override
    public String getTitle() {
        PsiDirectory directory = getValue();
        if (directory != null) {
            return PsiPackageHelper.getInstance(getProject()).getQualifiedName(directory, true);
        }
        return super.getTitle();
    }

    @Override
    public String getQualifiedNameSortKey() {
        PsiPackageHelper factory = PsiPackageHelper.getInstance(getProject());
        return factory.getQualifiedName(getValue(), true);
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
        return 3;
    }

    @Override
    public boolean shouldDrillDownOnEmptyElement() {
        return true;
    }

    @Override
    public boolean isAlwaysShowPlus() {
        VirtualFile file = getVirtualFile();
        return file == null || file.getChildren().length > 0;
    }
}
