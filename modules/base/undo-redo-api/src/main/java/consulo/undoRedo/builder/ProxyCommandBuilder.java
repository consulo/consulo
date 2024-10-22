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
package consulo.undoRedo.builder;

import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.UndoConfirmationPolicy;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2024-10-21
 */
public abstract class ProxyCommandBuilder<THIS extends CommandBuilder, THAT extends CommandBuilder> implements CommandBuilder<THIS> {
    protected final THAT subBuilder;

    protected ProxyCommandBuilder(THAT subBuilder) {
        this.subBuilder = subBuilder;
    }

    @Override
    public THIS withName(@Nonnull LocalizeValue name) {
        subBuilder.withName(name);
        return self();
    }

    @Override
    public THIS withGroupId(Object groupId) {
        subBuilder.withGroupId(groupId);
        return self();
    }

    @Override
    public THIS withProject(Project project) {
        subBuilder.withProject(project);
        return self();
    }

    @Override
    public THIS withDocument(Document document) {
        subBuilder.withDocument(document);
        return self();
    }

    @Override
    public THIS withUndoConfirmationPolicy(@Nonnull UndoConfirmationPolicy undoConfirmationPolicy) {
        subBuilder.withUndoConfirmationPolicy(undoConfirmationPolicy);
        return self();
    }

    @Override
    public THIS withShouldRecordActionForActiveDocument(boolean shouldRecordActionForActiveDocument) {
        subBuilder.withShouldRecordActionForActiveDocument(shouldRecordActionForActiveDocument);
        return self();
    }

    @Override
    public CommandDescriptor build() {
        return subBuilder.build();
    }

    @SuppressWarnings("unchecked")
    protected THIS self() {
        return (THIS)this;
    }
}
