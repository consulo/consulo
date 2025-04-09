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
import consulo.application.AllIcons;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiPackage;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class PackageElementNode extends ProjectViewNode<PackageElement> {
    public PackageElementNode(
        @Nonnull Project project,
        final PackageElement value,
        final ViewSettings viewSettings
    ) {
        super(project, value, viewSettings);
    }

    public PackageElementNode(
        @Nonnull Project project,
        final Object value,
        final ViewSettings viewSettings
    ) {
        this(project, (PackageElement)value, viewSettings);
    }

    @Override
    public boolean contains(@Nonnull final VirtualFile file) {
        if (!isUnderContent(file) || getValue() == null) {
            return false;
        }

        final PsiDirectory[] directories = getValue().getPackage().getDirectories();
        for (PsiDirectory directory : directories) {
            if (VirtualFileUtil.isAncestor(directory.getVirtualFile(), file, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnderContent(final VirtualFile file) {
        PackageElement element = getValue();
        final Module module = element == null ? null : element.getModule();
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
        final PackageElement value = getValue();
        if (value == null) {
            return Collections.emptyList();
        }
        final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
        final Module module = value.getModule();
        final PsiPackage aPackage = value.getPackage();

        if (!getSettings().isFlattenPackages()) {

            final PsiPackage[] subpackages = PackageNodeUtil.getSubpackages(aPackage, module, myProject, isLibraryElement());
            for (PsiPackage subpackage : subpackages) {
                PackageNodeUtil.addPackageAsChild(children, subpackage, module, getSettings(), isLibraryElement());
            }
        }
        // process only files in package's directories
        final PsiDirectory[] dirs = PackageNodeUtil.getDirectories(aPackage, myProject, module, isLibraryElement());
        for (final PsiDirectory dir : dirs) {
            children.addAll(BaseProjectViewDirectoryHelper.getDirectoryChildren(dir, getSettings(), false));
        }
        return children;
    }


    @Override
    protected void update(final PresentationData presentation) {
        if (getValue() != null && getValue().getPackage().isValid()) {
            updateValidData(presentation);
        }
        else {
            setValue(null);
        }
    }

    private void updateValidData(final PresentationData presentation) {
        final PackageElement value = getValue();
        final PsiPackage aPackage = value.getPackage();

        if (!getSettings().isFlattenPackages()
            && getSettings().isHideEmptyMiddlePackages()
            && PackageNodeUtil.isPackageEmpty(aPackage, value.getModule(), true, isLibraryElement())) {
            setValue(null);
            return;
        }

        PsiPackage parentPackage;
        Object parentValue = getParentValue();
        if (parentValue instanceof PackageElement) {
            parentPackage = ((PackageElement)parentValue).getPackage();
        }
        else {
            parentPackage = null;
        }
        String qName = aPackage.getQualifiedName();
        String name = TreeViewUtil.getNodeName(getSettings(), aPackage, parentPackage, qName, showFQName(aPackage));
        presentation.setPresentableText(name);

        presentation.setIcon(AllIcons.Nodes.Package);

        for (ProjectViewNodeDecorator decorator : ProjectViewNodeDecorator.EP_NAME.getExtensionList(myProject)) {
            decorator.decorate(this, presentation);
        }
    }

    private boolean showFQName(final PsiPackage aPackage) {
        return getSettings().isFlattenPackages() && !aPackage.getQualifiedName().isEmpty();
    }

    @Override
    public String getTestPresentation() {
        final PresentationData presentation = new PresentationData();
        update(presentation);
        return "PsiJavaPackage: " + presentation.getPresentableText();
    }

    @Override
    public boolean valueIsCut() {
        return getValue() != null && CopyPasteManager.getInstance().isCutElement(getValue().getPackage());
    }

    @Nonnull
    public VirtualFile[] getVirtualFiles() {
        final PackageElement value = getValue();
        if (value == null) {
            return VirtualFile.EMPTY_ARRAY;
        }
        final PsiDirectory[] directories = PackageNodeUtil
            .getDirectories(value.getPackage(), getProject(), value.getModule(), isLibraryElement());
        final VirtualFile[] result = new VirtualFile[directories.length];
        for (int i = 0; i < directories.length; i++) {
            PsiDirectory directory = directories[i];
            result[i] = directory.getVirtualFile();
        }
        return result;
    }

    @Override
    public boolean canRepresent(final Object element) {
        if (super.canRepresent(element)) {
            return true;
        }
        final PackageElement value = getValue();
        if (value == null) {
            return true;
        }
        if (element instanceof PackageElement) {
            final PackageElement packageElement = (PackageElement)element;
            final String otherPackage = packageElement.getPackage().getQualifiedName();
            final String aPackage = value.getPackage().getQualifiedName();
            if (otherPackage.equals(aPackage)) {
                return true;
            }
        }
        if (element instanceof PsiDirectory) {
            final PsiDirectory directory = (PsiDirectory)element;
            return Arrays.asList(value.getPackage().getDirectories()).contains(directory);
        }
        return false;
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public String getTitle() {
        final PackageElement packageElement = getValue();
        if (packageElement == null) {
            return super.getTitle();
        }
        return packageElement.getPackage().getQualifiedName();
    }

    @Override
    @Nullable
    public String getQualifiedNameSortKey() {
        final PackageElement packageElement = getValue();
        if (packageElement != null) {
            return packageElement.getPackage().getQualifiedName();
        }
        return null;
    }

    @Override
    public int getTypeSortWeight(final boolean sortByType) {
        return 4;
    }

    @Override
    public boolean isAlwaysShowPlus() {
        for (final VirtualFile dir : getVirtualFiles()) {
            if (dir.getChildren().length > 0) {
                return true;
            }
        }
        return false;
    }
}
