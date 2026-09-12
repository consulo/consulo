/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.document.Document;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.builder.RunnableCommandBuilder;
import consulo.undoRedo.event.CommandListener;
import consulo.undoRedo.internal.builder.BaseExecutableCommandBuilder;
import consulo.undoRedo.internal.builder.WrappableRunnableCommandBuilder;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * The production {@code CommandProcessorImpl} lives in {@code consulo.ide.impl} which is not part of the headless
 * application. There is no undo stack headlessly, so a command carries no state: the builder executes its runnable
 * inline (with {@code inWriteAction()} etc. honored through the standard wrappers) — VFS-driven flows like
 * {@code FileDocumentManagerImpl.reloadFromDisk} run their commands like any other code.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessCommandProcessor extends CommandProcessor {
    private final Application myApplication;

    @Inject
    public HeadlessCommandProcessor(Application application) {
        myApplication = application;
    }

    private final ThreadLocal<Integer> myCommandDepth = ThreadLocal.withInitial(() -> 0);

    private class HeadlessCommandBuilder<R, THIS extends HeadlessCommandBuilder<R, THIS>>
        extends BaseExecutableCommandBuilder<R, THIS> implements WrappableRunnableCommandBuilder<R, THIS> {
        @Override
        public CommandProcessor getCommandProcessor() {
            return HeadlessCommandProcessor.this;
        }

        @Override
        public Application getApplication() {
            return myApplication;
        }

        @Override
        public ExecutionResult<R> execute(ThrowableSupplier<R, ? extends Throwable> executable) {
            myCommandDepth.set(myCommandDepth.get() + 1);
            try {
                return super.execute(executable);
            }
            finally {
                myCommandDepth.set(myCommandDepth.get() - 1);
            }
        }
    }

    @Override
    public <T> RunnableCommandBuilder<T, ? extends RunnableCommandBuilder<T, ?>> newCommand() {
        return new HeadlessCommandBuilder<>();
    }

    @Override
    public void setCurrentCommandGroupId(@Nullable Object groupId) {
    }

    @Override
    public boolean hasCurrentCommand() {
        return myCommandDepth.get() > 0;
    }

    @Override
    public @Nullable Object getCurrentCommandGroupId() {
        return null;
    }

    @Override
    public @Nullable Project getCurrentCommandProject() {
        return null;
    }

    private final ThreadLocal<Integer> myTransparentDepth = ThreadLocal.withInitial(() -> 0);

    @Override
    public void runUndoTransparentAction(Runnable action) {
        myTransparentDepth.set(myTransparentDepth.get() + 1);
        try {
            action.run();
        }
        finally {
            myTransparentDepth.set(myTransparentDepth.get() - 1);
        }
    }

    @Override
    public boolean isUndoTransparentActionInProgress() {
        return myTransparentDepth.get() > 0;
    }

    @Override
    public void markCurrentCommandAsGlobal(@Nullable Project project) {
    }

    @Override
    public void addAffectedDocuments(@Nullable Project project, Document... docs) {
    }

    @Override
    public void addAffectedFiles(@Nullable Project project, VirtualFile... files) {
    }

    @Override
    public void addCommandListener(CommandListener listener) {
    }

    @Override
    public void removeCommandListener(CommandListener listener) {
    }
}
