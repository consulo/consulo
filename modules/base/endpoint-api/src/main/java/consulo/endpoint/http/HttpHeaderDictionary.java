// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import consulo.endpoint.mime.MimeTypeConstants;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HttpHeaderDictionary {
    private static final List<String> ENCODINGS = List.of(
        "compress", "deflate", "exi", "gzip", "identity", "pack200-gzip", "br", "bzip2", "lzma", "peerdist", "sdch", "xpress", "xz"
    );

    private static final List<String> KNOWN_EXTRA_HEADERS = List.of(
        "X-Correlation-ID",
        "X-Csrf-Token",
        "X-Forwarded-For",
        "X-Forwarded-Host",
        "X-Forwarded-Proto",
        "X-Http-Method-Override",
        "X-Request-ID",
        "X-Requested-With",
        "X-Total-Count",
        "X-User-Agent"
    );

    private static final Map<String, List<String>> HEADER_OPTION_NAMES = new HashMap<>();

    private static @Nullable Map<String, HttpHeaderDocumentation> ourHeaders = null;
    private static @Nullable Map<String, List<String>> ourHeaderValues;

    static {
        HEADER_OPTION_NAMES.put("Content-Type", List.of("charset", "boundary"));
    }

    private HttpHeaderDictionary() {
    }

    public static synchronized Map<String, HttpHeaderDocumentation> getHeaders() {
        Map<String, HttpHeaderDocumentation> headers = ourHeaders;
        if (headers == null) {
            headers = readHeaders();
            for (String extraHeader : KNOWN_EXTRA_HEADERS) {
                headers.put(extraHeader, new HttpHeaderDocumentation(extraHeader));
            }
            ourHeaders = headers;
        }
        return headers;
    }

    public static @Nullable HttpHeaderDocumentation getDocumentation(String fieldName) {
        Map<String, HttpHeaderDocumentation> headers = getHeaders();
        return headers.get(fieldName);
    }

    private static Map<String, HttpHeaderDocumentation> readHeaders() {
        Map<String, HttpHeaderDocumentation> result = new HashMap<>();
        InputStream stream = HttpHeaderDictionary.class.getResourceAsStream("headers-doc.json");
        try {
            String file = stream != null ? FileUtil.loadTextAndClose(stream) : "";

            if (StringUtil.isNotEmpty(file)) {
                JsonElement root = JsonParser.parseString(file);
                if (root.isJsonArray()) {
                    JsonArray array = root.getAsJsonArray();
                    for (JsonElement element : array) {
                        if (element.isJsonObject()) {
                            HttpHeaderDocumentation header = HttpHeaderDocumentation.read(element.getAsJsonObject());
                            if (header != null) {
                                result.put(header.getName(), header);
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            Logger.getInstance(HttpHeaderDictionary.class).error(e);
        }
        return result;
    }

    public static Collection<String> getHeaderValues(Project project, String headerName) {
        Map<String, List<String>> headerValues = ourHeaderValues;
        if (headerValues == null) {
            headerValues = readHeaderValues();
            ourHeaderValues = headerValues;
        }

        return headerValues.containsKey(headerName) ? headerValues.get(headerName) : Collections.emptyList();
    }

    public static Collection<String> getHeaderOptionNames(String headerName) {
        return HEADER_OPTION_NAMES.containsKey(headerName) ? HEADER_OPTION_NAMES.get(headerName) : Collections.emptyList();
    }

    private static Map<String, List<String>> readHeaderValues() {
        List<String> mimeTypes = Arrays.asList(MimeTypeConstants.PREDEFINED_MIME_VARIANTS);
        Map<String, List<String>> values = new HashMap<>();
        values.put("Accept", mimeTypes);
        values.put("Content-Type", mimeTypes);
        values.put("Accept-Encoding", ENCODINGS);
        return values;
    }
}
