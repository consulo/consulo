package consulo.language.psi.scope;

import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * @author peter
 */
public class DelegatingGlobalSearchScope extends GlobalSearchScope {
    protected final GlobalSearchScope myBaseScope;
    private final Object myEquality;

    public DelegatingGlobalSearchScope(GlobalSearchScope baseScope) {
        super(baseScope.getProject());
        myBaseScope = baseScope;
        myEquality = new Object();
    }

    public DelegatingGlobalSearchScope(GlobalSearchScope baseScope, Object... equality) {
        super(baseScope.getProject());
        myBaseScope = baseScope;
        myEquality = Arrays.asList(equality);
    }

    @Override
    public boolean contains(VirtualFile file) {
        return myBaseScope.contains(file);
    }

    @Override
    public int compare(VirtualFile file1, VirtualFile file2) {
        return myBaseScope.compare(file1, file2);
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule) {
        return myBaseScope.isSearchInModuleContent(aModule);
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule, boolean testSources) {
        return myBaseScope.isSearchInModuleContent(aModule, testSources);
    }

    @Override
    public boolean isSearchInLibraries() {
        return myBaseScope.isSearchInLibraries();
    }

    @Override
    public boolean isSearchOutsideRootModel() {
        return myBaseScope.isSearchOutsideRootModel();
    }

    @Override
    public String getDisplayName() {
        return myBaseScope.getDisplayName();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DelegatingGlobalSearchScope that = (DelegatingGlobalSearchScope) o;

        return myBaseScope.equals(that.myBaseScope)
            && myEquality.equals(that.myEquality);
    }

    @Override
    public int hashCode() {
        return 31 * myBaseScope.hashCode() + myEquality.hashCode();
    }
}
