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

import consulo.index.io.data.IOUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 2026-08-30
 */
public record OutputSourceInfo(String sourcePath, @Nullable String className) {
    public static OutputSourceInfo read(DataInput in) throws IOException {
        String sourcePath = IOUtil.readUTF(in);
        String className = StringUtil.nullize(IOUtil.readUTF(in));
        return new OutputSourceInfo(sourcePath, className);
    }

    public void write(DataOutput out) throws IOException {
        IOUtil.writeUTF(out, sourcePath);
        IOUtil.writeUTF(out, StringUtil.notNullize(className));
    }
}
