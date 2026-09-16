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
package consulo.virtualFileSystem.internal;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Classifying bytes as binary or text must not need any service, because it runs from content-based file type
 * detection, which can be reached while the project root index is being built. No application is booted here on
 * purpose: reintroducing a service lookup into the guess path fails these tests.
 *
 * @author VISTALL
 */
public class LoadTextUtilBinaryDetectionTest {
    private static final byte[] MANAGED_ASSEMBLY_HEADER = {
        'M', 'Z', (byte)0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, (byte)0xFF, (byte)0xFF, 0x00, 0x00
    };

    @Test
    public void binaryContentIsClassifiedWithoutAnyService() {
        assertThat(LoadTextUtil.getTextFromBytesOrNull(MANAGED_ASSEMBLY_HEADER, 0, MANAGED_ASSEMBLY_HEADER.length)).isNull();
    }

    @Test
    public void asciiContentIsStillDecoded() {
        byte[] bytes = "%YAML 1.1\n%TAG !u! tag:unity3d.com,2011:\n".getBytes(StandardCharsets.US_ASCII);

        assertThat(LoadTextUtil.getTextFromBytesOrNull(bytes, 0, bytes.length)).startsWith("%YAML");
    }

    @Test
    public void utf8ContentIsStillDecoded() {
        byte[] bytes = "m_Name: префаб\n".getBytes(StandardCharsets.UTF_8);

        assertThat(LoadTextUtil.getTextFromBytesOrNull(bytes, 0, bytes.length)).isEqualTo("m_Name: префаб\n");
    }

    @Test
    public void emptyContentIsNotBinary() {
        assertThat(LoadTextUtil.getTextFromBytesOrNull(new byte[0], 0, 0)).isEmpty();
    }
}
