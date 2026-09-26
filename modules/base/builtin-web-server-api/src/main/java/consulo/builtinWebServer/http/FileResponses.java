// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.http;

import consulo.http.HttpMethod;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileResponses {
    private static final Logger LOG = Logger.getInstance(FileResponses.class);

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String NO_CACHE = "no-cache";

    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String RANGE = "Range";
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String LAST_MODIFIED = "Last-Modified";
    private static final String CONTENT_RANGE = "Content-Range";

    private static final Pattern RANGE_HEADER = Pattern.compile("bytes=(\\d+)?-(\\d+)?");

    private static final DateTimeFormatter HTTP_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).withZone(ZoneOffset.UTC);

    private static volatile @Nullable Map<String, String> ourFileExtToMimeType;

    private FileResponses() {
    }

    public static String getContentType(String path) {
        String extension = PathUtil.getFileExtension(path);
        if (extension == null) {
            return DEFAULT_CONTENT_TYPE;
        }
        String contentType = getFileExtToMimeType().get(extension);
        return contentType == null ? DEFAULT_CONTENT_TYPE : contentType;
    }

    public static @Nullable HttpResponse checkCache(HttpRequest request, long lastModified) {
        String ifModifiedSince = request.getHeaderValue(IF_MODIFIED_SINCE);
        if (ifModifiedSince == null) {
            return null;
        }

        Long ifModified = parseHttpDate(ifModifiedSince);
        if (ifModified != null && ifModified >= Math.floorDiv(lastModified, 1000L) * 1000L) {
            return HttpResponse.create(HttpURLConnection.HTTP_NOT_MODIFIED, null, null);
        }
        return null;
    }

    public static HttpResponse prepareSend(HttpRequest request, long lastModified, String filename, byte @Nullable [] content) {
        if (request.getHeaderValue(RANGE) == null) {
            HttpResponse notModified = checkCache(request, lastModified);
            if (notModified != null) {
                return notModified;
            }
        }

        byte @Nullable [] body = request.method() == HttpMethod.HEAD ? null : content;
        return HttpResponse.create(HttpURLConnection.HTTP_OK, getContentType(filename), body)
            .withHeader(CACHE_CONTROL, NO_CACHE)
            .withHeader(LAST_MODIFIED, formatHttpDate(lastModified));
    }

    public static HttpResponse sendFile(HttpRequest request, Path file) throws IOException {
        return sendFile(request, file, null);
    }

    public static HttpResponse sendFile(HttpRequest request, Path file, byte @Nullable [] extraSuffix) throws IOException {
        String rangeHeader = request.getHeaderValue(RANGE);
        long lastModified;
        long fileSize;
        try {
            lastModified = Files.getLastModifiedTime(file).toMillis();
            if (rangeHeader == null && (extraSuffix == null || extraSuffix.length == 0)) {
                HttpResponse notModified = checkCache(request, lastModified);
                if (notModified != null) {
                    return notModified;
                }
            }

            fileSize = Files.size(file);
        }
        catch (NoSuchFileException ignored) {
            return HttpResponse.notFound();
        }

        long responseLength = fileSize + (extraSuffix == null ? 0 : extraSuffix.length);
        ByteRange requestedRange = parseRange(rangeHeader, responseLength);
        ByteRange range = requestedRange == null ? new ByteRange(0, responseLength) : requestedRange;
        boolean isPartialContent = !(range.start() == 0L && range.end() == responseLength);

        ByteRange fileRange = range.intersect(0, fileSize);
        long count = fileRange == null ? 0L : fileRange.length();

        byte[] suffix = ArrayUtil.EMPTY_BYTE_ARRAY;
        if (extraSuffix != null) {
            ByteRange suffixRange = range.intersect(fileSize, fileSize + extraSuffix.length);
            if (suffixRange != null && suffixRange.length() > 0) {
                suffix = Arrays.copyOfRange(extraSuffix, (int)(suffixRange.start() - fileSize), (int)(suffixRange.end() - fileSize));
            }
        }

        Path fileName = file.getFileName();
        String contentType = getContentType(fileName == null ? file.toString() : fileName.toString());
        HttpFileRegion region = new HttpFileRegion(file, range.start(), count, suffix);

        int code = isPartialContent ? HttpURLConnection.HTTP_PARTIAL : HttpURLConnection.HTTP_OK;
        HttpResponse response = HttpResponse.file(code, contentType, region).withHeader(CACHE_CONTROL, NO_CACHE);
        if (isPartialContent) {
            return response.withHeader(CONTENT_RANGE, "bytes " + range.start() + "-" + (range.end() - 1) + "/" + responseLength);
        }
        return response.withHeader(LAST_MODIFIED, formatHttpDate(lastModified));
    }

    private static Map<String, String> getFileExtToMimeType() {
        Map<String, String> map = ourFileExtToMimeType;
        if (map == null) {
            map = loadFileExtToMimeType();
            ourFileExtToMimeType = map;
        }
        return map;
    }

    private static Map<String, String> loadFileExtToMimeType() {
        Map<String, String> map = new HashMap<>();
        try (InputStream stream = FileResponses.class.getResourceAsStream("mime-types.csv")) {
            if (stream == null) {
                LOG.error("mime-types.csv is not found");
                return Map.of();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.isBlank()) {
                    continue;
                }

                int commaIndex = line.indexOf(',');
                map.put(line.substring(0, commaIndex), line.substring(commaIndex + 1));
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
        return Map.copyOf(map);
    }

    private static @Nullable Long parseHttpDate(String value) {
        try {
            return OffsetDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
        }
        catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String formatHttpDate(long millis) {
        return HTTP_DATE_FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    private static @Nullable ByteRange parseRange(@Nullable String header, long size) {
        if (StringUtil.isEmpty(header)) {
            return null;
        }

        Matcher m = RANGE_HEADER.matcher(header);
        if (!m.matches()) {
            LOG.warn("Range header is invalid: " + header);
            return null;
        }

        String startGroup = m.group(1);
        String endGroup = m.group(2);
        try {
            if (StringUtil.isEmpty(startGroup)) {
                if (StringUtil.isEmpty(endGroup)) {
                    LOG.warn("Range header is invalid: " + header);
                    return null;
                }

                long suffixLength = Long.parseLong(endGroup);
                if (suffixLength <= 0) {
                    LOG.warn("Range header is invalid: " + header);
                    return null;
                }
                return new ByteRange(Math.max(0L, size - suffixLength), size);
            }

            long start = Long.parseLong(startGroup);
            long end = StringUtil.isEmpty(endGroup) ? size - 1 : Long.parseLong(endGroup);
            if (end < start) {
                LOG.warn("start (" + start + ") must be greater than end (" + end + ")");
                return null;
            }
            if (end >= size) {
                LOG.warn("end (" + end + ") must be lesser than size (" + size + ")");
                return null;
            }
            return new ByteRange(start, end + 1);
        }
        catch (NumberFormatException e) {
            LOG.warn("Range header is invalid: " + header);
            return null;
        }
    }
}
