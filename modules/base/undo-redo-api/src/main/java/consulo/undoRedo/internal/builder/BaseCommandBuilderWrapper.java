/*
 * Copyright 2013-2024 consulo.io
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
package consulo.undoRedo.internal.builder;

import consulo.application.Application;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.undoRedo.builder.CommandBuilder;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2024-10-21
 */
public abstract class BaseCommandBuilderWrapper<THIS extends CommandBuilder<THIS>, THAT extends WrappableCommandBuilder<THAT>>
    implements WrappableCommandBuilder<THIS> {
    protected THAT mySubBuilder;

    protected BaseCommandBuilderWrapper(THAT subBuilder) {
        mySubBuilder = subBuilder;
    }

    @Override
    public THIS name(@Nonnull LocalizeValue name) {
        mySubBuilder = mySubBuilder.name(name);
        return self();
    }

    @Override
    public THIS groupId(Object groupId) {
        mySubBuilder = mySubBuilder.groupId(groupId);
        return self();
    }

    @Override
    public THIS project(Project project) {
        mySubBuilder = mySubBuilder.project(project);
        return self();
    }

    @Override
    public THIS document(Document document) {
        mySubBuilder = mySubBuilder.document(document);
        return self();
    }

    @Override
    public THIS undoConfirmationPolicy(@Nonnull UndoConfirmationPolicy undoConfirmationPolicy) {
        mySubBuilder = mySubBuilder.undoConfirmationPolicy(undoConfirmationPolicy);
        return self();
    }

    @Override
    public THIS shouldRecordActionForActiveDocument(boolean shouldRecordActionForActiveDocument) {
        mySubBuilder = mySubBuilder.shouldRecordActionForActiveDocument(shouldRecordActionForActiveDocument);
        return self();
    }

    @Override
    public CommandDescriptor build(@Nonnull Runnable command) {
        return mySubBuilder.build(command);
    }

    @Override
    public Application getApplication() {
        return mySubBuilder.getApplication();
    }

    @Override
    public CommandProcessor getCommandProcessor() {
        return mySubBuilder.getCommandProcessor();
    }

    @SuppressWarnings("unchecked")
    protected THIS self() {
        return (THIS)this;
    }
}
