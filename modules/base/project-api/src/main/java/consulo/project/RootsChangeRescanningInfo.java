// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project;

public interface RootsChangeRescanningInfo {

    RootsChangeRescanningInfo TOTAL_RESCAN = new RootsChangeRescanningInfo() {
        @Override
        public String toString() {
            return "RootsChangeRescanningInfo.TOTAL_RESCAN";
        }
    };

    /**
     * This value is designed to be used to index changes from {@code AdditionalLibraryRootsProvider},
     * {@link consulo.language.psi.stub.IndexableSetContributor} or {@link consulo.module.content.layer.DirectoryIndexExcludePolicy}
     */
    RootsChangeRescanningInfo RESCAN_DEPENDENCIES_IF_NEEDED = new RootsChangeRescanningInfo() {
        @Override
        public String toString() {
            return "RootsChangeRescanningInfo.RESCAN_DEPENDENCIES_IF_NEEDED";
        }
    };

    RootsChangeRescanningInfo NO_RESCAN_NEEDED = new RootsChangeRescanningInfo() {
        @Override
        public String toString() {
            return "RootsChangeRescanningInfo.NO_RESCAN_NEEDED";
        }
    };
}
