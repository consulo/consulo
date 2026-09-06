// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.util.TextWithMnemonic;
import consulo.virtualFileSystem.event.BatchFileChangeListener;
import org.jspecify.annotations.Nullable;

import java.util.Stack;

public final class DumbServiceVfsBatchListener {

    public DumbServiceVfsBatchListener(Application application, Project targetProject, MergingQueueGuiSuspender heavyActivities) {
        //noinspection UseOfObsoleteCollectionType
        application.getMessageBus().connect(targetProject)
            .subscribe(BatchFileChangeListener.class, new BatchFileChangeListener() {
                // synchronized, can be accessed from different threads
                @SuppressWarnings("UnnecessaryFullyQualifiedName")
                final java.util.Stack<AccessToken> stack = new Stack<>();

                @Override
                public void batchChangeStarted(ComponentManager project, @Nullable String activityName) {
                    if (project == targetProject) {
                        LocalizeValue heavyActivityName = activityName != null
                            ? LocalizeValue.of(TextWithMnemonic.parse(activityName).getText())
                            : ProjectLocalize.progressFileSystemChanges();
                        stack.push(heavyActivities.heavyActivityStarted(heavyActivityName));
                    }
                }

                @Override
                public void batchChangeCompleted(ComponentManager project) {
                    if (project != targetProject) {
                        return;
                    }

                    //noinspection UseOfObsoleteCollectionType
                    Stack<AccessToken> tokens = stack;
                    if (!tokens.isEmpty()) { // just in case
                        tokens.pop().finish();
                    }
                }
            });
    }
}
