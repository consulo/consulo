// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public final class OasParameter {
    private final String myName;
    private final OasParameterIn myInPlace;
    private final @Nullable String myDescription;
    private final boolean myRequired;
    private final boolean myDeprecated;
    private final @Nullable OasSchema mySchema;
    private final @Nullable OasParameterStyle myStyle;

    public OasParameter(
        String name,
        OasParameterIn inPlace,
        @Nullable String description,
        boolean isRequired,
        boolean isDeprecated,
        @Nullable OasSchema schema,
        @Nullable OasParameterStyle style
    ) {
        myName = name;
        myInPlace = inPlace;
        myDescription = description;
        myRequired = isRequired;
        myDeprecated = isDeprecated;
        mySchema = schema;
        myStyle = style;
    }

    public String getName() {
        return myName;
    }

    public OasParameterIn getInPlace() {
        return myInPlace;
    }

    public @Nullable String getDescription() {
        return myDescription;
    }

    public boolean isRequired() {
        return myRequired;
    }

    public boolean isDeprecated() {
        return myDeprecated;
    }

    public @Nullable OasSchema getSchema() {
        return mySchema;
    }

    public @Nullable OasParameterStyle getStyle() {
        return myStyle;
    }

    public static final class Builder {
        private final String myName;
        private final OasParameterIn myInPlace;
        private @Nullable String myDescription;
        private boolean myRequired;
        private boolean myDeprecated;
        private @Nullable OasSchema mySchema;
        private @Nullable OasParameterStyle myStyle;

        public Builder(String name, OasParameterIn inPlace) {
            myName = name;
            myInPlace = inPlace;
        }

        public String getName() {
            return myName;
        }

        public @Nullable String getDescription() {
            return myDescription;
        }

        public void setDescription(@Nullable String description) {
            myDescription = description;
        }

        public boolean isRequired() {
            return myRequired;
        }

        public void setRequired(boolean required) {
            myRequired = required;
        }

        public boolean isDeprecated() {
            return myDeprecated;
        }

        public void setDeprecated(boolean deprecated) {
            myDeprecated = deprecated;
        }

        public @Nullable OasSchema getSchema() {
            return mySchema;
        }

        public void setSchema(@Nullable OasSchema schema) {
            mySchema = schema;
        }

        public @Nullable OasParameterStyle getStyle() {
            return myStyle;
        }

        public void setStyle(@Nullable OasParameterStyle style) {
            myStyle = style;
        }

        public OasParameter build() {
            return build(null);
        }

        public OasParameter build(@Nullable Consumer<Builder> block) {
            if (block != null) {
                block.accept(this);
            }
            return new OasParameter(myName, myInPlace, myDescription, myRequired, myDeprecated, mySchema, myStyle);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Builder that)) {
                return false;
            }
            return myName.equals(that.myName) && myInPlace == that.myInPlace;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myName, myInPlace);
        }

        @Override
        public String toString() {
            return "Builder(name=" + myName + ", inPlace=" + myInPlace + ")";
        }
    }
}
