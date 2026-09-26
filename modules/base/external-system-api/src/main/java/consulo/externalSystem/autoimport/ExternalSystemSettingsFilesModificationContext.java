// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

public interface ExternalSystemSettingsFilesModificationContext {
    /**
     * Aggregated event for file.
     * <p>
     * For example, events CREATE and UPDATE will be merged into CREATE event, UPDATE and DELETE into DELETE.
     */
    Event getEvent();

    /**
     * Aggregated modification type for all changed files.
     */
    ExternalSystemModificationType getModificationType();

    /**
     * Current reload status is same for all settings files at one time.
     */
    ReloadStatus getReloadStatus();

    enum Event {
        CREATE,
        UPDATE,
        DELETE
    }

    enum ReloadStatus {
        IDLE,
        IN_PROGRESS,
        JUST_STARTED,
        JUST_FINISHED
    }
}
