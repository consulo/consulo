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

import consulo.index.io.data.DataExternalizer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author nik
 */
public class DummyPersistentState {
    public static final DummyPersistentState INSTANCE = new DummyPersistentState();
    public static final DataExternalizer<DummyPersistentState> EXTERNALIZER = new DummyPersistentStateExternalizer();

    private DummyPersistentState() {
    }

    private static class DummyPersistentStateExternalizer implements DataExternalizer<DummyPersistentState> {
        @Override
        public void save(DataOutput out, DummyPersistentState value) throws IOException {
        }

        @Override
        public DummyPersistentState read(DataInput in) throws IOException {
            return INSTANCE;
        }
    }
}
