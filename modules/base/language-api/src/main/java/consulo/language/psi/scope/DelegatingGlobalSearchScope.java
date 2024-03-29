package consulo.language.psi.scope;

import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * @author peter
 */
public class DelegatingGlobalSearchScope extends GlobalSearchScope {
  protected final GlobalSearchScope myBaseScope;
  private final Object myEquality;

  public DelegatingGlobalSearchScope(@Nonnull GlobalSearchScope baseScope) {
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
  public boolean contains(@Nonnull VirtualFile file) {
    return myBaseScope.contains(file);
  }

  @Override
  public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    return myBaseScope.compare(file1, file2);
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule) {
    return myBaseScope.isSearchInModuleContent(aModule);
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule, boolean testSources) {
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DelegatingGlobalSearchScope that = (DelegatingGlobalSearchScope)o;

    if (!myBaseScope.equals(that.myBaseScope)) return false;
    if (!myEquality.equals(that.myEquality)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myBaseScope.hashCode();
    result = 31 * result + myEquality.hashCode();
    return result;
  }
}
