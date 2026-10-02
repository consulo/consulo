// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.intention;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.endpoint.intention.EndpointIntentionPriorityComparableAction;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.UrlConstants;
import consulo.endpoint.url.UrlResolveRequest;
import consulo.endpoint.url.reference.UrlPathReference;
import consulo.language.editor.inspection.FileModifier;
import consulo.language.editor.intention.HighPriorityAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.language.impl.psi.path.WebReference;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.webBrowser.BrowserUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ExtensionImpl
@IntentionMetaData(ignoreId = "platform.open.in.web.browser", fileExtensions = "txt", categories = "Microservices")
public class OpenInWebBrowserIntention extends PsiElementBaseIntentionAction
    implements Iconable, HighPriorityAction, EndpointIntentionPriorityComparableAction {

    @Override
    public LocalizeValue getText() {
        return EndpointLocalize.microservicesOpenInWebBrowserIntentionText();
    }

    public LocalizeValue getFamilyName() {
        return EndpointLocalize.microservicesOpenInWebBrowserIntentionFamilyName();
    }

    @Override
    public Image getIcon(int flags) {
        return PlatformIconGroup.runconfigurationsWeb_app();
    }

    @Override
    @RequiredReadAction
    public void invoke(Project project, Editor editor, PsiElement element) {
        UrlPathReference urlPathReference = null;
        for (UrlPathReference reference : findUrlPathReferences(element)) {
            if (reference.isAtEnd()) {
                urlPathReference = reference;
                break;
            }
        }
        if (urlPathReference == null) {
            return;
        }
        Iterator<UrlResolveRequest> requests = urlPathReference.getContext().getResolveRequests().iterator();
        if (!requests.hasNext()) {
            return;
        }
        UrlResolveRequest request = requests.next();

        String scheme = request.getSchemeHint() != null ? request.getSchemeHint() : UrlConstants.HTTP_SCHEME;
        String authority = request.getAuthorityHint() != null ? request.getAuthorityHint() : UrlConstants.LOCALHOST;
        String path = request.getPath().getPresentation();

        BrowserUtil.browse(scheme + authority + path);
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(Project project, Editor editor, PsiElement element) {
        return !findUrlPathReferences(element).isEmpty();
    }

    @Override
    public @Nullable PsiElement getElementToMakeWritable(PsiFile currentFile) {
        return null;
    }

    @Override
    public @Nullable FileModifier getFileModifierForPreview(PsiFile target) {
        return null;
    }

    @RequiredReadAction
    private static List<UrlPathReference> findUrlPathReferences(PsiElement element) {
        List<UrlPathReference> result = new ArrayList<>();
        PsiElement current = element;
        for (int i = 0; i < 3 && current != null; i++) {
            if (WebReference.isWebReferenceWorthy(current)) {
                for (PsiReference reference : current.getReferences()) {
                    UrlPathReference urlPathReference = PsiReferenceUtil.findReferenceOfClass(reference, UrlPathReference.class);
                    if (urlPathReference != null) {
                        result.add(urlPathReference);
                    }
                }
            }
            current = current instanceof PsiFile ? null : current.getParent();
        }
        return result;
    }

    @Override
    public int getEndpointActionPriority() {
        return 0;
    }
}
