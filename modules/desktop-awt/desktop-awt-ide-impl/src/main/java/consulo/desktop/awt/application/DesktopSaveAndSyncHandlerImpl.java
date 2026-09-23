/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.application;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.BaseSaveAndSyncHandler;
import consulo.application.Application;
import consulo.application.SaveAndSyncHandler;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.progress.ProgressManager;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposable;
import consulo.document.FileDocumentManager;
import consulo.document.internal.FileDocumentManagerEx;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ModalityState;
import consulo.virtualFileSystem.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class DesktopSaveAndSyncHandlerImpl extends BaseSaveAndSyncHandler {
    private final Runnable myIdleListener;
    private final PropertyChangeListener myGeneralSettingsListener;

    @Inject
    public DesktopSaveAndSyncHandlerImpl(Application application,
                                         GeneralSettings generalSettings,
                                         ProgressManager progressManager,
                                         FileDocumentManager fileDocumentManager) {
        super(application, generalSettings, progressManager, fileDocumentManager);

        myIdleListener = this::saveAllDocumentsIfInactive;
        IdeEventQueue.getInstance().addIdleListener(myIdleListener, mySettings.getInactiveTimeout() * 1000);

        myGeneralSettingsListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if (GeneralSettings.PROP_INACTIVE_TIMEOUT.equals(e.getPropertyName())) {
                    IdeEventQueue eventQueue = IdeEventQueue.getInstance();
                    eventQueue.removeIdleListener(myIdleListener);
                    Integer timeout = (Integer) e.getNewValue();
                    eventQueue.addIdleListener(myIdleListener, timeout.intValue() * 1000);
                }
            }
        };
        mySettings.addPropertyChangeListener(myGeneralSettingsListener);
    }

    @Override
    public void dispose() {
        super.dispose();
        mySettings.removePropertyChangeListener(myGeneralSettingsListener);
        IdeEventQueue.getInstance().removeIdleListener(myIdleListener);
    }
}