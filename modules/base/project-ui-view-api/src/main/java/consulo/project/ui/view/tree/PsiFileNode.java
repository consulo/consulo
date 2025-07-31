/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.codeEditor.CodeInsightColors;
import consulo.component.util.Iconable;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.pom.NavigatableWithText;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.library.util.ModuleContentLibraryUtil;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public class PsiFileNode extends BasePsiNode<PsiFile> implements NavigatableWithText {
    public PsiFileNode(Project project, @Nonnull PsiFile value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    @Override
    @RequiredReadAction
    public Collection<AbstractTreeNode> getChildrenImpl() {
        Project project = getProject();
        VirtualFile jarRoot = getArchiveRoot();
        if (project != null && jarRoot != null) {
            PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(jarRoot);
            if (psiDirectory != null) {
                return BaseProjectViewDirectoryHelper.getDirectoryChildren(psiDirectory, getSettings(), true);
            }
        }

        return List.of();
    }

    private boolean isArchive() {
        VirtualFile file = getVirtualFile();
        return file != null && file.isValid() && file.getFileType() instanceof ArchiveFileType;
    }

    @Override
    @RequiredReadAction
    protected void updateImpl(PresentationData data) {
        PsiFile value = getValue();
        data.setPresentableText(value.getName());
        data.setIcon(IconDescriptorUpdaters.getIcon(value, Iconable.ICON_FLAG_READ_STATUS));

        VirtualFile file = getVirtualFile();
        if (file != null && file.is(VFileProperty.SYMLINK)) {
            String target = file.getCanonicalPath();
            if (target == null) {
                data.setAttributesKey(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
                data.setTooltip(CommonLocalize.vfsBrokenLink().get());
            }
            else {
                data.setTooltip(FileUtil.toSystemDependentName(target));
            }
        }
    }

    @Override
    public boolean canNavigate() {
        return isNavigatableLibraryRoot() || super.canNavigate();
    }

    private boolean isNavigatableLibraryRoot() {
        VirtualFile jarRoot = getArchiveRoot();
        Project project = getProject();
        if (jarRoot != null && ProjectRootsUtil.isLibraryRoot(jarRoot, project)) {
            OrderEntry orderEntry = ModuleContentLibraryUtil.findLibraryEntry(jarRoot, project);
            return orderEntry != null;
        }
        return false;
    }

    @Nullable
    private VirtualFile getArchiveRoot() {
        VirtualFile file = getVirtualFile();

        return ArchiveVfsUtil.getArchiveRootForLocalFile(file);
    }

    @Override
    @RequiredUIAccess
    public void navigate(boolean requestFocus) {
        VirtualFile jarRoot = getArchiveRoot();
        Project project = getProject();
        if (requestFocus && jarRoot != null && ProjectRootsUtil.isLibraryRoot(jarRoot, project)) {
            OrderEntry orderEntry = ModuleContentLibraryUtil.findLibraryEntry(jarRoot, project);
            if (orderEntry != null) {
                ProjectSettingsService.getInstance(project).openLibraryOrSdkSettings(orderEntry);
                return;
            }
        }

        super.navigate(requestFocus);
    }

    @Nonnull
    @Override
    public LocalizeValue getNavigateActionText(boolean focusEditor) {
        if (isNavigatableLibraryRoot()) {
            return ProjectUIViewLocalize.actionOpenLibrarySettingsText();
        }
        return LocalizeValue.empty();
    }

    @Override
    public int getWeight() {
        return 20;
    }

    @Override
    public String getTitle() {
        VirtualFile file = getVirtualFile();
        if (file != null) {
            return UserHomeFileUtil.getLocationRelativeToUserHome(file.getPresentableUrl());
        }

        return super.getTitle();
    }

    @Override
    protected boolean isMarkReadOnly() {
        return true;
    }

    @Override
    public Comparable getTypeSortKey() {
        String extension = extension(getValue());
        return extension == null ? null : new ExtensionSortKey(extension);
    }

    @Nullable
    public static String extension(@Nullable PsiFile file) {
        if (file != null) {
            VirtualFile vFile = file.getVirtualFile();
            if (vFile != null) {
                return vFile.getFileType().getDefaultExtension();
            }
        }

        return null;
    }

    public static class ExtensionSortKey implements Comparable<ExtensionSortKey> {
        private final String myExtension;

        public ExtensionSortKey(String extension) {
            myExtension = extension;
        }

        @Override
        public int compareTo(ExtensionSortKey rhs) {
            return myExtension.compareTo(rhs.myExtension);
        }
    }

    @Override
    public boolean shouldDrillDownOnEmptyElement() {
        return true;
    }

    @Override
    public boolean canRepresent(Object element) {
        return super.canRepresent(element) || getValue() != null && getValue().getVirtualFile() == element;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        return super.contains(file) || isArchive() && Comparing.equal(VirtualFilePathUtil.getLocalFile(file), getVirtualFile());
    }
}
