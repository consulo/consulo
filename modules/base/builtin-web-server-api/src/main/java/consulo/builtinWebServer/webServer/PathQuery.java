// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.webServer;

import org.jspecify.annotations.Nullable;

public final class PathQuery {
    private final boolean mySearchInLibs;
    private final boolean mySearchInArtifacts;
    private final boolean myUseHtaccess;
    private final boolean myUseVfs;

    public PathQuery(boolean searchInLibs, boolean searchInArtifacts, boolean useHtaccess, boolean useVfs) {
        mySearchInLibs = searchInLibs;
        mySearchInArtifacts = searchInArtifacts;
        myUseHtaccess = useHtaccess;
        myUseVfs = useVfs;
    }

    public PathQuery() {
        this(true, true, true, false);
    }

    public boolean isSearchInLibs() {
        return mySearchInLibs;
    }

    public boolean isSearchInArtifacts() {
        return mySearchInArtifacts;
    }

    public boolean isUseHtaccess() {
        return myUseHtaccess;
    }

    public boolean isUseVfs() {
        return myUseVfs;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PathQuery that)) {
            return false;
        }

        return mySearchInLibs == that.mySearchInLibs &&
            mySearchInArtifacts == that.mySearchInArtifacts &&
            myUseHtaccess == that.myUseHtaccess &&
            myUseVfs == that.myUseVfs;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(mySearchInLibs);
        result = 31 * result + Boolean.hashCode(mySearchInArtifacts);
        result = 31 * result + Boolean.hashCode(myUseHtaccess);
        result = 31 * result + Boolean.hashCode(myUseVfs);
        return result;
    }

    @Override
    public String toString() {
        return "PathQuery(searchInLibs=" + mySearchInLibs +
            ", searchInArtifacts=" + mySearchInArtifacts +
            ", useHtaccess=" + myUseHtaccess +
            ", useVfs=" + myUseVfs + ")";
    }
}
