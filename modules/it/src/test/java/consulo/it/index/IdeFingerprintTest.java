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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    /**
     * Plugins' bindings are collected in parallel, so the index extensions come in a different order on every start. The
     * fingerprint hashed their versions in that order: it changed on every start and every project was fully rescanned.
     */
    @Test
    public void indexVersionsHashDoesNotDependOnTheOrderOfIndexes() {
        List<IdeFingerprint.IndexVersion> versions = List.of(
            new IdeFingerprint.IndexVersion("Stubs", 3),
            new IdeFingerprint.IndexVersion("IdIndex", 17),
            new IdeFingerprint.IndexVersion("TodoIndex", 2),
            new IdeFingerprint.IndexVersion("FilenameIndex", 1)
        );
        List<IdeFingerprint.IndexVersion> shuffled = new ArrayList<>(versions);
        Collections.shuffle(shuffled, new Random(42));
        List<IdeFingerprint.IndexVersion> reversed = new ArrayList<>(versions);
        Collections.reverse(reversed);

        long hash = IdeFingerprint.indexVersionsHash(versions);

        assertThat(IdeFingerprint.indexVersionsHash(shuffled)).isEqualTo(hash);
        assertThat(IdeFingerprint.indexVersionsHash(reversed)).isEqualTo(hash);
    }

    @Test
    public void indexVersionsHashChangesWithAVersion() {
        long before = IdeFingerprint.indexVersionsHash(List.of(
            new IdeFingerprint.IndexVersion("Stubs", 3),
            new IdeFingerprint.IndexVersion("IdIndex", 17)
        ));
        long after = IdeFingerprint.indexVersionsHash(List.of(
            new IdeFingerprint.IndexVersion("Stubs", 4),
            new IdeFingerprint.IndexVersion("IdIndex", 17)
        ));

        assertThat(after).isNotEqualTo(before);
    }

    /**
     * Versions alone would not notice two indexes trading their versions.
     */
    @Test
    public void indexVersionsHashTakesTheIndexNamesIntoAccount() {
        long before = IdeFingerprint.indexVersionsHash(List.of(
            new IdeFingerprint.IndexVersion("Stubs", 3),
            new IdeFingerprint.IndexVersion("IdIndex", 17)
        ));
        long swapped = IdeFingerprint.indexVersionsHash(List.of(
            new IdeFingerprint.IndexVersion("Stubs", 17),
            new IdeFingerprint.IndexVersion("IdIndex", 3)
        ));

        assertThat(swapped).isNotEqualTo(before);
    }
}
