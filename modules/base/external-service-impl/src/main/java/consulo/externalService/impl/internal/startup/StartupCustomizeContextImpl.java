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
package consulo.externalService.impl.internal.startup;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeFinishTask;
import consulo.localize.LocalizeValue;
import consulo.util.dataholder.UserDataHolderBase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class StartupCustomizeContextImpl extends UserDataHolderBase implements StartupCustomizeContext {
    private final boolean myFirstShow;

    private List<PluginDescriptor> myRepositoryPlugins = List.of();
    private final Set<PluginId> myPluginsToInstall = new LinkedHashSet<>();
    private final List<StartupCustomizeFinishTask> myFinishTasks = new ArrayList<>();
    private final List<LocalizeValue> myProblems = new CopyOnWriteArrayList<>();

    public StartupCustomizeContextImpl(boolean firstShow) {
        myFirstShow = firstShow;
    }

    @Override
    public boolean isFirstShow() {
        return myFirstShow;
    }

    @Override
    public List<PluginDescriptor> getRepositoryPlugins() {
        return myRepositoryPlugins;
    }

    public void setRepositoryPlugins(List<PluginDescriptor> repositoryPlugins) {
        myRepositoryPlugins = List.copyOf(repositoryPlugins);
    }

    @Override
    public Set<PluginId> getPluginsToInstall() {
        return myPluginsToInstall;
    }

    @Override
    public void addFinishTask(StartupCustomizeFinishTask task) {
        myFinishTasks.add(task);
    }

    public List<StartupCustomizeFinishTask> getFinishTasks() {
        return myFinishTasks;
    }

    @Override
    public void addProblem(LocalizeValue problem) {
        myProblems.add(problem);
    }

    public List<LocalizeValue> getProblems() {
        return myProblems;
    }
}
