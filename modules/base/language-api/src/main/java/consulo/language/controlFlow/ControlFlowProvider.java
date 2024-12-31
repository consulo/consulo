// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.controlFlow;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ControlFlowProvider {
    ExtensionPointName<ControlFlowProvider> EP_NAME = ExtensionPointName.create(ControlFlowProvider.class);

    /**
     * @return a control flow which contains the <code>element</code> or null if the <code>element</code> is not supported by provider
     */
    @Nullable
    ControlFlow getControlFlow(@Nonnull PsiElement element);

    /**
     * @param instruction belongs to a control flow which was created by the provider
     * @return an additional language-specific representation of the <code>instruction</code>
     */
    @Nullable
    String getAdditionalInfo(@Nonnull Instruction instruction);
}
