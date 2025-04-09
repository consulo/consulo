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
package consulo.project.ui.view.tree;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiPackage;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class PackageElementNode extends ProjectViewNode<PackageElement> {
    public PackageElementNode(@Nonnull Project project, PackageElement value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    public PackageElementNode(@Nonnull Project project, Object value, ViewSettings viewSettings) {
        this(project, (PackageElement)value, viewSettings);
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        if (!isUnderContent(file) || getValue() == null) {
            return false;
        }

        PsiDirectory[] directories = getValue().getPackage().getDirectories();
        for (PsiDirectory directory : directories) {
            if (VirtualFileUtil.isAncestor(directory.getVirtualFile(), file, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnderContent(VirtualFile file) {
        PackageElement element = getValue();
        Module module = element == null ? null : element.getModule();
        if (module == null) {
            return ModuleUtilCore.projectContainsFile(getProject(), file, isLibraryElement());
        }
        else {
            return ModuleUtilCore.moduleContainsFile(module, file, isLibraryElement());
        }
    }

    private boolean isLibraryElement() {
        return getValue() != null && getValue().isLibraryElement();
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public Collection<AbstractTreeNode> getChildren() {
        PackageElement value = getValue();
        if (value == null) {
            return Collections.emptyList();
        }
        List<AbstractTreeNode> children = new ArrayList<>();
        Module module = value.getModule();
        PsiPackage aPackage = value.getPackage();

        if (!getSettings().isFlattenPackages()) {
            PsiPackage[] subpackages = PackageNodeUtil.getSubpackages(aPackage, module, myProject, isLibraryElement());
            for (PsiPackage subpackage : subpackages) {
                PackageNodeUtil.addPackageAsChild(children, subpackage, module, getSettings(), isLibraryElement());
            }
        }
        // process only files in package's directories
        PsiDirectory[] dirs = PackageNodeUtil.getDirectories(aPackage, myProject, module, isLibraryElement());
        for (PsiDirectory dir : dirs) {
            children.addAll(BaseProjectViewDirectoryHelper.getDirectoryChildren(dir, getSettings(), false));
        }
        return children;
    }

    @Override
    @RequiredUIAccess
    protected void update(PresentationData presentation) {
        if (getValue() != null && getValue().getPackage().isValid()) {
            updateValidData(presentation);
        }
        else {
            setValue(null);
        }
    }

    @RequiredUIAccess
    private void updateValidData(PresentationData presentation) {
        PackageElement value = getValue();
        PsiPackage aPackage = value.getPackage();

        if (!getSettings().isFlattenPackages()
            && getSettings().isHideEmptyMiddlePackages()
            && PackageNodeUtil.isPackageEmpty(aPackage, value.getModule(), true, isLibraryElement())) {
            setValue(null);
            return;
        }

        Object parentValue = getParentValue();
        PsiPackage parentPackage = parentValue instanceof PackageElement packageElement ? packageElement.getPackage() : null;
        String qName = aPackage.getQualifiedName();
        String name = TreeViewUtil.getNodeName(getSettings(), aPackage, parentPackage, qName, showFQName(aPackage));
        presentation.setPresentableText(name);

        presentation.setIcon(PlatformIconGroup.nodesPackage());

        for (ProjectViewNodeDecorator decorator : ProjectViewNodeDecorator.EP_NAME.getExtensionList(myProject)) {
            decorator.decorate(this, presentation);
        }
    }

    private boolean showFQName(PsiPackage aPackage) {
        return getSettings().isFlattenPackages() && !aPackage.getQualifiedName().isEmpty();
    }

    @Override
    @RequiredUIAccess
    public String getTestPresentation() {
        PresentationData presentation = new PresentationData();
        update(presentation);
        return "PsiJavaPackage: " + presentation.getPresentableText();
    }

    @Override
    public boolean valueIsCut() {
        return getValue() != null && CopyPasteManager.getInstance().isCutElement(getValue().getPackage());
    }

    @Nonnull
    public VirtualFile[] getVirtualFiles() {
        PackageElement value = getValue();
        if (value == null) {
            return VirtualFile.EMPTY_ARRAY;
        }
        PsiDirectory[] directories =
            PackageNodeUtil.getDirectories(value.getPackage(), getProject(), value.getModule(), isLibraryElement());
        VirtualFile[] result = new VirtualFile[directories.length];
        for (int i = 0; i < directories.length; i++) {
            PsiDirectory directory = directories[i];
            result[i] = directory.getVirtualFile();
        }
        return result;
    }

    @Override
    public boolean canRepresent(Object element) {
        if (super.canRepresent(element)) {
            return true;
        }
        PackageElement value = getValue();
        if (value == null) {
            return true;
        }
        if (element instanceof PackageElement packageElement) {
            String otherPackage = packageElement.getPackage().getQualifiedName();
            String aPackage = value.getPackage().getQualifiedName();
            if (otherPackage.equals(aPackage)) {
                return true;
            }
        }
        return element instanceof PsiDirectory directory
            && Arrays.asList(value.getPackage().getDirectories()).contains(directory);
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public String getTitle() {
        PackageElement packageElement = getValue();
        if (packageElement == null) {
            return super.getTitle();
        }
        return packageElement.getPackage().getQualifiedName();
    }

    @Override
    @Nullable
    public String getQualifiedNameSortKey() {
        PackageElement packageElement = getValue();
        if (packageElement != null) {
            return packageElement.getPackage().getQualifiedName();
        }
        return null;
    }

    @Override
    public int getTypeSortWeight(boolean sortByType) {
        return 4;
    }

    @Override
    public boolean isAlwaysShowPlus() {
        for (VirtualFile dir : getVirtualFiles()) {
            if (dir.getChildren().length > 0) {
                return true;
            }
        }
        return false;
    }
}
