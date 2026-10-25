// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.editor;

import com.google.gson.Gson;

public class GsonComplexPathSerializer<P extends ComplexPath> implements ComplexPathSerializer<P> {
    private final Class<P> myPathClass;
    private final Gson myGson;

    public GsonComplexPathSerializer(Class<P> pathClass) {
        this(pathClass, new Gson());
    }

    public GsonComplexPathSerializer(Class<P> pathClass, Gson gson) {
        myPathClass = pathClass;
        myGson = gson;
    }

    @Override
    public String serialize(P path) {
        return myGson.toJson(path);
    }

    @Override
    public P deserialize(String rawPath) {
        return myGson.fromJson(rawPath, myPathClass);
    }
}
