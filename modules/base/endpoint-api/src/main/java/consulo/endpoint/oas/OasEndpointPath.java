// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import consulo.endpoint.url.UrlPath;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class OasEndpointPath {
    private final String myPath;
    private final @Nullable String mySummary;
    private final Collection<OasOperation> myOperations;
    private final String myAbsolutePath;

    public OasEndpointPath(String path, @Nullable String summary, Collection<OasOperation> operations) {
        myPath = path;
        mySummary = summary;
        myOperations = operations;
        myAbsolutePath = path.startsWith("/") ? path : "/" + path;
    }

    public String getPath() {
        return myPath;
    }

    public @Nullable String getSummary() {
        return mySummary;
    }

    public Collection<OasOperation> getOperations() {
        return myOperations;
    }

    public String getAbsolutePath() {
        return myAbsolutePath;
    }

    public static final class Builder {
        private final String myPath;
        private @Nullable String mySummary;
        private Collection<OasOperation> myOperations = List.of();

        public Builder(String path) {
            myPath = path;
        }

        public Builder(UrlPath urlPath) {
            this(urlPath.getPresentation(OasModelUtil.OPEN_API_PRESENTATION));
        }

        public @Nullable String getSummary() {
            return mySummary;
        }

        public void setSummary(@Nullable String summary) {
            mySummary = summary;
        }

        public Collection<OasOperation> getOperations() {
            return myOperations;
        }

        public void setOperations(Collection<OasOperation> operations) {
            myOperations = operations;
        }

        public OasEndpointPath build() {
            return build(null);
        }

        public OasEndpointPath build(@Nullable Consumer<Builder> block) {
            if (block != null) {
                block.accept(this);
            }
            return new OasEndpointPath(myPath, mySummary, myOperations);
        }
    }
}
