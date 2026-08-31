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
package consulo.compiler.impl.internal.state;

import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.IOUtil;
import consulo.util.io.FileUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 2026-08-30
 */
public class PathKeyDescriptor implements KeyDescriptor<String> {
    public static final PathKeyDescriptor INSTANCE = new PathKeyDescriptor();

    @Override
    public int hashCode(String value) {
        return FileUtil.PATH_HASHING_STRATEGY.hashCode(value);
    }

    @Override
    public boolean equals(String val1, String val2) {
        return FileUtil.PATH_HASHING_STRATEGY.equals(val1, val2);
    }

    @Override
    public void save(DataOutput out, String value) throws IOException {
        IOUtil.writeUTF(out, value);
    }

    @Override
    public String read(DataInput in) throws IOException {
        return IOUtil.readUTF(in);
    }
}
