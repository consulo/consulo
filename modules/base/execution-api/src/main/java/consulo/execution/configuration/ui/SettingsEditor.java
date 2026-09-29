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
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.configuration.ui.event.SettingsEditorListener;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * This class presents an abstraction of user interface transactional editor provider of some abstract data type.
 * {@link #getUIComponent()} should be called before {@link #resetFrom(Object)}
 */
public abstract class SettingsEditor<Settings> implements Disposable {
    private final List<SettingsEditorListener<Settings>> myListeners = Lists.newLockFreeCopyOnWriteList();
    private boolean myIsInUpdate = false;
    private final Supplier<Settings> mySettingsFactory;
    private CompositeSettingsEditor<Settings> myOwner;
    private @Nullable Component myUIComponent;

    protected abstract void resetEditorFrom(Settings s);

    protected abstract void applyEditorTo(Settings s) throws ConfigurationException;

    @RequiredUIAccess
    protected abstract Component createUIComponent();

    @RequiredUIAccess
    public final Component getUIComponent() {
        Component component = myUIComponent;
        if (component == null) {
            component = createUIComponent();
            myUIComponent = component;
        }
        return component;
    }

    protected void disposeEditor() {
    }

    public SettingsEditor() {
        this(null);
    }

    public SettingsEditor(Supplier<Settings> settingsFactory) {
        mySettingsFactory = settingsFactory;
        Disposer.register(this, this::disposeEditor);
    }

    public Settings getSnapshot() throws ConfigurationException {
        if (myOwner != null) {
            return myOwner.getSnapshot();
        }

        Settings settings = mySettingsFactory.get();
        applyTo(settings);
        return settings;
    }

    final void setOwner(CompositeSettingsEditor<Settings> owner) {
        myOwner = owner;
    }

    public final CompositeSettingsEditor<Settings> getOwner() {
        return myOwner;
    }

    public Supplier<Settings> getFactory() {
        return mySettingsFactory;
    }

    public final void resetFrom(Settings s) {
        myIsInUpdate = true;
        try {
            resetEditorFrom(s);
        }
        finally {
            myIsInUpdate = false;
        }
    }

    public final void bulkUpdate(Runnable runnable) {
        boolean wasInUpdate = myIsInUpdate;
        try {
            myIsInUpdate = true;
            runnable.run();
        }
        finally {
            myIsInUpdate = wasInUpdate;
        }
        fireEditorStateChanged();
    }

    public final void applyTo(Settings s) throws ConfigurationException {
        applyEditorTo(s);
    }

    @Override
    public final void dispose() {
    }

    public final void addSettingsEditorListener(SettingsEditorListener<Settings> listener) {
        myListeners.add(listener);
    }

    public final void removeSettingsEditorListener(SettingsEditorListener<Settings> listener) {
        myListeners.remove(listener);
    }

    @SuppressWarnings("unchecked")
    protected final void fireEditorStateChanged() {
        if (myIsInUpdate || myListeners == null) {
            return;
        }
        for (SettingsEditorListener listener : myListeners) {
            listener.stateChanged(this);
        }
    }
}
