package consulo.execution.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.language.editor.util.LanguageEditorNavigationUtil;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.*;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
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
import java.util.function.Supplier;

/**
 * User: anna
 * Date: 1/2/12
 */
public class CoverageListNode extends AbstractTreeNode {
    protected CoverageSuitesBundle myBundle;
    protected CoverageViewManager.StateBean myStateBean;
    private final FileStatusManager myFileStatusManager;

    public CoverageListNode(
        Project project,
        final PsiNamedElement classOrPackage,
        CoverageSuitesBundle bundle,
        CoverageViewManager.StateBean stateBean
    ) {
        super(project, classOrPackage);
        myName = ApplicationManager.getApplication().runReadAction((Supplier<String>) () -> classOrPackage.getName());
        myBundle = bundle;
        myStateBean = stateBean;
        myFileStatusManager = FileStatusManager.getInstance(myProject);
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public Collection<? extends AbstractTreeNode> getChildren() {
        final Object[] children = CoverageViewTreeStructure.getChildren(this, myBundle, myStateBean);
        return Arrays.asList((CoverageListNode[]) children);
    }

    @Override
    protected void update(final PresentationData presentation) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                final Object value = getValue();
                if (value instanceof PsiNamedElement) {

                    if (value instanceof PsiQualifiedNamedElement
                        && (myStateBean.myFlattenPackages && ((PsiNamedElement) value).getContainingFile() == null
                        || getParent() instanceof CoverageListRootNode)) {
                        presentation.setPresentableText(((PsiQualifiedNamedElement) value).getQualifiedName());
                    }
                    else {
                        presentation.setPresentableText(((PsiNamedElement) value).getName());
                    }
                    presentation.setIcon(IconDescriptorUpdaters.getIcon(((PsiElement) value), 0));
                }
            }
        });
    }

    @Override
    public FileStatus getFileStatus() {
        final PsiFile containingFile = ApplicationManager.getApplication().runReadAction(new Supplier<PsiFile>() {
            @Nullable
            @Override
            public PsiFile get() {
                Object value = getValue();
                if (value instanceof PsiElement && ((PsiElement) value).isValid()) {
                    return ((PsiElement) value).getContainingFile();
                }
                return null;
            }
        });
        return containingFile != null ? myFileStatusManager.getStatus(containingFile.getVirtualFile()) : super.getFileStatus();
    }

    @Override
    public boolean canNavigate() {
        final Object value = getValue();
        return value instanceof PsiElement && ((PsiElement) value).isValid() && ((PsiElement) value).getContainingFile() != null;
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (canNavigate()) {
            final PsiNamedElement value = (PsiNamedElement) getValue();
            if (requestFocus) {
                LanguageEditorNavigationUtil.activateFileWithPsiElement(value, true);
            }
            else if (value instanceof NavigationItem) {
                ((NavigationItem) value).navigate(requestFocus);
            }
        }
    }

    @Override
    public int getWeight() {
        return ApplicationManager.getApplication().runReadAction((Supplier<Integer>) () -> {
            //todo weighted
            final Object value = getValue();
            if (value instanceof PsiElement && ((PsiElement) value).getContainingFile() != null) {
                return 40;
            }
            return 30;
        });
    }

    public boolean contains(VirtualFile file) {
        final Object value = getValue();
        if (value instanceof PsiElement) {
            final boolean equalContainingFile = Comparing.equal(PsiUtilCore.getVirtualFile((PsiElement) value), file);
            if (equalContainingFile) {
                return true;
            }
        }
        if (value instanceof PsiDirectory) {
            return contains(file, (PsiDirectory) value);
        }
        else if (value instanceof PsiDirectoryContainer) {
            final PsiDirectory[] directories = ((PsiDirectoryContainer) value).getDirectories();
            for (PsiDirectory directory : directories) {
                if (contains(file, directory)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean contains(VirtualFile file, PsiDirectory value) {
        if (myStateBean.myFlattenPackages) {
            return Comparing.equal(value.getVirtualFile(), file.getParent());
        }

        if (VirtualFileUtil.isAncestor(value.getVirtualFile(), file, false)) {
            return true;
        }

        return false;
    }
}
