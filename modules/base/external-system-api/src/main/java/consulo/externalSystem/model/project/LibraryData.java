package consulo.externalSystem.model.project;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.AbstractNamedData;
import consulo.externalSystem.service.project.Named;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2011-08-24
 */
public class LibraryData extends AbstractNamedData implements Named {
    private static final long serialVersionUID = 1L;

    private final Map<String, Set<String>> myPaths = new HashMap<>();

    private final boolean myUnresolved;

    public LibraryData(ProjectSystemId owner, String name) {
        this(owner, name, false);
    }

    public LibraryData(ProjectSystemId owner, String name, boolean unresolved) {
        super(owner, name, String.format("%s: %s", owner.getLibraryPrefix(), name));
        myUnresolved = unresolved;
    }

    public boolean isUnresolved() {
        return myUnresolved;
    }

    public Set<String> getPaths(String type) {
        Set<String> result = myPaths.get(type);
        return result == null ? Collections.<String>emptySet() : result;
    }

    public void addPath(String type, String path) {
        Set<String> paths = myPaths.get(type);
        if (paths == null) {
            myPaths.put(type, paths = new HashSet<>());
        }
        paths.add(ExternalSystemApiUtil.toCanonicalPath(path));
    }

    public Set<String> getLibraryRootTypeIds() {
        return myPaths.keySet();
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
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }

        return o instanceof LibraryData that
            && super.equals(that)
            && myUnresolved == that.myUnresolved
            && myPaths.equals(that.myPaths);
    }

    @Override
    public String toString() {
        return String.format("library %s%s", getExternalName(), myUnresolved ? "(unresolved)" : "");
    }
}
