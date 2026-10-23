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
package consulo.virtualFileSystem.impl.internal.url;

import consulo.virtualFileSystem.url.VirtualFileUrl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VirtualFileUrlManagerImplTest {
    private final VirtualFileUrlManagerImpl myManager = new VirtualFileUrlManagerImpl(() -> {
        throw new AssertionError("the trie must not touch the VFS");
    });

    @Test
    public void sameUrlIsSameInstance() {
        VirtualFileUrl first = myManager.storeAndGet("file:///home/user/jdk/lib");
        VirtualFileUrl second = myManager.storeAndGet("file:///home/user/jdk/lib");

        assertThat(second).isSameAs(first);
        assertThat(first.getUrl()).isEqualTo("file:///home/user/jdk/lib");
        assertThat(first.getFileName()).isEqualTo("lib");
    }

    @Test
    public void getFindsOnlyRegisteredUrls() {
        myManager.storeAndGet("file:///home/user/jdk/lib");

        assertThat(myManager.get("file:///home/user/jdk/lib")).isNotNull();
        assertThat(myManager.get("file:///home/user/jdk")).isNull();
        assertThat(myManager.findNode("file:///home/user/jdk")).isNotNull();
        assertThat(myManager.get("file:///home/other")).isNull();
    }

    @Test
    public void archiveUrls() {
        VirtualFileUrl url = myManager.storeAndGet("jar:///home/user/jdk/src.zip!/");

        assertThat(url.getUrl()).isEqualTo("jar:///home/user/jdk/src.zip!/");
        assertThat(url.getPresentableUrl()).isEqualTo("/home/user/jdk/src.zip");
    }

    @Test
    public void parentAndAppend() {
        VirtualFileUrl jdk = myManager.storeAndGet("file:///home/user/jdk");
        VirtualFileUrl lib = jdk.append("lib/rt");

        assertThat(lib.getUrl()).isEqualTo("file:///home/user/jdk/lib/rt");
        assertThat(lib).isSameAs(myManager.get("file:///home/user/jdk/lib/rt"));
        assertThat(lib.getParent()).isNotNull();
        assertThat(lib.getParent().getUrl()).isEqualTo("file:///home/user/jdk/lib");
        assertThat(jdk.getPresentableUrl()).isEqualTo("/home/user/jdk");
    }

    @Test
    public void processChildrenVisitsRegisteredDescendants() {
        myManager.storeAndGet("file:///home/user/jdk/lib/a.jar");
        myManager.storeAndGet("file:///home/user/jdk/src");
        myManager.storeAndGet("file:///home/other/b.jar");

        List<String> visited = new ArrayList<>();
        myManager.processChildrenRecursively("file:///home/user/jdk", url -> {
            visited.add(url.getUrl());
            return TreeNodeProcessingResult.CONTINUE;
        });

        assertThat(visited).containsExactlyInAnyOrder("file:///home/user/jdk/lib/a.jar", "file:///home/user/jdk/src");
    }

    @Test
    public void fromPathAndEmptyUrl() {
        assertThat(myManager.fromPath("/home/user/jdk").getUrl()).isEqualTo("file:///home/user/jdk");
        assertThat(myManager.storeAndGet("").getUrl()).isEmpty();
        assertThat(myManager.get("")).isNull();
    }
}
