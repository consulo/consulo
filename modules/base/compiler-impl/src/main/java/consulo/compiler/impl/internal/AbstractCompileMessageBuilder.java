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
package consulo.compiler.impl.internal;

import consulo.compiler.CompileContext;
import consulo.compiler.CompilerMessageCategory;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author UNV
 * @since 2026-06-23
 */
public abstract class AbstractCompileMessageBuilder implements CompileContext.MessageBuilder {
    protected final CompilerMessageCategory myCategory;
    protected final LocalizeValue myMessage;

    protected @Nullable Navigatable myNavigatable = null;
    protected @Nullable VirtualFile myFile = null;
    protected int myRow = -1;
    protected int myColumn = -1;

    protected AbstractCompileMessageBuilder(CompilerMessageCategory category, LocalizeValue message) {
        myCategory = category;
        myMessage = message;
    }

    protected CompileContext.MessageBuilder optionalFile(@Nullable VirtualFile file) {
        myFile = file;
        return this;
    }

    @Override
    public CompileContext.MessageBuilder position(int row, int column) {
        myRow = row;
        myColumn = column;
        return this;
    }

    @Override
    public CompileContext.MessageBuilder navigatable(Navigatable navigatable) {
        myNavigatable = navigatable;
        return this;
    }
}
