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

import consulo.index.io.data.DataExternalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author VISTALL
 * @since 2026-08-30
 */
public class PathPersistentMapTest {
    private static final DataExternalizer<OutputSourceInfo> EXTERNALIZER = new DataExternalizer<>() {
        @Override
        public void save(DataOutput out, OutputSourceInfo value) throws IOException {
            value.write(out);
        }

        @Override
        public OutputSourceInfo read(DataInput in) throws IOException {
            return OutputSourceInfo.read(in);
        }
    };

    @TempDir
    Path myTempDir;

    @Test
    public void testPutGetAndReopen() throws IOException {
        File baseFile = myTempDir.resolve("out_to_src").toFile();

        PathPersistentMap<OutputSourceInfo> map = new PathPersistentMap<>(baseFile, EXTERNALIZER);
        map.put("/out/a/Foo.class", new OutputSourceInfo("/src/a/Foo.java", "a.Foo"));
        map.put("/out/a/Bar.class", new OutputSourceInfo("/src/a/Bar.java", null));
        map.close();

        PathPersistentMap<OutputSourceInfo> reopened = new PathPersistentMap<>(baseFile, EXTERNALIZER);
        OutputSourceInfo foo = reopened.get("/out/a/Foo.class");
        assertNotNull(foo);
        assertEquals("/src/a/Foo.java", foo.sourcePath());
        assertEquals("a.Foo", foo.className());

        OutputSourceInfo bar = reopened.get("/out/a/Bar.class");
        assertNotNull(bar);
        assertEquals("/src/a/Bar.java", bar.sourcePath());
        assertNull(bar.className());

        Collection<String> keys = reopened.getAllKeys();
        assertEquals(2, keys.size());
        reopened.close();
    }

    @Test
    public void testRemove() throws IOException {
        File baseFile = myTempDir.resolve("stamps").toFile();

        PathPersistentMap<OutputSourceInfo> map = new PathPersistentMap<>(baseFile, EXTERNALIZER);
        map.put("/src/Foo.java", new OutputSourceInfo("/src/Foo.java", null));
        map.remove("/src/Foo.java");
        assertNull(map.get("/src/Foo.java"));
        assertTrue(map.getAllKeys().isEmpty());
        map.close();
    }

    @Test
    public void testWipe() throws IOException {
        File baseFile = myTempDir.resolve("src_to_out").toFile();

        PathPersistentMap<OutputSourceInfo> map = new PathPersistentMap<>(baseFile, EXTERNALIZER);
        map.put("/src/Foo.java", new OutputSourceInfo("/src/Foo.java", "Foo"));
        map.wipe();

        assertNull(map.get("/src/Foo.java"));
        assertTrue(map.getAllKeys().isEmpty());

        map.put("/src/Bar.java", new OutputSourceInfo("/src/Bar.java", "Bar"));
        assertNotNull(map.get("/src/Bar.java"));
        map.close();
    }

    @Test
    public void testProcessKeys() throws IOException {
        File baseFile = myTempDir.resolve("keys").toFile();

        PathPersistentMap<OutputSourceInfo> map = new PathPersistentMap<>(baseFile, EXTERNALIZER);
        map.put("/root/a.java", new OutputSourceInfo("/root/a.java", null));
        map.put("/root/sub/b.java", new OutputSourceInfo("/root/sub/b.java", null));
        map.put("/other/c.java", new OutputSourceInfo("/other/c.java", null));

        Collection<String> underRoot = new java.util.ArrayList<>();
        map.processKeys(path -> {
            if (path.startsWith("/root/")) {
                underRoot.add(path);
            }
            return true;
        });
        assertEquals(2, underRoot.size());
        map.close();
    }
}
