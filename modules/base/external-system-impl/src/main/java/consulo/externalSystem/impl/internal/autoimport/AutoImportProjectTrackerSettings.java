// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.disposer.util.DisposableList;
import consulo.externalSystem.autoimport.DefaultAutoReloadTypeProvider;
import consulo.externalSystem.autoimport.ExternalSystemProjectTrackerSettings;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceImpl
@State(name = "AutoImportSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class AutoImportProjectTrackerSettings
    implements ExternalSystemProjectTrackerSettings, PersistentStateComponent<AutoImportProjectTrackerSettingsState> {

    private final Application myApplication;

    private final DisposableList<Runnable> myAutoReloadTypeListeners = DisposableList.create();

    private volatile AutoReloadType myAutoReloadType;

    @Inject
    public AutoImportProjectTrackerSettings(Application application) {
        myApplication = application;
        myAutoReloadType = getDefaultAutoReloadType();
    }

    public static AutoImportProjectTrackerSettings getInstance(Project project) {
        return (AutoImportProjectTrackerSettings) ExternalSystemProjectTrackerSettings.getInstance(project);
    }

    public void addAutoReloadTypeListener(Disposable parentDisposable, Runnable listener) {
        myAutoReloadTypeListeners.add(listener, parentDisposable);
    }

    @Override
    public AutoReloadType getAutoReloadType() {
        return myAutoReloadType;
    }

    @Override
    public void setAutoReloadType(AutoReloadType autoReloadType) {
        myAutoReloadType = autoReloadType;
        for (Runnable listener : myAutoReloadTypeListeners) {
            listener.run();
        }
    }

    @Override
    public AutoImportProjectTrackerSettingsState getState() {
        AutoImportProjectTrackerSettingsState state = new AutoImportProjectTrackerSettingsState();
        state.autoReloadType = getAutoReloadType().name();
        return state;
    }

    @Override
    public void loadState(AutoImportProjectTrackerSettingsState state) {
        AutoReloadType autoReloadType = parseAutoReloadType(state.autoReloadType);
        setAutoReloadType(autoReloadType == null ? getDefaultAutoReloadType() : autoReloadType);
    }

    private static @Nullable AutoReloadType parseAutoReloadType(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return AutoReloadType.valueOf(value);
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private AutoReloadType getDefaultAutoReloadType() {
        AutoReloadType autoReloadType = myApplication.getExtensionPoint(DefaultAutoReloadTypeProvider.class)
            .computeSafeIfAny(DefaultAutoReloadTypeProvider::getAutoReloadType);
        return autoReloadType == null ? AutoReloadType.SELECTIVE : autoReloadType;
    }
}
