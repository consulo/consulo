// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.mime;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.mime.MimeTypeReference;
import consulo.language.inject.ReferenceInjector;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.util.ProcessingContext;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

@ExtensionImpl
public class MimeTypeReferenceInjector extends ReferenceInjector {
    @Override
    public String getId() {
        return "mime-type-reference";
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesPpweb();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return EndpointLocalize.injectMimeTypeReference();
    }

    @Override
    public PsiReference[] getReferences(PsiElement element, ProcessingContext context, TextRange range) {
        return MimeTypeReference.forElement(element, range, true);
    }
}
