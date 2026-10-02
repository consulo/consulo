// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OasResponse {
    private final String myCode;
    private final @Nullable String myDescription;
    private final Map<String, OasMediaTypeObject> myContent;
    private final List<OasHeader> myHeaders;

    public OasResponse(String code, @Nullable String description) {
        this(code, description, Map.of());
    }

    public OasResponse(String code, @Nullable String description, Map<String, OasMediaTypeObject> content) {
        this(code, description, content, List.of());
    }

    public OasResponse(String code, @Nullable String description, Map<String, OasMediaTypeObject> content, List<OasHeader> headers) {
        myCode = code;
        myDescription = description;
        myContent = content;
        myHeaders = headers;
    }

    public String getCode() {
        return myCode;
    }

    public @Nullable String getDescription() {
        return myDescription;
    }

    public Map<String, OasMediaTypeObject> getContent() {
        return myContent;
    }

    public List<OasHeader> getHeaders() {
        return myHeaders;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OasResponse that)) {
            return false;
        }
        return myCode.equals(that.myCode)
            && Objects.equals(myDescription, that.myDescription)
            && myContent.equals(that.myContent)
            && myHeaders.equals(that.myHeaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myCode, myDescription, myContent, myHeaders);
    }

    @Override
    public String toString() {
        return "OasResponse(code=" + myCode + ", description=" + myDescription + ", content=" + myContent + ", headers=" + myHeaders + ")";
    }
}
