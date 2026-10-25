// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.editor;

public interface ComplexPathSerializer<P extends ComplexPath> {
    String serialize(P path);

    P deserialize(String rawPath);
}
