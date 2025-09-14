/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.DateFormatUtil;
import consulo.component.messagebus.MessageBusConnection;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.internal.EditorNotificationBuilderEx;
import consulo.fileEditor.internal.EditorNotificationBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.ClientProperty;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.function.TripleFunction;
import consulo.versionControlSystem.CachingCommittedChangesProvider;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.commited.CommittedChangesAdapter;
import consulo.versionControlSystem.change.commited.CommittedChangesListener;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.util.List;

/**
 * @author yole
 * todo: use EditorNotifications
 */
@Singleton
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public class OutdatedVersionNotifier {
    private static final Logger LOG = Logger.getInstance(OutdatedVersionNotifier.class);

    private static final Key<EditorNotificationBuilderEx> NOTIFICATION_BUILDER = Key.create(EditorNotificationBuilderEx.class);
    private static final Key<JComponent> PANEL_KEY = Key.create("OutdatedRevisionPanel");

    private final Provider<FileEditorManager> myFileEditorManager;

    private final CommittedChangesCache myCache;

    private final Project myProject;

    private volatile boolean myIncomingChangesRequested;

    @Inject
    public OutdatedVersionNotifier(Provider<FileEditorManager> fileEditorManager,
                                   CommittedChangesCache cache,
                                   Project project) {
        myFileEditorManager = fileEditorManager;
        myCache = cache;
        myProject = project;
        MessageBusConnection busConnection = project.getMessageBus().connect();
        busConnection.subscribe(CommittedChangesListener.class, new CommittedChangesAdapter() {
            @Override
            public void incomingChangesUpdated(@Nullable List<CommittedChangeList> receivedChanges) {
                if (myCache.getCachedIncomingChanges() == null) {
                    requestLoadIncomingChanges();
                }
                else {
                    updateAllEditorsLater();
                }
            }

            @Override
            public void changesCleared() {
                updateAllEditorsLater();
            }
        });
        busConnection.subscribe(FileEditorManagerListener.class, new MyFileEditorManagerListener());
    }

    private void requestLoadIncomingChanges() {
        debug("Requesting load of incoming changes");
        if (!myIncomingChangesRequested) {
            myIncomingChangesRequested = true;
            myCache.loadIncomingChangesAsync(committedChangeLists -> {
                myIncomingChangesRequested = false;
                updateAllEditorsLater();
            }, true);
        }
    }

    private static void debug(@NonNls String message) {
        LOG.debug(message);
    }

    private void updateAllEditorsLater() {
        debug("Queueing update of editors");
        ApplicationManager.getApplication().invokeLater(this::updateAllEditors, myProject.getDisposed());
    }

    private void updateAllEditors() {
        if (myCache.getCachedIncomingChanges() == null) {
            requestLoadIncomingChanges();
            return;
        }
        debug("Updating editors");
        VirtualFile[] files = myFileEditorManager.get().getOpenFiles();
        for (VirtualFile file : files) {
            Pair<CommittedChangeList, Change> pair = myCache.getIncomingChangeList(file);
            FileEditor[] fileEditors = myFileEditorManager.get().getEditors(file);
            for (FileEditor editor : fileEditors) {
                JComponent oldPanel = editor.getUserData(PANEL_KEY);
                if (pair != null) {
                    if (oldPanel != null) {
                        EditorNotificationBuilderEx builderEx = ClientProperty.get(oldPanel, NOTIFICATION_BUILDER);
                        if (builderEx != null) {
                            builderEx.withText(updateLabelText(pair.getFirst(), pair.getSecond()));
                        }
                    }
                    else {
                        initPanel(pair.first, pair.second, editor);
                    }
                }
                else if (oldPanel != null) {
                    myFileEditorManager.get().removeTopComponent(editor, oldPanel);
                    editor.putUserData(PANEL_KEY, null);
                }
            }
        }
    }

    private void initPanel(CommittedChangeList list, Change c, FileEditor editor) {
        if (!isIncomingChangesSupported(list)) {
            return;
        }

        JComponent component = createOutdatedRevisionPanel(list, c);

        editor.putUserData(PANEL_KEY, component);

        myFileEditorManager.get().addTopComponent(editor, component);
    }

    private class MyFileEditorManagerListener implements FileEditorManagerListener {
        @Override
        public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
            if (myCache.getCachedIncomingChanges() == null) {
                requestLoadIncomingChanges();
            }
            else {
                Pair<CommittedChangeList, Change> pair = myCache.getIncomingChangeList(file);
                if (pair != null) {
                    FileEditor[] fileEditors = source.getEditors(file);
                    for (FileEditor editor : fileEditors) {
                        initPanel(pair.first, pair.second, editor);
                    }
                }
            }
        }
    }

    private JComponent createOutdatedRevisionPanel(CommittedChangeList changeList, Change c) {
        EditorNotificationBuilderEx builder =
            (EditorNotificationBuilderEx) myProject.getApplication().getInstance(EditorNotificationBuilderFactory.class).newBuilder();

        builder.withAction(VcsLocalize.outdatedVersionShowDiffAction(), "Compare.LastVersion");
        builder.withAction(VcsLocalize.outdatedVersionUpdateProjectAction(), "Vcs.UpdateProject");

        JComponent component = builder.getComponent();

        ClientProperty.put(component, NOTIFICATION_BUILDER, builder);

        builder.withText(updateLabelText(changeList, c));

        return component;
    }

    private static LocalizeValue updateLabelText(CommittedChangeList committedChangeList, Change c) {
        String comment = committedChangeList.getComment();
        int pos = comment.indexOf("\n");
        if (pos >= 0) {
            comment = comment.substring(0, pos).trim() + "...";
        }
        String formattedDate = DateFormatUtil.formatPrettyDateTime(committedChangeList.getCommitDate());
        boolean dateIsPretty = !formattedDate.contains("/");

        TripleFunction<Object, Object, Object, LocalizeValue> func;
        if (c.getType() == Change.Type.DELETED) {
            func = VcsLocalize::outdatedVersionTextDeleted;
        }
        else {
            func = dateIsPretty ? VcsLocalize::outdatedVersionPrettyDateText : VcsLocalize::outdatedVersionText;
        }

        return func.fun(committedChangeList.getCommitterName(), formattedDate, comment);
    }

    private static boolean isIncomingChangesSupported(@Nonnull CommittedChangeList list) {
        CachingCommittedChangesProvider provider = list.getVcs().getCachingCommittedChangesProvider();
        return provider != null && provider.supportsIncomingChanges();
    }
}
