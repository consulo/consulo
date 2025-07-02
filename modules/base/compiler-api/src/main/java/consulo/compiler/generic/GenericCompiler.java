/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.compiler.generic;

import consulo.compiler.CompileContext;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.Compiler;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class GenericCompiler<Key, SourceState, OutputState> implements Compiler {
    protected static final KeyDescriptor<String> STRING_KEY_DESCRIPTOR = new EnumeratorStringDescriptor();
    private final String myId;
    private final int myVersion;
    private final CompileOrderPlace myOrderPlace;

    protected GenericCompiler(@Nonnull String id, int version, @Nonnull CompileOrderPlace orderPlace) {
        myId = id;
        myVersion = version;
        myOrderPlace = orderPlace;
    }

    @Nonnull
    public abstract KeyDescriptor<Key> getItemKeyDescriptor();

    @Nonnull
    public abstract DataExternalizer<SourceState> getSourceStateExternalizer();

    @Nonnull
    public abstract DataExternalizer<OutputState> getOutputStateExternalizer();

    @Nonnull
    public abstract GenericCompilerInstance<?, ? extends CompileItem<Key, SourceState, OutputState>, Key, SourceState, OutputState> createInstance(
        @Nonnull CompileContext context
    );

    public final String getId() {
        return myId;
    }

    public final int getVersion() {
        return myVersion;
    }

    @Override
    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    public CompileOrderPlace getOrderPlace() {
        return myOrderPlace;
    }

    public static enum CompileOrderPlace {
        CLASS_INSTRUMENTING,
        CLASS_POST_PROCESSING,
        PACKAGING,
        VALIDATING
    }
}
