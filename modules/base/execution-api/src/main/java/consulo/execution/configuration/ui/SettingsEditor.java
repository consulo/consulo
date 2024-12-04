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

import consulo.annotation.DeprecationInfo;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.configuration.ui.event.SettingsEditorListener;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UserActivityWatcher;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.Lists;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * This class presents an abstraction of user interface transactional editor provider of some abstract data type.
 * {@link #getComponent()} should be called before {@link #resetFrom(Object)}
 */
public abstract class SettingsEditor<Settings> implements Disposable {
    private final List<SettingsEditorListener<Settings>> myListeners = Lists.newLockFreeCopyOnWriteList();
    private UserActivityWatcher myWatcher;
    private boolean myIsInUpdate = false;
    private final Supplier<Settings> mySettingsFactory;
    private CompositeSettingsEditor<Settings> myOwner;
    private JComponent myEditorComponent;

    protected abstract void resetEditorFrom(Settings s);

    protected abstract void applyEditorTo(Settings s) throws ConfigurationException;

    @Nonnull
    @Deprecated
    @DeprecationInfo(value = "Implement interface via overriding 'createUIComponent()' method")
    protected JComponent createEditor() {
        Component uiComponent = createUIComponent();
        if (uiComponent != null) {
            return (JComponent) TargetAWT.to(uiComponent);
        }

        throw new AbstractMethodError("please implement 'createEditor()' or 'createUIComponent()'");
    }

    @Nullable
    @RequiredUIAccess
    protected Component createUIComponent() {
        return null;
    }

    protected void disposeEditor() {
    }

    public SettingsEditor() {
        this(null);
    }

    public SettingsEditor(Supplier<Settings> settingsFactory) {
        mySettingsFactory = settingsFactory;
        Disposer.register(this, () -> {
            disposeEditor();
            uninstallWatcher();
        });
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

    public final JComponent getComponent() {
        if (myEditorComponent == null) {
            myEditorComponent = createEditor();
            installWatcher(myEditorComponent);
        }
        return myEditorComponent;
    }

    @Override
    public final void dispose() {
    }

    protected void uninstallWatcher() {
        myWatcher = null;
    }

    protected void installWatcher(JComponent c) {
        myWatcher = new UserActivityWatcher();
        myWatcher.register(c);

        myWatcher.addUserActivityListener(this::fireEditorStateChanged, this);
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