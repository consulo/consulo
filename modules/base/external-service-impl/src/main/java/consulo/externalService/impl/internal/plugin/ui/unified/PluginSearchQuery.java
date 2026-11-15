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
package consulo.externalService.impl.internal.plugin.ui.unified;

import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.extension.preview.ExtensionPreviewAcceptor;
import consulo.component.internal.PluginDescriptorWithExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.impl.internal.pluginAdvertiser.PluginAdvertiserImpl;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The search text of the plugins page: free text with filters - {@code /tag:vcs} for a tag, {@code /extension:*.yaml}
 * for plugins supporting a file name, {@code /extension:<api class>=<id>} for plugins with that extension. Filters of
 * one kind match any of their values, the text matches as before. A value with spaces is quoted.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public final class PluginSearchQuery {
    private static final Logger LOG = Logger.getInstance(PluginSearchQuery.class);

    public static final String TAG = "/tag:";
    public static final String EXTENSION = "/extension:";

    private final List<String> myTags = new ArrayList<>();
    /**
     * Api class name to the id of the extension.
     */
    private final List<Map.Entry<String, String>> myExtensions = new ArrayList<>();
    private final @Nullable String myText;

    private PluginSearchQuery(String query) {
        StringBuilder text = new StringBuilder();
        for (String token : tokenize(query)) {
            if (token.startsWith(TAG) && token.length() > TAG.length()) {
                myTags.add(unquote(token.substring(TAG.length())));
            }
            else if (token.startsWith(EXTENSION) && token.length() > EXTENSION.length()) {
                String value = unquote(token.substring(EXTENSION.length()));
                int separator = value.indexOf('=');
                myExtensions.add(separator < 0
                    ? Map.entry(FileTypeFactory.class.getName(), value)
                    : Map.entry(value.substring(0, separator), value.substring(separator + 1)));
            }
            else {
                if (!text.isEmpty()) {
                    text.append(' ');
                }
                text.append(token);
            }
        }
        myText = StringUtil.nullize(text.toString(), true);
    }

    public static PluginSearchQuery parse(@Nullable String query) {
        return new PluginSearchQuery(StringUtil.notNullize(query));
    }

    /**
     * @return the query finding plugins with any of the extensions
     */
    public static String of(Collection<ExtensionPreview> previews) {
        StringBuilder sb = new StringBuilder();
        for (ExtensionPreview preview : previews) {
            String value = FileTypeFactory.class.getName().equals(preview.apiClassName())
                ? preview.implId()
                : preview.apiClassName() + "=" + preview.implId();
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(EXTENSION).append(quote(value));
        }
        return sb.toString();
    }

    /**
     * @return the query with its tag filters replaced by the tag, or removed when the tag is null
     */
    public static String withTag(@Nullable String query, @Nullable String tag) {
        StringBuilder sb = new StringBuilder();
        for (String token : tokenize(StringUtil.notNullize(query))) {
            if (!token.startsWith(TAG)) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(token);
            }
        }
        if (tag != null) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(TAG).append(quote(tag));
        }
        return sb.toString();
    }

    public @Nullable String getTag() {
        return myTags.isEmpty() ? null : myTags.get(0);
    }

    /**
     * @return the text without the filters
     */
    public @Nullable String getText() {
        return myText;
    }

    public boolean acceptsFilters(PluginDescriptor descriptor) {
        if (!myTags.isEmpty() && !hasAnyTag(descriptor)) {
            return false;
        }
        return myExtensions.isEmpty() || hasAnyExtension(descriptor);
    }

    private boolean hasAnyTag(PluginDescriptor descriptor) {
        for (String tag : descriptor.getTags()) {
            for (String wanted : myTags) {
                if (tag.equalsIgnoreCase(wanted)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAnyExtension(PluginDescriptor descriptor) {
        if (!(descriptor instanceof PluginDescriptorWithExtensionPreview withPreviews)) {
            return false;
        }

        for (ExtensionPreview pluginPreview : withPreviews.getExtensionPreviews()) {
            for (Map.Entry<String, String> wanted : myExtensions) {
                if (!pluginPreview.apiClassName().equals(wanted.getKey())) {
                    continue;
                }

                // a typed query names no plugins, they are taken from the preview of the plugin
                ExtensionPreview feature = new ExtensionPreview(
                    pluginPreview.apiPluginId(),
                    wanted.getKey(),
                    pluginPreview.implPluginId(),
                    wanted.getValue()
                );
                try {
                    @SuppressWarnings("unchecked")
                    ExtensionPreviewAcceptor<Object> acceptor = (ExtensionPreviewAcceptor<Object>) PluginAdvertiserImpl.findAcceptor(feature);
                    if (acceptor.accept(pluginPreview, feature)) {
                        return true;
                    }
                }
                catch (Exception e) {
                    LOG.error(e);
                }
            }
        }
        return false;
    }

    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                current.append(c);
            }
            else if (Character.isWhitespace(c) && !quoted) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            }
            else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static String quote(String value) {
        return value.indexOf(' ') >= 0 ? '"' + value + '"' : value;
    }

    private static String unquote(String value) {
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }
}
