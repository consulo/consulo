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
package consulo.task.impl.internal;

import consulo.task.CustomTaskState;
import consulo.task.TaskRepositoryOptionsUtil;
import consulo.task.TaskRepositoryType;
import consulo.task.TaskState;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskRepositoryStoreTest {
    private final TestTaskRepositoryType myType = new TestTaskRepositoryType("Test");

    private @Nullable TaskRepositoryType<?> find(String id) {
        return myType.getId().equals(id) ? myType : null;
    }

    private static TestTaskRepositoryOptions options(@Nullable String accountId, String serverUrl, String projectKey) {
        TestTaskRepositoryOptions options = new TestTaskRepositoryOptions();
        options.accountId = accountId;
        options.serverUrl = serverUrl;
        options.projectKey = projectKey;
        return options;
    }

    @Test
    public void optionsRoundTripAndCopyIsIndependent() {
        TestTaskRepositoryOptions options = options("account", "https://server", "DEMO");
        options.shouldFormatCommitMessage = true;
        options.preferredOpenTaskState = CustomTaskState.fromPredefined(TaskState.IN_PROGRESS);

        Element element = TaskRepositoryOptionsUtil.serialize(options);
        TestTaskRepositoryOptions restored = myType.readOptions(element);

        assertThat(restored.projectKey).isEqualTo("DEMO");
        assertThat(restored.shouldFormatCommitMessage).isTrue();
        assertThat(restored.preferredOpenTaskState).isNotNull();
        assertThat(restored.preferredOpenTaskState.getId()).isEqualTo(TaskState.IN_PROGRESS.name());
        assertThat(TaskRepositoryOptionsUtil.isEqual(options, restored)).isTrue();

        TestTaskRepositoryOptions copy = myType.copyOptions(options);
        copy.projectKey = "OTHER";
        assertThat(options.projectKey).isEqualTo("DEMO");
        assertThat(TaskRepositoryOptionsUtil.isEqual(options, copy)).isFalse();
    }

    @Test
    public void configuredHiddenAndUnknownServersRoundTrip() {
        TaskRepositoryStore store = new TaskRepositoryStore();
        store.set(List.of(new TaskRepositoryEntry(myType, options("account", "https://server", "DEMO"), false)), Set.of("hidden-key"));

        Element written = store.write();
        assertThat(written.getAttributeValue("format")).isEqualTo(TaskRepositoryStore.FORMAT);
        assertThat(written.getChildren("server")).hasSize(1);
        assertThat(written.getChildren("hidden")).hasSize(1);

        written.addContent(new Element("server").setAttribute("type", "NotInstalled").addContent(new Element("options")));

        TaskRepositoryStore loaded = new TaskRepositoryStore();
        loaded.read(written, this::find);

        assertThat(loaded.getConfigured()).hasSize(1);
        assertThat(TaskRepositoryOptionsUtil.isEqual(loaded.getConfigured().get(0).getOptions(), options("account", "https://server", "DEMO"))).isTrue();
        assertThat(loaded.getHiddenKeys()).containsExactly("hidden-key");
        assertThat(loaded.write().getChildren("server"))
            .extracting(child -> child.getAttributeValue("type"))
            .containsExactly("Test", "NotInstalled");
    }

    @Test
    public void oldFormatIsIgnored() {
        TaskRepositoryStore store = new TaskRepositoryStore();
        store.read(new Element("servers").addContent(new Element("Test").setAttribute("url", "https://old")), this::find);

        assertThat(store.getConfigured()).isEmpty();
        assertThat(store.write().getChildren()).isEmpty();
    }

    @Test
    public void mergeKeepsConfiguredOverDetectedAndDropsHidden() {
        TaskRepositoryStore store = new TaskRepositoryStore();
        TaskRepositoryEntry edited = new TaskRepositoryEntry(myType, options("a1", "https://server", "EDITED"), false);
        TaskRepositoryEntry detectedSame = new TaskRepositoryEntry(myType, options("a1", "https://server", "A"), true);
        TaskRepositoryEntry detectedHidden = new TaskRepositoryEntry(myType, options("a2", "https://server", "A"), true);
        TaskRepositoryEntry detectedOther = new TaskRepositoryEntry(myType, options("a3", "https://server", "A"), true);
        store.set(List.of(edited), Set.of(detectedHidden.getKey()));

        List<TaskRepositoryEntry> merged = store.merge(List.of(detectedSame, detectedHidden, detectedOther), List.of(), this::find);

        assertThat(merged).containsExactly(edited, detectedOther);
    }

    @Test
    public void sharedServerWithoutTrackerBecomesUnboundEntry() {
        TaskRepositoryStore store = new TaskRepositoryStore();
        TaskSharedServer present = new TaskSharedServer();
        present.type = "Test";
        present.serverUrl = "https://server";
        TaskSharedServer missing = new TaskSharedServer();
        missing.type = "Test";
        missing.serverUrl = "https://shared";
        TaskSharedServer notInstalled = new TaskSharedServer();
        notInstalled.type = "NotInstalled";
        notInstalled.serverUrl = "https://elsewhere";

        TaskRepositoryEntry detected = new TaskRepositoryEntry(myType, options("a1", "https://server", "A"), true);
        List<TaskRepositoryEntry> merged = store.merge(List.of(detected), List.of(present, missing, notInstalled), this::find);

        assertThat(merged).hasSize(2);
        TaskRepositoryEntry unbound = merged.get(1);
        assertThat(unbound.isDetected()).isTrue();
        assertThat(unbound.getOptions().accountId).isNull();
        assertThat(unbound.getOptions().serverUrl).isEqualTo("https://shared");
        assertThat(unbound.getOptions().shared).isTrue();
    }

    @Test
    public void sharedServersAreCollectedOncePerTypeAndUrl() {
        TestTaskRepositoryOptions first = options("a1", "https://server", "A");
        first.shared = true;
        TestTaskRepositoryOptions second = options("a2", "https://server", "B");
        second.shared = true;
        TestTaskRepositoryOptions personal = options("a3", "https://personal", "C");

        List<TaskSharedServer> servers = TaskRepositoryStore.collectSharedServers(List.of(
            new TaskRepositoryEntry(myType, first, false),
            new TaskRepositoryEntry(myType, second, true),
            new TaskRepositoryEntry(myType, personal, false)
        ));

        assertThat(servers).hasSize(1);
        assertThat(servers.get(0).serverUrl).isEqualTo("https://server");
    }
}
