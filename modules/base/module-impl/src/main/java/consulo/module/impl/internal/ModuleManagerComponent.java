/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.module.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.container.util.StatCollector;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author yole
 */
@State(name = "ModuleManager", storages = @Storage("modules.xml"))
@Singleton
@ServiceImpl
public class ModuleManagerComponent extends ModuleManagerImpl {
    public static final Logger LOG = Logger.getInstance(ModuleManagerComponent.class);

    @Nonnull
    private final ProgressManager myProgressManager;
    @Nonnull
    private final MessageBusConnection myConnection;
    @Nonnull
    private final ComponentBinding myComponentBinding;

    @Inject
    public ModuleManagerComponent(Project project, @Nonnull ProgressManager progressManager, @Nonnull ComponentBinding componentBinding) {
        super(project);
        myComponentBinding = componentBinding;
        myConnection = myMessageBus.connect(project);
        myProgressManager = progressManager;

        myConnection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void beforeRootsChange(ModuleRootEvent event) {
                cleanCachedStuff();
            }

            @Override
            public void rootsChanged(ModuleRootEvent event) {
                cleanCachedStuff();
            }
        });

        if (project.isDefault()) {
            myReady = true;
        }
    }

    @Nonnull
    @Override
    protected ModuleEx createModule(@Nonnull String name, @Nullable String dirUrl, ProgressIndicator progressIndicator) {
        return new ModuleImpl(name, dirUrl, myProject, myComponentBinding);
    }

    @Nonnull
    public CompletableFuture<?> loadModules(@Nonnull ProgressIndicator indicator) {
        StatCollector stat = new StatCollector();

        stat.markWith("load modules", () -> loadModules(myModuleModel, indicator, true));

        indicator.setIndeterminate(true);

        return AccessRule.writeAsync(() -> {
            stat.markWith("fire modules add", () -> {
                for (Module module : myModuleModel.myModules) {
                    fireModuleAdded(module);
                }
            });

            myReady = true;
            stat.dump("ModulesManager", LOG::info);
        });
    }

    @Override
    protected void fireModulesAdded() {
        Runnable runnableWithProgress = () -> {
            Application app = myProject.getApplication();
            for (final Module module : myModuleModel.getModules()) {
                final Runnable swingRunnable = () -> fireModuleAddedInWriteAction(module);
                if (app.isDispatchThread() || app.isHeadlessEnvironment()) {
                    swingRunnable.run();
                }
                else {
                    ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
                    app.invokeAndWait(swingRunnable, pi.getModalityState());
                }
            }
        };

        ProgressIndicator progressIndicator = myProgressManager.getProgressIndicator();
        if (progressIndicator == null) {
            myProgressManager.runProcessWithProgressSynchronously(runnableWithProgress, "Loading modules", false, myProject);
        }
        else {
            runnableWithProgress.run();
        }
    }

    @Override
    protected void doFirstModulesLoad() {
    }

    @Override
    protected void deliverPendingEvents() {
        myConnection.deliverImmediately();
    }
}
