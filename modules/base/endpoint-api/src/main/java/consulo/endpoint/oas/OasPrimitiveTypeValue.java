// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.oas;

public sealed abstract class OasPrimitiveTypeValue<T> implements OasExampleValue permits OasStringValue, OasNumberValue, OasBooleanValue {
    private final T myValue;

    protected OasPrimitiveTypeValue(T value) {
        myValue = value;
    }

    public T getValue() {
        return myValue;
    }
}
