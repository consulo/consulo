// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

public final class HttpHeaderDocumentation {
    private static final String CC_LICENSE =
        " is licensed under <a href=\"https://creativecommons.org/licenses/by-sa/2.5/\">CC-BY-SA 2.5</a>.";

    private static final String URL_PREFIX = "https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/";
    private static final String RFC_PREFIX = "https://tools.ietf.org/html/rfc";

    private final String myName;
    private final String myRfc;
    private final String myRfcTitle;
    private final String myDescription;
    private final boolean myIsDeprecated;

    private HttpHeaderDocumentation(String name, String rfc, String rfcTitle, String description, boolean isDeprecated) {
        myName = name;
        myRfc = rfc;
        myRfcTitle = rfcTitle;
        myDescription = description;
        myIsDeprecated = isDeprecated;
    }

    HttpHeaderDocumentation(String name) {
        this(name, "", "", "", false);
    }

    public static @Nullable HttpHeaderDocumentation read(JsonObject obj) {
        String name = getAsString(obj, "name");
        if (StringUtil.isNotEmpty(name)) {
            String rfcTitle = getAsString(obj, "rfc-title");
            String rfcRef = getAsString(obj, "rfc-ref");
            String descr = getAsString(obj, "descr");

            JsonElement obsolete = obj.get("obsolete");
            boolean isObsolete = obsolete != null && obsolete.isJsonPrimitive() && obsolete.getAsBoolean();
            return new HttpHeaderDocumentation(name, rfcRef, rfcTitle, descr, isObsolete);
        }
        return null;
    }

    private static String getAsString(JsonObject obj, String name) {
        JsonElement element = obj.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
    }

    public @Nullable String generateDoc() {
        if (StringUtil.isNotEmpty(myDescription)) {
            StringBuilder out = new StringBuilder().append(myDescription);
            if (StringUtil.isNotEmpty(myRfc) && StringUtil.isNotEmpty(myRfcTitle)) {
                out.append("<br/><br/>");
                out.append("<a href=\"").append(RFC_PREFIX).append(myRfc).append("\">").append(myRfcTitle).append("</a>");
            }

            String url = getUrl();
            out.append("<br/><br/>");
            out.append("<a href=\"").append(url).append("\">").append(getName()).append("</a> by ");
            out.append("<a href=\"").append(url).append("$history").append("\">").append("Mozilla Contributors").append("</a>");
            out.append(CC_LICENSE);
            return out.toString();
        }
        return null;
    }

    public String getName() {
        return myName;
    }

    public boolean isDeprecated() {
        return myIsDeprecated;
    }

    public String getDescription() {
        return myDescription;
    }

    public String getUrl() {
        return URL_PREFIX + getName();
    }
}
