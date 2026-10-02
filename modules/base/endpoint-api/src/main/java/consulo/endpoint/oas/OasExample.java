// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public final class OasExample {
    private final @Nullable String mySummary;
    private final @Nullable String myDescription;
    private final OasExampleValue myValue;

    public OasExample(@Nullable String summary, @Nullable String description, OasExampleValue value) {
        mySummary = summary;
        myDescription = description;
        myValue = value;
    }

    public @Nullable String getSummary() {
        return mySummary;
    }

    public @Nullable String getDescription() {
        return myDescription;
    }

    public OasExampleValue getValue() {
        return myValue;
    }

    public static final class Builder {
        private final OasExampleValue myValue;
        private @Nullable String mySummary;
        private @Nullable String myDescription;

        public Builder(OasExampleValue value) {
            myValue = value;
        }

        public OasExampleValue getValue() {
            return myValue;
        }

        public @Nullable String getSummary() {
            return mySummary;
        }

        public void setSummary(@Nullable String summary) {
            mySummary = summary;
        }

        public @Nullable String getDescription() {
            return myDescription;
        }

        public void setDescription(@Nullable String description) {
            myDescription = description;
        }

        public OasExample build() {
            return build(null);
        }

        public OasExample build(@Nullable Consumer<Builder> block) {
            if (block != null) {
                block.accept(this);
            }
            return new OasExample(mySummary, myDescription, myValue);
        }
    }
}
