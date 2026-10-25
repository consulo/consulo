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
package consulo.it.index;

import consulo.language.index.impl.internal.dependencies.IdeFingerprint;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fingerprint takes the platform's build stamp from the timestamp of the jar it is loaded from. On the module path
 * that jar is named by a {@code jar:file:...!/} URL; when it could not be read, the fallback - the build date - was the
 * start time of a SNAPSHOT build, so every restart of one invalidated every indexing flag.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class IdeFingerprintTest {
    @Test
    public void theJarOfAModulePathLocationIsFound() throws Exception {
        Path jar = Files.createTempFile("consulo-it-fingerprint", ".jar");

        URL moduleLocation = new URL("jar:" + jar.toUri() + "!/");

        assertThat(IdeFingerprint.codeSourcePath(moduleLocation)).isEqualTo(jar);
    }

    @Test
    public void aPlainFileLocationStaysAsItIs() throws Exception {
        Path jar = Files.createTempFile("consulo-it-fingerprint", ".jar");

        assertThat(IdeFingerprint.codeSourcePath(jar.toUri().toURL())).isEqualTo(jar);
    }
}
