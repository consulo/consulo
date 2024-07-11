/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.language.editor.internal;

import consulo.language.editor.scope.AnalysisScope;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import jakarta.annotation.Nullable;

public final record ModelScopeItemView(RadioButton button,
                                       @Nullable Component additionalComponent,
                                       ModelScopeItem model,
                                       @AnalysisScope.Type int scopeId) {

}
