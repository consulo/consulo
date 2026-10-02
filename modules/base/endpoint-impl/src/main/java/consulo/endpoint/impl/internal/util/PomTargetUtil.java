// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.impl.psi.DelegatePsiTarget;
import consulo.language.pom.PomRenameableTarget;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiTarget;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public final class PomTargetUtil {
    private PomTargetUtil() {
    }

    public static PomRenameableTarget<?> toPomRenameableTarget(
        PomTarget delegate,
        String nameIfNeeded,
        @Nullable Image icon,
        LocalizeValue typeName
    ) {
        if (delegate instanceof PomRenameableTarget<?> renameableTarget) {
            return renameableTarget;
        }
        if (delegate instanceof PsiTarget psiTarget) {
            return new DelegatePsiTargetSimpleNamePomTarget(psiTarget, nameIfNeeded, icon, typeName);
        }
        return new DelegateSimpleNamePomTarget(delegate, nameIfNeeded, icon, typeName);
    }

    @RequiredReadAction
    public static PomRenameableTarget<?> toPomRenameableTarget(
        PsiElement delegate,
        String nameIfNeeded,
        @Nullable Image icon,
        LocalizeValue typeName
    ) {
        if (delegate instanceof PomRenameableTarget<?> renameableTarget) {
            return renameableTarget;
        }
        if (delegate instanceof PomTargetPsiElement pomTargetPsiElement) {
            if (pomTargetPsiElement.getTarget() instanceof PomRenameableTarget<?> renameableTarget) {
                return renameableTarget;
            }
            return toPomRenameableTarget(pomTargetPsiElement.getTarget(), nameIfNeeded, icon, typeName);
        }
        return toPomRenameableTarget(
            new DelegatePsiTarget(delegate),
            nameIfNeeded,
            icon != null ? icon : IconDescriptorUpdaters.getIcon(delegate, 0),
            typeName
        );
    }
}
