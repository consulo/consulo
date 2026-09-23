/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.build;

import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.event.StartBuildEvent;
import consulo.project.Project;
import consulo.util.collection.SmartList;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Which builds a tool window of builds is holding, and which of them a new build takes the place of. A build
 * is shown by a view of its own, so a build which runs again is not cleared - the view it had is dropped and
 * the new one starts empty.
 *
 * @author Vladislav.Soroka
 */
public abstract class BaseMultipleBuildsView implements BuildsView {
    /**
     * What a new build does to the builds already in the window.
     */
    public record ClearDecision(boolean clearAll, List<AbstractViewManager.BuildInfo> buildsToRemove) {
        static ClearDecision nothing() {
            return new ClearDecision(false, List.of());
        }
    }

    protected final Project myProject;
    protected final AbstractViewManager myViewManager;

    protected final Map<Object, AbstractViewManager.BuildInfo> myBuildsMap = new ConcurrentHashMap<>();

    protected BaseMultipleBuildsView(Project project, AbstractViewManager viewManager) {
        myProject = project;
        myViewManager = viewManager;
    }

    @Override
    public boolean shouldConsume(Object buildId) {
        return myBuildsMap.containsKey(buildId);
    }

    @Override
    public Set<BuildDescriptor> getBuildDescriptors() {
        return getBuildsMap().keySet();
    }

    protected abstract Map<BuildDescriptor, ?> getBuildsMap();

    /**
     * A build which is starting takes the place of the ones the window is done with - all of them when none is
     * still running and none finished a moment ago, and otherwise only the earlier runs of the same build.
     */
    protected ClearDecision decideClearOldBuilds(StartBuildEvent startBuildEvent, List<AbstractViewManager.BuildInfo> builds) {
        if (builds.isEmpty()) {
            return ClearDecision.nothing();
        }

        long currentTime = System.currentTimeMillis();
        boolean clearAll = true;
        List<AbstractViewManager.BuildInfo> sameBuildsToClear = new SmartList<>();

        for (AbstractViewManager.BuildInfo build : builds) {
            boolean sameBuild = build.getWorkingDir().equals(startBuildEvent.getBuildDescriptor().getWorkingDir());
            if (!build.isRunning() && sameBuild) {
                sameBuildsToClear.add(build);
            }
            boolean buildFinishedRecently = currentTime - build.endTime < TimeUnit.SECONDS.toMillis(1);
            if (build.isRunning() || !sameBuild && buildFinishedRecently) {
                clearAll = false;
            }
        }

        return clearAll ? new ClearDecision(true, List.copyOf(builds)) : new ClearDecision(false, sameBuildsToClear);
    }
}
