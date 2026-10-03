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
package consulo.execution.profiler.impl.internal.session;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.impl.internal.editor.ProfilerCaptureVirtualFile;
import consulo.execution.profiler.impl.internal.editor.ProfilerSessionVirtualFile;
import consulo.execution.profiler.impl.internal.view.ProfilerCaptureViewFactory;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ProfilerSessionManager implements Disposable {
    private static final Logger LOG = Logger.getInstance(ProfilerSessionManager.class);

    private final Project myProject;
    private final Provider<FileEditorManager> myFileEditorManager;
    private final ProfilerCaptureViewFactory myCaptureViewFactory;
    private final ActionToolbarFactory myActionToolbarFactory;
    private final JBPopupFactory myPopupFactory;
    private final List<ProfilerSession> mySessions = new ArrayList<>();
    private final List<ProfilerSessionListener> myListeners = new CopyOnWriteArrayList<>();
    private final ProfilerSessionListener myForwarder = new ProfilerSessionListener() {
        @Override
        @RequiredUIAccess
        public void statusChanged(ProfilerSession session) {
            fire(listener -> listener.statusChanged(session));
        }

        @Override
        @RequiredUIAccess
        public void captureAdded(ProfilerSession session, ProfilerCapture capture) {
            fire(listener -> listener.captureAdded(session, capture));
        }

        @Override
        @RequiredUIAccess
        public void captureRemoved(ProfilerSession session, ProfilerCapture capture) {
            fire(listener -> listener.captureRemoved(session, capture));
        }
    };

    private volatile boolean myDisposed;

    @Inject
    public ProfilerSessionManager(
        Project project,
        Provider<FileEditorManager> fileEditorManager,
        ProfilerCaptureViewFactory captureViewFactory,
        ActionToolbarFactory actionToolbarFactory,
        JBPopupFactory popupFactory
    ) {
        myProject = project;
        myFileEditorManager = fileEditorManager;
        myCaptureViewFactory = captureViewFactory;
        myActionToolbarFactory = actionToolbarFactory;
        myPopupFactory = popupFactory;
    }

    @RequiredUIAccess
    public @Nullable ProfilerSession getOrCreateSession(ProfilerProcess<?> process) {
        if (myDisposed || myProject.isDisposed()) {
            return null;
        }

        for (ProfilerSession session : mySessions) {
            if (session.getProcess() == process) {
                return session;
            }
        }

        ProfilerSession session = new ProfilerSession(myProject, process);
        Disposer.register(this, session);
        Disposer.register(session, session.addListener(myForwarder));
        Disposer.register(session, () -> mySessions.remove(session));
        mySessions.add(session);

        fire(listener -> listener.sessionAdded(session));
        return session;
    }

    @RequiredUIAccess
    public List<ProfilerSession> getSessions() {
        return List.copyOf(mySessions);
    }

    public Disposable addListener(ProfilerSessionListener listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    @RequiredUIAccess
    public ProfilerSessionPanel createPanel(ProfilerSession session, ProfilerSessionLayout layout) {
        return new ProfilerSessionPanel(
            myProject,
            session,
            layout,
            this,
            myCaptureViewFactory,
            myActionToolbarFactory,
            myPopupFactory
        );
    }

    @RequiredUIAccess
    public boolean isSessionEditorOpen(ProfilerSession session) {
        ProfilerSessionVirtualFile file = session.findVirtualFile();
        return file != null && myFileEditorManager.get().isFileOpen(file);
    }

    @RequiredUIAccess
    public void openSessionEditor(ProfilerSession session, boolean focus) {
        if (session.isDisposed() || myProject.isDisposed()) {
            return;
        }
        myFileEditorManager.get().openFile(session.getVirtualFile(), focus, true);
    }

    @RequiredUIAccess
    public void openCaptureEditor(ProfilerCapture capture, boolean focus) {
        ProfilerSession session = capture.getSession();
        if (myProject.isDisposed() || (session != null && session.isDisposed())) {
            return;
        }
        capture.markShown();
        myFileEditorManager.get().openFile(capture.getVirtualFile(), focus, true);
    }

    @RequiredUIAccess
    public void closeSession(ProfilerSession session) {
        if (session.isDisposed() || session.isActive() || !mySessions.remove(session)) {
            return;
        }

        FileEditorManager fileEditorManager = myFileEditorManager.get();
        for (ProfilerCapture capture : session.getCaptures()) {
            ProfilerCaptureVirtualFile captureFile = capture.findVirtualFile();
            if (captureFile != null) {
                closeEditors(fileEditorManager, captureFile);
            }
        }
        ProfilerSessionVirtualFile sessionFile = session.findVirtualFile();
        if (sessionFile != null) {
            closeEditors(fileEditorManager, sessionFile);
        }

        fire(listener -> listener.sessionRemoved(session));
        Disposer.dispose(session);
    }

    @Override
    public void dispose() {
        myDisposed = true;
        myListeners.clear();
    }

    @RequiredUIAccess
    private static void closeEditors(FileEditorManager fileEditorManager, VirtualFile file) {
        for (FileEditorWindow window : fileEditorManager.getWindows()) {
            if (window.isFileOpen(file)) {
                fileEditorManager.closeFile(file, window);
            }
        }
    }

    @RequiredUIAccess
    private void fire(Consumer<ProfilerSessionListener> call) {
        for (ProfilerSessionListener listener : myListeners) {
            try {
                call.accept(listener);
            }
            catch (Throwable e) {
                LOG.error("Profiler session listener failed", e);
            }
        }
    }
}
