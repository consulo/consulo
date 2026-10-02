// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.application.util.query.Plow;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.sem.SemElement;
import consulo.language.sem.SemKey;

public interface RenameableSemElement extends SemElement {
    SemKey<RenameableSemElement> RENAMEABLE_SEM_KEY = SemKey.createKey("RenameableSemElement");

    String getName();

    default Plow<LookupElement> getNameVariants() {
        return Plow.empty();
    }
}
