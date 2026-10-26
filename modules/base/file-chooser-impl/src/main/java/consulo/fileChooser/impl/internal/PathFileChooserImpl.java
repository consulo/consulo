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
package consulo.fileChooser.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.internal.PathFileChooser;
import consulo.fileChooser.internal.PlatformFileChooserContributor;
import consulo.fileChooser.PlatformFileChooserContributorProvider;
import consulo.fileChooser.UniversalFileChooserContributor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The universal chooser over paths: one machine when the descriptor names a remote platform, or the local tabs plus
 * the {@link PlatformFileChooserContributorProvider platform tabs} when it allows any platform.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ServiceImpl
@Singleton
public class PathFileChooserImpl implements PathFileChooser {
    private final Application myApplication;
    private final ApplicationTreeExecutorFactory myTreeExecutorFactory;
    private final ProjectManager myProjectManager;

    @Inject
    public PathFileChooserImpl(Application application,
                               ApplicationTreeExecutorFactory treeExecutorFactory,
                               ProjectManager projectManager) {
        myApplication = application;
        myTreeExecutorFactory = treeExecutorFactory;
        myProjectManager = projectManager;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<List<Path>> choose(FileChooserDescriptor descriptor, @Nullable ComponentManager project, @Nullable Path toSelect) {
        Project targetProject = project instanceof Project resolved ? resolved : myProjectManager.getDefaultProject();

        List<UniversalFileChooserContributor> contributors = new ArrayList<>();
        Path preselect = toSelect;
        if (descriptor.isAnyPlatform()) {
            contributors.addAll(myApplication.getExtensionPoint(UniversalFileChooserContributor.class).getExtensionList());
            myApplication.getExtensionPoint(PlatformFileChooserContributorProvider.class)
                .forEach(provider -> contributors.addAll(provider.getContributors()));
        }
        else {
            Platform platform = descriptor.getPlatform();
            contributors.add(new PlatformFileChooserContributor(
                LocalizeValue.of(platform.getName()),
                null,
                () -> platform,
                () -> CompletableFuture.completedFuture(platform)
            ));
            if (preselect == null) {
                preselect = platform.user().homePath();
            }
        }

        UniversalFileChooser chooser = new UniversalFileChooser(descriptor, targetProject, myTreeExecutorFactory);
        return chooser.choosePaths(targetProject, preselect, contributors);
    }
}
