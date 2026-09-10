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
package consulo.ide.impl.idea.openapi.vfs.encoding;

import com.dslplatform.json.CompiledJson;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.virtualFileSystem.encoding.EncodingReference;

import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2026-09-08
 */
@CompiledJson
public class EncodingManagerState {
    EncodingReference myDefaultEncoding = new EncodingReference(StandardCharsets.UTF_8);

    EncodingReference myDefaultConsoleEncoding = EncodingReference.DEFAULT;

    @Attribute("default_encoding")
    public String getDefaultCharsetName() {
        return myDefaultEncoding.getCharset() == null ? "" : myDefaultEncoding.getCharset().name();
    }

    public void setDefaultCharsetName(String name) {
        myDefaultEncoding = new EncodingReference(StringUtil.nullize(name));
    }

    @Attribute("default_console_encoding")
    public String getDefaultConsoleEncodingName() {
        return myDefaultConsoleEncoding.getCharset() == null ? "" : myDefaultConsoleEncoding.getCharset().name();
    }

    public void setDefaultConsoleEncodingName(String name) {
        myDefaultConsoleEncoding = new EncodingReference(StringUtil.nullize(name));
    }
}
