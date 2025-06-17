package consulo.execution.coverage.view;

import consulo.ui.ex.tree.NodeDescriptor;
import consulo.project.Project;
import consulo.language.psi.PsiNamedElement;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * User: anna
 * Date: 1/2/12
 */
public class CoverageViewDescriptor extends NodeDescriptor {
    private final Object myClassOrPackage;

    public CoverageViewDescriptor(final Project project, final NodeDescriptor parentDescriptor, final Object classOrPackage) {
        super(parentDescriptor);
        myClassOrPackage = classOrPackage;
        myName = classOrPackage instanceof PsiNamedElement ? ((PsiNamedElement) classOrPackage).getName() : classOrPackage.toString();
    }

    @RequiredUIAccess
    public boolean update() {
        return false;
    }

    public Object getElement() {
        return myClassOrPackage;
    }
}
  