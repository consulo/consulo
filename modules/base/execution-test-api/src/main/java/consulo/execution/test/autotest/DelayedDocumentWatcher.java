/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.test.autotest;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.codeEditor.EditorFactory;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.event.FileDocumentManagerListener;
import consulo.language.editor.PsiErrorElementUtil;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DelayedDocumentWatcher implements AutoTestWatcher {
    // All instance fields are be accessed from EDT
    private final Project myProject;
    private final Alarm myAlarm;
    private final int myDelayMillis;
    private final Consumer<Integer> myModificationStampConsumer;
    private final Condition<VirtualFile> myChangedFileFilter;
    private final MyDocumentAdapter myListener;
    private final Runnable myAlarmRunnable;

    private final Set<VirtualFile> myChangedFiles = new HashSet<>();
    private boolean myDocumentSavingInProgress = false;
    private MessageBusConnection myConnection;
    private int myModificationStamp = 0;
    private Disposable myListenerDisposable;

    public DelayedDocumentWatcher(
        @Nonnull Project project,
        int delayMillis,
        @Nonnull Consumer<Integer> modificationStampConsumer,
        @Nullable Condition<VirtualFile> changedFileFilter
    ) {
        myProject = project;
        myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
        myDelayMillis = delayMillis;
        myModificationStampConsumer = modificationStampConsumer;
        myChangedFileFilter = changedFileFilter;
        myListener = new MyDocumentAdapter();
        myAlarmRunnable = new MyRunnable();
    }

    @Nonnull
    public Project getProject() {
        return myProject;
    }

    public void activate() {
        if (myConnection == null) {
            myListenerDisposable = Disposable.newDisposable();
            Disposer.register(myProject, myListenerDisposable);
            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(myListener, myListenerDisposable);
            myConnection = ApplicationManager.getApplication().getMessageBus().connect(myProject);
            myConnection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
                @Override
                public void beforeAllDocumentsSaving() {
                    myDocumentSavingInProgress = true;
                    ApplicationManager.getApplication()
                        .invokeLater(() -> myDocumentSavingInProgress = false, Application.get().getAnyModalityState());
                }
            });
        }
    }

    public void deactivate() {
        if (myConnection != null) {
            if (myListenerDisposable != null) {
                Disposer.dispose(myListenerDisposable);
                myListenerDisposable = null;
            }
            myConnection.disconnect();
            myConnection = null;
        }
    }

    public boolean isUpToDate(int modificationStamp) {
        return myModificationStamp == modificationStamp;
    }

    private class MyDocumentAdapter extends DocumentAdapter {
        @Override
        public void documentChanged(DocumentEvent event) {
            if (myDocumentSavingInProgress) {
        /* When {@link FileDocumentManager#saveAllDocuments} is called,
           {@link consulo.ide.impl.idea.openapi.editor.impl.TrailingSpacesStripper} can change a document.
           These needless 'documentChanged' events should be filtered out.
         */
                return;
            }
            final Document document = event.getDocument();
            final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file == null) {
                return;
            }
            if (!myChangedFiles.contains(file)) {
                if (ProjectCoreUtil.isProjectOrWorkspaceFile(file)) {
                    return;
                }
                if (myChangedFileFilter != null && !myChangedFileFilter.value(file)) {
                    return;
                }

                myChangedFiles.add(file);
            }

            myAlarm.cancelRequest(myAlarmRunnable);
            myAlarm.addRequest(myAlarmRunnable, myDelayMillis);
            myModificationStamp++;
        }
    }

    private class MyRunnable implements Runnable {
        @Override
        public void run() {
            final int oldModificationStamp = myModificationStamp;
            asyncCheckErrors(myChangedFiles, errorsFound -> {
                if (myModificationStamp != oldModificationStamp) {
                    // 'documentChanged' event was raised during async checking files for errors
                    // Do nothing in that case, this method will be invoked subsequently.
                    return;
                }
                if (errorsFound) {
                    // Do nothing, if some changed file has syntax errors.
                    // This method will be invoked subsequently, when syntax errors are fixed.
                    return;
                }
                myChangedFiles.clear();
                myModificationStampConsumer.accept(myModificationStamp);
            });
        }
    }

    private void asyncCheckErrors(@Nonnull final Collection<VirtualFile> files, @Nonnull final Consumer<Boolean> errorsFoundConsumer) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final boolean errorsFound = ApplicationManager.getApplication().runReadAction(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    for (VirtualFile file : files) {
                        if (PsiErrorElementUtil.hasErrors(myProject, file)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            ApplicationManager.getApplication()
                .invokeLater(() -> errorsFoundConsumer.accept(errorsFound), Application.get().getAnyModalityState());
        });
    }
}
