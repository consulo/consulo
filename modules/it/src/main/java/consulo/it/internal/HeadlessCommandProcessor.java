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
import consulo.document.Document;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.builder.RunnableCommandBuilder;
import consulo.undoRedo.event.CommandListener;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * The production {@code CommandProcessorImpl} lives in {@code consulo.ide.impl} which is not part of the headless
 * application. VFS event listeners (e.g. {@code PerFileMappingsBase}) query the command state during event delivery,
 * so the headless application answers "no command is running" to everything.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessCommandProcessor extends CommandProcessor {
    @Override
    public <T> RunnableCommandBuilder<T, ? extends RunnableCommandBuilder<T, ?>> newCommand() {
        throw new UnsupportedOperationException("Commands are not supported in the headless application");
    }

    @Override
    public void setCurrentCommandGroupId(@Nullable Object groupId) {
    }

    @Override
    public boolean hasCurrentCommand() {
        return false;
    }

    @Override
    public @Nullable Object getCurrentCommandGroupId() {
        return null;
    }

    @Override
    public @Nullable Project getCurrentCommandProject() {
        return null;
    }

    @Override
    public void runUndoTransparentAction(Runnable action) {
        action.run();
    }

    @Override
    public boolean isUndoTransparentActionInProgress() {
        return false;
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
