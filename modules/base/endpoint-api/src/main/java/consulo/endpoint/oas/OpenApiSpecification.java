// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import consulo.endpoint.mime.MimeTypeConstants;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public final class OpenApiSpecification {
    private final Collection<OasEndpointPath> myPaths;
    private final @Nullable OasComponents myComponents;
    private final @Nullable List<OasTag> myTags;

    public OpenApiSpecification(Collection<OasEndpointPath> paths) {
        this(paths, null);
    }

    public OpenApiSpecification(Collection<OasEndpointPath> paths, @Nullable OasComponents components) {
        this(paths, components, null);
    }

    public OpenApiSpecification(Collection<OasEndpointPath> paths, @Nullable OasComponents components, @Nullable List<OasTag> tags) {
        myPaths = paths;
        myComponents = components;
        myTags = tags;
    }

    public Collection<OasEndpointPath> getPaths() {
        return myPaths;
    }

    public @Nullable OasComponents getComponents() {
        return myComponents;
    }

    public @Nullable List<OasTag> getTags() {
        return myTags;
    }

    public boolean isEmpty() {
        return myPaths.iterator().hasNext();
    }

    public @Nullable OasSchema findSinglePathRequestBodyData() {
        return findSinglePathRequestBodyData(MimeTypeConstants.APPLICATION_JSON);
    }

    public @Nullable OasSchema findSinglePathRequestBodyData(String contentType) {
        for (OasEndpointPath path : myPaths) {
            for (OasOperation operation : path.getOperations()) {
                OasRequestBody requestBody = operation.getRequestBody();
                OasSchema schema = requestBody != null ? requestBody.getContent().get(contentType) : null;
                if (schema != null) {
                    return schema;
                }
            }
        }
        return null;
    }
}
