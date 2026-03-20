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
package consulo.project.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.component.internal.ComponentBinding;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectOpenService;
import consulo.project.internal.WelcomeProjectManager;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.concurrent.CompletableFuture;

/**
 * Manages welcome project lifecycle. Unlike {@link DefaultProjectFactoryImpl} which holds a singleton,
 * this manager creates a fresh {@link WelcomeProjectImpl} each time the welcome screen is shown
 * and disposes it when it is closed.
 * <p>
 * State persistence is handled by this manager via {@link PersistentStateComponent},
 * storing welcome project state in {@code project.welcome.xml}.
 * <p>
 * Project opening uses the standard {@link ProjectOpenService} flow with a
 * {@linkplain WelcomeProjectManager#WELCOME_PATH stub path} that is detected
 * by {@link ProjectOpenServiceImpl}.
 *
 * @author VISTALL
 * @since 2026-03-08
 */
@State(name = "WelcomeProjectManager", storages = @Storage("project.welcome.xml"))
@ServiceImpl
@Singleton
public class WelcomeProjectManagerImpl implements WelcomeProjectManager, PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(WelcomeProjectManagerImpl.class);

    private final Application myApplication;
    private final ProjectManager myProjectManager;
    private final ComponentBinding myComponentBinding;

    /**
     * The currently open welcome project, or {@code null} when no welcome project is open.
     * This reference is only held while the project is actively open — it is cleared on close.
     */
    private volatile @Nullable WelcomeProjectImpl myOpenWelcomeProject;

    /**
     * Persisted state element from the welcome project.
     * Survives welcome project dispose — used to initialize the next welcome project instance
     * and persisted to {@code project.welcome.xml} via {@link #getState()}.
     */
    private @Nullable Element myStateElement;

    @Inject
    public WelcomeProjectManagerImpl(Application application,
                                     ProjectManager projectManager,
                                     ComponentBinding componentBinding) {
        myApplication = application;
        myProjectManager = projectManager;
        myComponentBinding = componentBinding;
    }

    @Override
    public CompletableFuture<?> openWelcomeProjectAsync(UIAccess uiAccess) {
        WelcomeProjectImpl current = myOpenWelcomeProject;
        if (current != null && ((ProjectManagerImpl) myProjectManager).isProjectOpened(current)) {
            return CompletableFuture.completedFuture(null);
        }

        // Create a fresh welcome project instance.
        // The standard open flow in ProjectOpenServiceImpl will detect WELCOME_PATH
        // and retrieve this project via getOpenWelcomeProject().
        WelcomeProjectImpl welcomeProject = createWelcomeProject();
        myOpenWelcomeProject = welcomeProject;

        ProjectOpenService openService = myApplication.getInstance(ProjectOpenService.class);
        return openService.openProjectAsync(WELCOME_PATH, uiAccess, new ProjectOpenContext())
            .whenComplete((project, error) -> {
                if (error != null || project == null) {
                    myOpenWelcomeProject = null;
                }
            });
    }

    /**
     * Creates a fresh {@link WelcomeProjectImpl}, restoring persisted state if available.
     * The project is NOT initialized — that happens in the standard open flow
     * ({@code doOpenInProject} calls {@code initNotLazyServices()}).
     */
    WelcomeProjectImpl createWelcomeProject() {
        WelcomeProjectImpl project = new WelcomeProjectImpl(myApplication, myProjectManager, myComponentBinding);

        // Restore persisted state from previous welcome project instance
        if (myStateElement != null) {
            project.setStateElement(myStateElement.clone());
        }

        return project;
    }

    @Override
    public CompletableFuture<Boolean> closeWelcomeProjectAsync(UIAccess uiAccess) {
        WelcomeProjectImpl welcomeProject = myOpenWelcomeProject;
        if (welcomeProject == null || !((ProjectManagerImpl) myProjectManager).isProjectOpened(welcomeProject)) {
            myOpenWelcomeProject = null;
            return CompletableFuture.completedFuture(true);
        }

        myOpenWelcomeProject = null;

        // Close with save=true to persist state before dispose.
        // Don't check canClose veto — welcome project always closes.
        // dispose=false — we handle dispose manually after capturing the state element.
        return myProjectManager.closeProjectAsync(welcomeProject, uiAccess, false, true, false)
            .thenCompose(closed -> {
                if (Boolean.TRUE.equals(closed)) {
                    // Capture state element after save, before dispose
                    Element stateElement = welcomeProject.getStateElement();
                    if (stateElement != null) {
                        myStateElement = stateElement.clone();
                    }

                    // Dispose the project under write action
                    CompletableFuture<Boolean> disposeFuture = new CompletableFuture<>();
                    WriteAction.runLater(() -> {
                        Disposer.dispose(welcomeProject);
                        disposeFuture.complete(Boolean.TRUE);
                    });
                    return disposeFuture;
                }
                return CompletableFuture.completedFuture(closed);
            });
    }

    @Override
    public boolean isWelcomeProjectOpened() {
        WelcomeProjectImpl welcomeProject = myOpenWelcomeProject;
        return welcomeProject != null && ((ProjectManagerImpl) myProjectManager).isProjectOpened(welcomeProject);
    }

    @Override
    public @Nullable Project getOpenWelcomeProject() {
        return myOpenWelcomeProject;
    }

    // region PersistentStateComponent

    @RequiredWriteAction
    @Override
    public @Nullable Element getState() {
        // If welcome project is currently open, save it and capture fresh state
        WelcomeProjectImpl welcomeProject = myOpenWelcomeProject;
        if (welcomeProject != null && !Disposer.isDisposed(welcomeProject)) {
            welcomeProject.save(Application.get().getLastUIAccess());

            Element stateElement = welcomeProject.getStateElement();
            if (stateElement != null) {
                myStateElement = stateElement.clone();
            }
        }

        if (myStateElement == null) {
            return null;
        }

        Element element = new Element("state");
        element.addContent(myStateElement.clone());
        return element;
    }

    @RequiredReadAction
    @Override
    public void loadState(Element state) {
        Element welcomeProjectElement = state.getChild("welcomeProject");
        if (welcomeProjectElement != null) {
            welcomeProjectElement.detach();
            myStateElement = welcomeProjectElement;
        }
    }

    // endregion
}
