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

import consulo.logging.Logger;
import consulo.task.TaskRepositoryOptions;
import consulo.task.TaskRepositoryOptionsUtil;
import consulo.task.TaskRepositoryType;
import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class TaskRepositoryStore {
    private static final Logger LOG = Logger.getInstance(TaskRepositoryStore.class);

    public static final String FORMAT = "2";

    private final List<TaskRepositoryEntry> myConfigured = new ArrayList<>();
    private final Set<String> myHiddenKeys = new LinkedHashSet<>();
    private final List<Element> myUnknownServers = new ArrayList<>();

    public synchronized List<TaskRepositoryEntry> getConfigured() {
        return new ArrayList<>(myConfigured);
    }

    public synchronized Set<String> getHiddenKeys() {
        return new LinkedHashSet<>(myHiddenKeys);
    }

    public synchronized void set(List<TaskRepositoryEntry> configured, Set<String> hiddenKeys) {
        myConfigured.clear();
        myConfigured.addAll(configured);
        myHiddenKeys.clear();
        myHiddenKeys.addAll(hiddenKeys);
    }

    public synchronized Element write() {
        Element element = new Element("servers");
        element.setAttribute("format", FORMAT);
        for (TaskRepositoryEntry entry : myConfigured) {
            Element server = new Element("server");
            server.setAttribute("type", entry.getType().getId());
            server.setAttribute("key", entry.getKey());
            server.addContent(TaskRepositoryOptionsUtil.serialize(entry.getOptions()));
            element.addContent(server);
        }
        for (Element unknown : myUnknownServers) {
            element.addContent(unknown.clone());
        }
        for (String key : myHiddenKeys) {
            element.addContent(new Element("hidden").setAttribute("key", key));
        }
        return element;
    }

    public synchronized void read(Element element, Function<String, @Nullable TaskRepositoryType<?>> types) {
        myConfigured.clear();
        myUnknownServers.clear();
        myHiddenKeys.clear();
        if (!FORMAT.equals(element.getAttributeValue("format"))) {
            return;
        }
        for (Element child : element.getChildren()) {
            if ("hidden".equals(child.getName())) {
                String key = child.getAttributeValue("key");
                if (key != null) {
                    myHiddenKeys.add(key);
                }
                continue;
            }
            if (!"server".equals(child.getName())) {
                continue;
            }
            TaskRepositoryType<?> type = types.apply(StringUtil.notNullize(child.getAttributeValue("type")));
            Element options = child.getChild(TaskRepositoryOptionsUtil.OPTIONS_ELEMENT);
            if (type == null || options == null) {
                myUnknownServers.add(child.clone());
                continue;
            }
            try {
                myConfigured.add(new TaskRepositoryEntry(type, type.readOptions(options), false));
            }
            catch (Exception e) {
                LOG.error("Cannot load " + type.getId() + " repository", e);
                myUnknownServers.add(child.clone());
            }
        }
    }

    public List<TaskRepositoryEntry> merge(
        List<TaskRepositoryEntry> detected,
        List<TaskSharedServer> sharedServers,
        Function<String, @Nullable TaskRepositoryType<?>> types
    ) {
        List<TaskRepositoryEntry> configured = getConfigured();
        Set<String> hidden = getHiddenKeys();

        List<TaskRepositoryEntry> entries = new ArrayList<>(configured);
        Set<String> keys = new HashSet<>();
        for (TaskRepositoryEntry entry : configured) {
            keys.add(entry.getKey());
        }

        for (TaskRepositoryEntry entry : detected) {
            String key = entry.getKey();
            if (!hidden.contains(key) && keys.add(key)) {
                entries.add(entry);
            }
        }

        for (TaskSharedServer server : sharedServers) {
            TaskRepositoryType<?> type = types.apply(server.type);
            if (type == null || isServerPresent(entries, type, server.serverUrl)) {
                continue;
            }
            TaskRepositoryOptions options = type.createOptions();
            options.serverUrl = server.serverUrl;
            options.shared = true;
            TaskRepositoryEntry entry = new TaskRepositoryEntry(type, options, true);
            if (!hidden.contains(entry.getKey()) && keys.add(entry.getKey())) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public static List<TaskSharedServer> collectSharedServers(List<TaskRepositoryEntry> entries) {
        List<TaskSharedServer> servers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (TaskRepositoryEntry entry : entries) {
            TaskRepositoryOptions options = entry.getOptions();
            if (options.shared && !options.serverUrl.isEmpty() && seen.add(entry.getType().getId() + " " + options.serverUrl)) {
                TaskSharedServer server = new TaskSharedServer();
                server.type = entry.getType().getId();
                server.serverUrl = options.serverUrl;
                servers.add(server);
            }
        }
        return servers;
    }

    private static boolean isServerPresent(List<TaskRepositoryEntry> entries, TaskRepositoryType<?> type, String serverUrl) {
        for (TaskRepositoryEntry entry : entries) {
            if (entry.getType() == type && serverUrl.equals(entry.getOptions().serverUrl)) {
                return true;
            }
        }
        return false;
    }
}
