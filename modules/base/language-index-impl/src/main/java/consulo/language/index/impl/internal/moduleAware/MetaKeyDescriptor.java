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
package consulo.language.index.impl.internal.moduleAware;

import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataInputOutputUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class MetaKeyDescriptor implements KeyDescriptor<MetaKey> {
    public static final MetaKeyDescriptor INSTANCE = new MetaKeyDescriptor();

    private MetaKeyDescriptor() {
    }

    @Override
    public void save(DataOutput out, MetaKey value) throws IOException {
        DataInputOutputUtil.writeINT(out, value.indexUniqueId());
        DataInputOutputUtil.writeINT(out, value.fileId());
    }

    @Override
    public MetaKey read(DataInput in) throws IOException {
        int indexUniqueId = DataInputOutputUtil.readINT(in);
        int fileId = DataInputOutputUtil.readINT(in);
        return new MetaKey(indexUniqueId, fileId);
    }
}
