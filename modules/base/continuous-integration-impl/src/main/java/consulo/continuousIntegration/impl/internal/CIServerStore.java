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
package consulo.continuousIntegration.impl.internal;

import consulo.continuousIntegration.CIServerOptionsUtil;
import consulo.continuousIntegration.CIServerType;
import consulo.logging.Logger;
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
 * Configured servers and hidden detected servers. Servers of unknown types (plugin not loaded) are kept as they are.
 *
 * @since 2026-11-09
 */
public final class CIServerStore {
    private static final Logger LOG = Logger.getInstance(CIServerStore.class);

    public static final String FORMAT = "1";

    private final List<CIServerEntry> myConfigured = new ArrayList<>();
    private final Set<String> myHiddenKeys = new LinkedHashSet<>();
    private final List<Element> myUnknownServers = new ArrayList<>();

    public synchronized List<CIServerEntry> getConfigured() {
        return new ArrayList<>(myConfigured);
    }

    public synchronized Set<String> getHiddenKeys() {
        return new LinkedHashSet<>(myHiddenKeys);
    }

    public synchronized void set(List<CIServerEntry> configured, Set<String> hiddenKeys) {
        myConfigured.clear();
        myConfigured.addAll(configured);
        myHiddenKeys.clear();
        myHiddenKeys.addAll(hiddenKeys);
    }

    public synchronized Element write() {
        Element element = new Element("servers");
        element.setAttribute("format", FORMAT);
        for (CIServerEntry entry : myConfigured) {
            Element server = new Element("server");
            server.setAttribute("type", entry.getType().getId());
            server.setAttribute("key", entry.getKey());
            server.addContent(CIServerOptionsUtil.serialize(entry.getOptions()));
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

    public synchronized void read(Element element, Function<String, @Nullable CIServerType<?>> types) {
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
            CIServerType<?> type = types.apply(StringUtil.notNullize(child.getAttributeValue("type")));
            Element options = child.getChild(CIServerOptionsUtil.OPTIONS_ELEMENT);
            if (type == null || options == null) {
                myUnknownServers.add(child.clone());
                continue;
            }
            try {
                myConfigured.add(new CIServerEntry(type, type.readOptions(options), false));
            }
            catch (Exception e) {
                LOG.error("Cannot load " + type.getId() + " server", e);
                myUnknownServers.add(child.clone());
            }
        }
    }

    public List<CIServerEntry> merge(List<CIServerEntry> detected) {
        List<CIServerEntry> configured = getConfigured();
        Set<String> hidden = getHiddenKeys();
        List<CIServerEntry> entries = new ArrayList<>(configured);
        Set<String> keys = new HashSet<>();
        for (CIServerEntry entry : configured) {
            keys.add(entry.getKey());
        }
        for (CIServerEntry entry : detected) {
            String key = entry.getKey();
            if (!hidden.contains(key) && keys.add(key)) {
                entries.add(entry);
            }
        }
        return entries;
    }
}
