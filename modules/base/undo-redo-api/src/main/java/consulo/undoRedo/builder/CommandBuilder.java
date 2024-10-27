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

import consulo.application.Application;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandDescriptor;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author UNV
 * @since 2024-10-21
 */
public interface CommandBuilder<THIS extends CommandBuilder<THIS>> {
    THIS name(@Nonnull LocalizeValue name);

    THIS groupId(Object groupId);

    THIS project(Project project);

    THIS document(Document document);

    THIS undoConfirmationPolicy(@Nonnull UndoConfirmationPolicy undoConfirmationPolicy);

    THIS shouldRecordActionForActiveDocument(boolean shouldRecordActionForActiveDocument);

    /* Context methods: execute in specific context */

    @SuppressWarnings("unchecked")
    default THIS inLater() {
        return (THIS)new ProxyCommandBuilder(this) {
            @Override
            @RequiredUIAccess
            public void run(@Nonnull Runnable runnable) {
                Application.get().invokeLater(runnable);
            }
        };
    }

    @SuppressWarnings("unchecked")
    default THIS inWriteAction() {
        return (THIS)new ProxyCommandBuilder(this) {
            @Override
            @RequiredUIAccess
            public void run(@Nonnull Runnable runnable) {
                Application.get().runWriteAction(runnable);
            }
        };
    }

    @SuppressWarnings("unchecked")
    default THIS inBulkUpdate() {
        return (THIS)new ProxyCommandBuilder(this) {
            @Override
            @RequiredUIAccess
            public void run(@Nonnull Runnable runnable) {
                super.run(() -> DocumentUtil.executeInBulk(build(runnable).document(), true, runnable));
            }
        };
    }

    @SuppressWarnings("unchecked")
    default THIS inBulkUpdateIf(boolean inBulkUpdate) {
        return inBulkUpdate ? inBulkUpdate() : (THIS)this;
    }

    @SuppressWarnings("unchecked")
    default THIS inGlobalUndoAction() {
        return (THIS)new ProxyCommandBuilder(this) {
            @Override
            public void run(@Nonnull Runnable runnable) {
                CommandDescriptor descriptor = build(runnable);
                super.run(() -> {
                    CommandProcessor.getInstance().markCurrentCommandAsGlobal(descriptor.project());
                    runnable.run();
                });
            }
        };
    }

    @SuppressWarnings("unchecked")
    default THIS inGlobalUndoActionIf(boolean globalUndoAction) {
        return globalUndoAction ? inGlobalUndoAction() : (THIS)this;
    }

    /* Finishing methods */

    CommandDescriptor build(@RequiredUIAccess @Nonnull Runnable command);

    void run(@RequiredUIAccess @Nonnull Runnable runnable);

    default <T> T get(@RequiredUIAccess @Nonnull Supplier<T> supplier) {
        SimpleReference<T> result = SimpleReference.create();
        run(() -> result.set(supplier.get()));
        return result.get();
    }
}
