package consulo.execution.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.language.editor.util.LanguageEditorNavigationUtil;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.*;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author anna
 * @since 2012-01-02
 */
public class CoverageListNode extends AbstractTreeNode<PsiNamedElement> {
    protected CoverageSuitesBundle myBundle;
    protected CoverageViewManager.StateBean myStateBean;
    private final FileStatusManager myFileStatusManager;

    public CoverageListNode(
        Project project,
        PsiNamedElement classOrPackage,
        CoverageSuitesBundle bundle,
        CoverageViewManager.StateBean stateBean
    ) {
        super(project, classOrPackage);
        myName = myProject.getApplication().runReadAction((Supplier<String>) () -> classOrPackage.getName());
        myBundle = bundle;
        myStateBean = stateBean;
        myFileStatusManager = FileStatusManager.getInstance(myProject);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Collection<? extends AbstractTreeNode> getChildren() {
        Object[] children = CoverageViewTreeStructure.getChildren(this, myBundle, myStateBean);
        return Arrays.asList((CoverageListNode[]) children);
    }

    @Override
    protected void update(PresentationData presentation) {
        myProject.getApplication().runReadAction(() -> {
            if (getValue() instanceof PsiNamedElement namedElement) {
                if (namedElement instanceof PsiQualifiedNamedElement qualifiedNamedElement
                    && (myStateBean.myFlattenPackages && namedElement.getContainingFile() == null
                    || getParent() instanceof CoverageListRootNode)) {
                    presentation.setPresentableText(qualifiedNamedElement.getQualifiedName());
                }
                else {
                    presentation.setPresentableText(namedElement.getName());
                }
                presentation.setIcon(IconDescriptorUpdaters.getIcon(namedElement, 0));
            }
        });
    }

    @Nonnull
    @Override
    public FileStatus getFileStatus() {
        PsiFile containingFile = myProject.getApplication().runReadAction(new Supplier<PsiFile>() {
            @Nullable
            @Override
            public PsiFile get() {
                if (getValue() instanceof PsiElement element && element.isValid()) {
                    return element.getContainingFile();
                }
                return null;
            }
        });
        return containingFile != null ? myFileStatusManager.getStatus(containingFile.getVirtualFile()) : super.getFileStatus();
    }

    @Override
    public boolean canNavigate() {
        return getValue() instanceof PsiElement element && element.isValid() && element.getContainingFile() != null;
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    @RequiredUIAccess
    public void navigate(boolean requestFocus) {
        if (canNavigate()) {
            PsiNamedElement value = getValue();
            if (requestFocus) {
                LanguageEditorNavigationUtil.activateFileWithPsiElement(value, true);
            }
            else if (value instanceof NavigationItem navItem) {
                navItem.navigate(requestFocus);
            }
        }
    }

    @Override
    public int getWeight() {
        return myProject.getApplication().runReadAction((Supplier<Integer>) () -> {
            //todo weighted
            if (getValue() instanceof PsiElement element && element.getContainingFile() != null) {
                return 40;
            }
            return 30;
        });
    }

    public boolean contains(VirtualFile file) {
        PsiNamedElement namedElement = getValue();
        //noinspection SimplifiableIfStatement
        if (Objects.equals(PsiUtilCore.getVirtualFile(namedElement), file)) {
            return true;
        }

        return switch (namedElement) {
            case PsiDirectory directory -> contains(file, directory);
            case PsiDirectoryContainer directoryContainer -> {
                for (PsiDirectory directory : directoryContainer.getDirectories()) {
                    if (contains(file, directory)) {
                        yield true;
                    }
                }
                yield false;
            }
            default -> false;
        };
    }

    private boolean contains(VirtualFile file, PsiDirectory value) {
        if (myStateBean.myFlattenPackages) {
            return Comparing.equal(value.getVirtualFile(), file.getParent());
        }

        //noinspection RedundantIfStatement
        if (VirtualFileUtil.isAncestor(value.getVirtualFile(), file, false)) {
            return true;
        }

        return false;
    }
}
