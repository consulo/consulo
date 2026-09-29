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
package consulo.execution.configuration.ui;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.execution.configuration.ui.event.SettingsEditorListener;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class CompositeSettingsEditor<Settings> extends SettingsEditor<Settings> {
    private Collection<SettingsEditor<Settings>> myEditors = List.of();
    private @Nullable SettingsEditorListener<Settings> myChildSettingsListener;
    private @Nullable SynchronizationController mySyncController;
    private boolean myIsDisposed = false;

    public CompositeSettingsEditor() {
    }

    public CompositeSettingsEditor(Supplier<Settings> factory) {
        super(factory);
        if (factory != null) {
            mySyncController = new SynchronizationController();
        }
    }

    public abstract CompositeSettingsBuilder<Settings> getBuilder();

    @Override
    public void resetEditorFrom(Settings settings) {
        for (SettingsEditor<Settings> myEditor : myEditors) {
            myEditor.resetEditorFrom(settings);
        }
    }

    @Override
    public void applyEditorTo(Settings settings) throws ConfigurationException {
        for (SettingsEditor<Settings> myEditor : myEditors) {
            myEditor.applyTo(settings);
        }
    }

    @RequiredUIAccess
    @Override
    protected final Component createUIComponent() {
        CompositeSettingsBuilder<Settings> builder = getBuilder();
        myEditors = builder.getEditors();
        for (SettingsEditor<Settings> editor : myEditors) {
            Disposer.register(this, editor);
            editor.setOwner(this);
        }

        SettingsEditorListener<Settings> childSettingsListener = editor -> {
            fireEditorStateChanged();
            SynchronizationController syncController = mySyncController;
            if (syncController != null) {
                syncController.handleStateChange(editor);
            }
        };
        myChildSettingsListener = childSettingsListener;
        for (SettingsEditor<Settings> editor : myEditors) {
            editor.addSettingsEditorListener(childSettingsListener);
        }

        return builder.createCompoundEditor(this);
    }

    @Override
    public void disposeEditor() {
        SettingsEditorListener<Settings> childSettingsListener = myChildSettingsListener;
        if (childSettingsListener != null) {
            for (SettingsEditor<Settings> editor : myEditors) {
                editor.removeSettingsEditorListener(childSettingsListener);
            }
        }

        SynchronizationController syncController = mySyncController;
        if (syncController != null) {
            syncController.cancel();
        }

        Disposer.dispose(this);
        myIsDisposed = true;
    }

    private class SynchronizationController {
        private final Set<SettingsEditor<Settings>> myChangedEditors = new HashSet<>();
        private @Nullable Future<?> mySyncFuture;
        private boolean myIsInSync = false;

        @RequiredUIAccess
        public void handleStateChange(SettingsEditor<Settings> editor) {
            if (myIsInSync || myIsDisposed) {
                return;
            }
            myChangedEditors.add(editor);
            cancel();
            mySyncFuture = UIAccess.current().getScheduler().schedule(() -> {
                if (!myIsDisposed) {
                    sync();
                }
            }, 300, TimeUnit.MILLISECONDS);
        }

        public void cancel() {
            Future<?> syncFuture = mySyncFuture;
            if (syncFuture != null) {
                syncFuture.cancel(false);
            }
            mySyncFuture = null;
        }

        public void sync() {
            myIsInSync = true;
            try {
                Settings snapshot = getSnapshot();
                for (SettingsEditor<Settings> editor : myEditors) {
                    if (!myChangedEditors.contains(editor)) {
                        editor.resetFrom(snapshot);
                    }
                }
            }
            catch (ConfigurationException ignored) {
            }
            finally {
                myChangedEditors.clear();
                myIsInSync = false;
            }
        }
    }
}
