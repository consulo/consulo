// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class OasOperation {
    private final OasHttpMethod myMethod;
    private final List<String> myTags;
    private final @Nullable String myDescription;
    private final @Nullable String mySummary;
    private final @Nullable String myOperationId;
    private final boolean myDeprecated;
    private final Collection<OasParameter> myParameters;
    private final @Nullable OasRequestBody myRequestBody;
    private final Collection<OasResponse> myResponses;

    public OasOperation(
        OasHttpMethod method,
        List<String> tags,
        @Nullable String description,
        @Nullable String summary,
        @Nullable String operationId,
        boolean isDeprecated,
        Collection<OasParameter> parameters,
        @Nullable OasRequestBody requestBody,
        Collection<OasResponse> responses
    ) {
        myMethod = method;
        myTags = tags;
        myDescription = description;
        mySummary = summary;
        myOperationId = operationId;
        myDeprecated = isDeprecated;
        myParameters = parameters;
        myRequestBody = requestBody;
        myResponses = responses;
    }

    public OasHttpMethod getMethod() {
        return myMethod;
    }

    public List<String> getTags() {
        return myTags;
    }

    public @Nullable String getDescription() {
        return myDescription;
    }

    public @Nullable String getSummary() {
        return mySummary;
    }

    public @Nullable String getOperationId() {
        return myOperationId;
    }

    public boolean isDeprecated() {
        return myDeprecated;
    }

    public Collection<OasParameter> getParameters() {
        return myParameters;
    }

    public @Nullable OasRequestBody getRequestBody() {
        return myRequestBody;
    }

    public Collection<OasResponse> getResponses() {
        return myResponses;
    }

    public static final class Builder {
        private final OasHttpMethod myMethod;
        private List<String> myTags = List.of();
        private @Nullable String mySummary;
        private @Nullable String myDescription;
        private @Nullable String myOperationId;
        private boolean myDeprecated;
        private List<OasParameter> myParameters = List.of();
        private @Nullable OasRequestBody myRequestBody;
        private List<OasResponse> myResponses = List.of();

        public Builder(OasHttpMethod method) {
            myMethod = method;
        }

        public Builder(String method) {
            this(httpMethodOrGet(method));
        }

        private static OasHttpMethod httpMethodOrGet(String method) {
            OasHttpMethod httpMethod = OasModelUtil.getHttpMethodByName(method);
            return httpMethod != null ? httpMethod : OasHttpMethod.GET;
        }

        public List<String> getTags() {
            return myTags;
        }

        public void setTags(List<String> tags) {
            myTags = tags;
        }

        /**
         * A short summary of what the operation does.
         */
        public @Nullable String getSummary() {
            return mySummary;
        }

        public void setSummary(@Nullable String summary) {
            mySummary = summary;
        }

        /**
         * A verbose explanation of the operation behavior. Supports CommonMark syntax.
         */
        public @Nullable String getDescription() {
            return myDescription;
        }

        public void setDescription(@Nullable String description) {
            myDescription = description;
        }

        public @Nullable String getOperationId() {
            return myOperationId;
        }

        public void setOperationId(@Nullable String operationId) {
            myOperationId = operationId;
        }

        public boolean isDeprecated() {
            return myDeprecated;
        }

        public void setDeprecated(boolean deprecated) {
            myDeprecated = deprecated;
        }

        public List<OasParameter> getParameters() {
            return myParameters;
        }

        public void setParameters(List<OasParameter> parameters) {
            myParameters = parameters;
        }

        public @Nullable OasRequestBody getRequestBody() {
            return myRequestBody;
        }

        public void setRequestBody(@Nullable OasRequestBody requestBody) {
            myRequestBody = requestBody;
        }

        public List<OasResponse> getResponses() {
            return myResponses;
        }

        public void setResponses(List<OasResponse> responses) {
            myResponses = responses;
        }

        public OasOperation build() {
            return build(null);
        }

        public OasOperation build(@Nullable Consumer<Builder> block) {
            if (block != null) {
                block.accept(this);
            }
            return new OasOperation(
                myMethod,
                myTags,
                myDescription,
                mySummary,
                myOperationId,
                myDeprecated,
                myParameters,
                myRequestBody,
                myResponses
            );
        }
    }
}
