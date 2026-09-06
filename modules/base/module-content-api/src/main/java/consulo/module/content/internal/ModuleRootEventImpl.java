// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.module.content.internal;

import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.project.Project;
import consulo.project.RootsChangeRescanningInfo;

import java.util.Collections;
import java.util.List;

/**
 * This is an internal class, use {@link ProjectRootManagerEx#makeRootsChange(Runnable, RootsChangeRescanningInfo)}
 * to fire {@code rootsChanged} event.
 */
public class ModuleRootEventImpl extends ModuleRootEvent {
    private final boolean myFiletypes;
    private final List<? extends RootsChangeRescanningInfo> myInfos;

    public ModuleRootEventImpl(Project project, boolean filetypes) {
        this(project, filetypes, Collections.singletonList(RootsChangeRescanningInfo.TOTAL_RESCAN));
    }

    public ModuleRootEventImpl(Project project, boolean filetypes, List<? extends RootsChangeRescanningInfo> indexingInfos) {
        super(project);
        myFiletypes = filetypes;
        myInfos = indexingInfos;
    }

    @Override
    public boolean isCausedByFileTypesChange() {
        return myFiletypes;
    }

    /**
     * Always `Collections.singletonList(RootsChangeRescanningInfo.TOTAL_REINDEX)` for beforeRootsChangedEvent;
     * provided meaningfully only for rootsChangedEvent.
     * Full reindex is detected by having {@link RootsChangeRescanningInfo#TOTAL_RESCAN} in list
     */
    public List<? extends RootsChangeRescanningInfo> getInfos() {
        return myInfos;
    }
}
