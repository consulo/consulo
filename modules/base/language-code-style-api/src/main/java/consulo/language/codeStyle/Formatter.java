// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

@ServiceAPI(ComponentScope.APPLICATION)
public interface Formatter extends IndentFactory, WrapFactory, AlignmentFactory, SpacingFactory, FormattingModelFactory {
    static Formatter getInstance() {
        return Application.get().getInstance(Formatter.class);
    }
}
