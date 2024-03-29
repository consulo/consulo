package consulo.externalSystem.model.project;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.AbstractNamedData;
import consulo.externalSystem.service.project.Named;
import consulo.externalSystem.util.ExternalSystemApiUtil;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/24/11 4:50 PM
 */
public class LibraryData extends AbstractNamedData implements Named {

  private static final long serialVersionUID = 1L;

  private final Map<LibraryPathType, Set<String>> myPaths = new HashMap<LibraryPathType, Set<String>>();

  private final boolean myUnresolved;

  public LibraryData(@Nonnull ProjectSystemId owner, @Nonnull String name) {
    this(owner, name, false);
  }

  public LibraryData(@Nonnull ProjectSystemId owner, @Nonnull String name, boolean unresolved) {
    super(owner, name, String.format("%s: %s", owner.getLibraryPrefix(), name));
    myUnresolved = unresolved;
  }

  public boolean isUnresolved() {
    return myUnresolved;
  }

  @Nonnull
  public Set<String> getPaths(@Nonnull LibraryPathType type) {
    Set<String> result = myPaths.get(type);
    return result == null ? Collections.<String>emptySet() : result;
  }

  public void addPath(@Nonnull LibraryPathType type, @Nonnull String path) {
    Set<String> paths = myPaths.get(type);
    if (paths == null) {
      myPaths.put(type, paths = new HashSet<String>());
    }
    paths.add(ExternalSystemApiUtil.toCanonicalPath(path));
  }

  public void forgetAllPaths() {
    myPaths.clear();
  }

  @Override
  public int hashCode() {
    int result = myPaths.hashCode();
    result = 31 * result + super.hashCode();
    result = 31 * result + (myUnresolved ? 0 : 1);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;

    LibraryData that = (LibraryData)o;
    return super.equals(that) && myUnresolved == that.myUnresolved && myPaths.equals(that.myPaths);
  }

  @Override
  public String toString() {
    return String.format("library %s%s", getExternalName(), myUnresolved ? "(unresolved)" : "");
  }
}
