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
package consulo.execution.profiler.impl.internal.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.execution.profiler.NewCallTreeOnlyProfilerData;
import consulo.execution.profiler.ProfilerData;
import consulo.execution.profiler.view.ProfilerDataViewProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.layout.TabbedLayout;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ProfilerCaptureViewFactory {
    private final Project myProject;
    private final ApplicationTreeExecutorFactory myTreeExecutorFactory;
    private final ProfilerNavigator myNavigator;

    @Inject
    public ProfilerCaptureViewFactory(Project project, ApplicationTreeExecutorFactory treeExecutorFactory) {
        myProject = project;
        myTreeExecutorFactory = treeExecutorFactory;
        myNavigator = new ProfilerNavigator(project);
    }

    @RequiredUIAccess
    public Component createView(ProfilerData data, Disposable parentDisposable) {
        List<ProfilerViewTab> extraTabs = new ArrayList<>();
        myProject.getExtensionPoint(ProfilerDataViewProvider.class).forEach(provider -> {
            if (provider.isApplicable(data)) {
                extraTabs.add(new ProfilerViewTab(provider.getName(), provider.createView(data, parentDisposable)));
            }
        });

        if (data instanceof NewCallTreeOnlyProfilerData callTreeData) {
            return new CallTreeDataView(callTreeData, myNavigator, myTreeExecutorFactory, extraTabs, parentDisposable).getComponent();
        }

        if (extraTabs.isEmpty()) {
            return ProfilerUIUtil.hint(LocalizeValue.localizeTODO("No installed plugin can show this profiler data"));
        }

        if (extraTabs.size() == 1) {
            return extraTabs.get(0).component();
        }

        TabbedLayout tabs = TabbedLayout.create();
        for (ProfilerViewTab extraTab : extraTabs) {
            ProfilerUIUtil.addTab(tabs, extraTab.name(), extraTab.component());
        }
        return tabs;
    }
}
