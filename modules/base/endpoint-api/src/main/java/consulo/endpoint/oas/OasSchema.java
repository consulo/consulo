// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class OasSchema {
    public static final String SCHEMA_DEFINITION_STUB_REFERENCE = "#/components/schemas/__SchemaDefinitionStub";

    private final @Nullable OasSchemaType myType;
    private final @Nullable OasSchemaFormat myFormat;
    private final @Nullable String myDefault;
    private final @Nullable OasSchema myItems;
    private final @Nullable List<OasProperty> myProperties;
    private final @Nullable String myReference;
    private final @Nullable List<String> myEnum;
    private final @Nullable List<String> myRequired;
    private final boolean myNullable;
    private final boolean myItemsUnique;

    public OasSchema() {
        this(null, null, null, null, null, null, null, null, false, false);
    }

    public OasSchema(
        @Nullable OasSchemaType type,
        @Nullable OasSchemaFormat format,
        @Nullable String defaultValue,
        @Nullable OasSchema items,
        @Nullable List<OasProperty> properties,
        @Nullable String reference,
        @Nullable List<String> enumValues,
        @Nullable List<String> required,
        boolean isNullable,
        boolean areItemsUnique
    ) {
        myType = type;
        myFormat = format;
        myDefault = defaultValue;
        myItems = items;
        myProperties = properties;
        myReference = reference;
        myEnum = enumValues;
        myRequired = required;
        myNullable = isNullable;
        myItemsUnique = areItemsUnique;
    }

    public @Nullable OasSchemaType getType() {
        return myType;
    }

    public @Nullable OasSchemaFormat getFormat() {
        return myFormat;
    }

    public @Nullable String getDefault() {
        return myDefault;
    }

    public @Nullable OasSchema getItems() {
        return myItems;
    }

    public @Nullable List<OasProperty> getProperties() {
        return myProperties;
    }

    public @Nullable String getReference() {
        return myReference;
    }

    public @Nullable List<String> getEnum() {
        return myEnum;
    }

    public @Nullable List<String> getRequired() {
        return myRequired;
    }

    public boolean isNullable() {
        return myNullable;
    }

    public boolean getAreItemsUnique() {
        return myItemsUnique;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OasSchema that)) {
            return false;
        }
        return myNullable == that.myNullable
            && myItemsUnique == that.myItemsUnique
            && myType == that.myType
            && myFormat == that.myFormat
            && Objects.equals(myDefault, that.myDefault)
            && Objects.equals(myItems, that.myItems)
            && Objects.equals(myProperties, that.myProperties)
            && Objects.equals(myReference, that.myReference)
            && Objects.equals(myEnum, that.myEnum)
            && Objects.equals(myRequired, that.myRequired);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            myType,
            myFormat,
            myDefault,
            myItems,
            myProperties,
            myReference,
            myEnum,
            myRequired,
            myNullable,
            myItemsUnique
        );
    }

    @Override
    public String toString() {
        return "OasSchema(type=" + myType +
            ", format=" + myFormat +
            ", default=" + myDefault +
            ", items=" + myItems +
            ", properties=" + myProperties +
            ", reference=" + myReference +
            ", enum=" + myEnum +
            ", required=" + myRequired +
            ", isNullable=" + myNullable +
            ", areItemsUnique=" + myItemsUnique + ")";
    }

    public static final class Builder {
        private final OasSchemaType myType;
        private @Nullable OasSchemaFormat myFormat;
        private @Nullable String myReference;
        private @Nullable OasSchema myItems;
        private @Nullable Map<String, OasSchema> myProperties;

        public Builder(OasSchemaType type) {
            myType = type;
        }

        public OasSchemaType getType() {
            return myType;
        }

        public @Nullable OasSchemaFormat getFormat() {
            return myFormat;
        }

        public void setFormat(@Nullable OasSchemaFormat format) {
            myFormat = format;
        }

        public @Nullable String getReference() {
            return myReference;
        }

        public void setReference(@Nullable String reference) {
            myReference = reference;
        }

        public @Nullable OasSchema getItems() {
            return myItems;
        }

        public void setItems(@Nullable OasSchema items) {
            myItems = items;
        }

        public @Nullable Map<String, OasSchema> getProperties() {
            return myProperties;
        }

        public void setProperties(@Nullable Map<String, OasSchema> properties) {
            myProperties = properties;
        }

        public OasSchema build() {
            return build(null);
        }

        public OasSchema build(@Nullable Consumer<Builder> block) {
            if (block != null) {
                block.accept(this);
            }

            List<OasProperty> properties = null;
            if (myProperties != null) {
                properties = new ArrayList<>(myProperties.size());
                for (Map.Entry<String, OasSchema> entry : myProperties.entrySet()) {
                    properties.add(new OasProperty(entry.getKey(), entry.getValue()));
                }
            }

            return new OasSchema(myType, myFormat, null, myItems, properties, myReference, null, null, false, false);
        }
    }
}
