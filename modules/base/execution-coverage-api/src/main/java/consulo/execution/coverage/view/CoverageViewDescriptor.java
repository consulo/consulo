package consulo.execution.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.project.Project;
import consulo.language.psi.PsiNamedElement;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author anna
 * @since 2012-01-02
 */
public class CoverageViewDescriptor extends NodeDescriptor {
    private final Object myClassOrPackage;

    @RequiredReadAction
    public CoverageViewDescriptor(Project project, NodeDescriptor parentDescriptor, Object classOrPackage) {
        super(parentDescriptor);
        myClassOrPackage = classOrPackage;
        myName = classOrPackage instanceof PsiNamedElement namedElem ? namedElem.getName() : classOrPackage.toString();
    }

    @Override
    @RequiredUIAccess
    public boolean update() {
        return false;
    }

    @Override
    public Object getElement() {
        return myClassOrPackage;
    }
}
  