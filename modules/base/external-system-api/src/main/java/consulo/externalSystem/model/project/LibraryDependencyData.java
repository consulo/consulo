package consulo.externalSystem.model.project;

import consulo.externalSystem.service.project.Named;
import org.jspecify.annotations.Nullable;

/**
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2011-08-10
 */
public class LibraryDependencyData extends AbstractDependencyData<LibraryData> implements Named {
    private final LibraryLevel myLevel;

    public LibraryDependencyData(ModuleData ownerModule, LibraryData library, LibraryLevel level) {
        super(ownerModule, library);
        myLevel = level;
    }

    public LibraryLevel getLevel() {
        return myLevel;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + myLevel.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }
        return o instanceof LibraryDependencyData that
            && super.equals(o)
            && myLevel.equals(that.myLevel);
    }
}
