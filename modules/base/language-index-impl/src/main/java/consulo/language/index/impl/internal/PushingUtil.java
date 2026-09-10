// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.application.ReadAction;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.index.impl.internal.roots.kind.ModuleRootOrigin;
import consulo.module.Module;
import consulo.module.content.FilePropertyPusher;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.project.Project;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static consulo.language.index.impl.internal.PushedFilePropertiesUpdaterImpl.getImmediateValuesEx;
import static consulo.language.index.impl.internal.PushedFilePropertiesUpdaterImpl.getModuleImmediateValues;

final class PushingUtil {
    private final @Nullable List<FilePropertyPusher<?>> myPushers;
    private final @Nullable List<FilePropertyPusherEx<?>> myPusherExs;
    private final Object @Nullable [] myModuleValues;
    private final PushedFilePropertiesUpdater myPushedFilePropertiesUpdater;
    private final boolean myMayBeUsed;

    PushingUtil(Project project, IndexableFilesIterator provider) {

        myPushedFilePropertiesUpdater = PushedFilePropertiesUpdater.getInstance(project);

        // We always need to properly dispose perProviderSink.
        // Make this fact explicit to clients by requiring clients to provide an instance

        IndexableSetOrigin origin = provider.getOrigin();
        if (origin instanceof ModuleRootOrigin moduleRootOrigin && !moduleRootOrigin.getModule().isDisposed()) {
            Module module = moduleRootOrigin.getModule();
            List<FilePropertyPusher<?>> pushers = PushedFilePropertiesUpdaterImpl.getAllPushers(project.getApplication());
            myPushers = pushers;
            myPusherExs = null;
            myModuleValues = ReadAction.compute(() -> {
                if (module.isDisposed()) {
                    return null;
                }
                return getModuleImmediateValues(pushers, module);
            });
            myMayBeUsed = myModuleValues != null;
        }
        else {
            myPushers = null;
            List<FilePropertyPusherEx<?>> extendedPushers = new SmartList<>();
            for (FilePropertyPusher<?> pusher : PushedFilePropertiesUpdaterImpl.getAllPushers(project.getApplication())) {
                if (pusher instanceof FilePropertyPusherEx<?> pusherEx && pusherEx.acceptsOrigin(project, origin)) {
                    extendedPushers.add(pusherEx);
                }
            }
            if (extendedPushers.isEmpty()) {
                myPusherExs = null;
                myModuleValues = null;
            }
            else {
                myPusherExs = extendedPushers;
                myModuleValues = ReadAction.compute(() -> getImmediateValuesEx(extendedPushers, origin));
            }
            myMayBeUsed = true;
        }
    }

    /**
     * Introduced to check if providers are still not obviously broken to the level they should not be used, as they didn't spend their life
     * under read lock
     */
    boolean mayBeUsed() {
        return myMayBeUsed;
    }

    public void applyPushers(VirtualFile fileOrDir) {
        if (myPushers != null && myPushedFilePropertiesUpdater instanceof PushedFilePropertiesUpdaterImpl updater) {
            updater.applyPushersToFile(fileOrDir, myPushers, myModuleValues);
        }
        else if (myPusherExs != null && myPushedFilePropertiesUpdater instanceof PushedFilePropertiesUpdaterImpl updater) {
            updater.applyPushersToFile(fileOrDir, myPusherExs, myModuleValues);
        }
    }
}
