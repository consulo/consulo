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
package consulo.component.store.impl.internal.json;

import consulo.component.persist.StateJsonSerializer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 2026-10-23
 */
public final class StateJsonSerializerImpl implements StateJsonSerializer {
    public static final StateJsonSerializer INSTANCE = new StateJsonSerializerImpl();

    private StateJsonSerializerImpl() {
    }

    @Override
    public boolean isJsonCapable(Class<?> stateClass) {
        return StateJsonBinder.isJsonCapable(stateClass);
    }

    @Override
    public byte[] serialize(Object state) throws IOException {
        return StateJsonBinder.serialize(state);
    }

    @Override
    public <T> @Nullable T deserialize(Class<T> stateClass, byte[] content) throws IOException {
        return StateJsonBinder.deserialize(stateClass, content);
    }

    @Override
    public @Nullable Object readTree(byte[] content) throws IOException {
        return StateJsonBinder.readTree(content);
    }

    @Override
    public byte[] writeTree(@Nullable Object tree) throws IOException {
        return StateJsonBinder.writeTree(tree);
    }
}
