// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.http;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.scope.SearchScope;
import consulo.document.util.TextRange;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.impl.psi.FakePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SyntheticElement;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

public class HttpMethodElement extends FakePsiElement implements SyntheticElement, PsiMetaOwner, PsiPresentableMetaData {
    private final PsiElement myParent;
    private final String myMethodName;
    private final TextRange myRefTextRange;

    public HttpMethodElement(PsiElement parent, String methodName, TextRange refTextRange) {
        myParent = parent;
        myMethodName = methodName;
        myRefTextRange = refTextRange;
    }

    @Override
    public String getName() {
        return myMethodName;
    }

    @Override
    public String getName(PsiElement context) {
        return getName();
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public PsiElement getDeclaration() {
        return this;
    }

    @Override
    public PsiMetaData getMetaData() {
        return this;
    }

    @Override
    public PsiElement getNavigationElement() {
        return this;
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return true;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesPpweb();
    }

    @Override
    public String getTypeName() {
        return EndpointLocalize.httpMethodElement().get();
    }

    @Override
    public SearchScope getUseScope() {
        return GlobalSearchScope.allScope(getProject());
    }

    @Override
    public GlobalSearchScope getResolveScope() {
        return GlobalSearchScope.allScope(getProject());
    }

    @Override
    public void init(PsiElement element) {
    }

    @Override
    public Object[] getDependences() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return another instanceof HttpMethodElement methodElement && methodElement.myMethodName.equals(myMethodName);
    }

    @Override
    @RequiredReadAction
    public int getTextOffset() {
        return getParent().getTextOffset() + myRefTextRange.getStartOffset();
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        Project project = getProject();
        if (DumbService.getInstance(project).isDumb()) {
            return;
        }
        PsiFile file = getContainingFile();
        if (file != null && file.isValid()) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile == null) {
                return;
            }
            OpenFileDescriptorFactory.getInstance(project)
                .newBuilder(virtualFile)
                .offset(getTextOffset())
                .build()
                .navigate(requestFocus);
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        HttpMethodElement that = (HttpMethodElement) other;

        return myMethodName.equals(that.myMethodName);
    }

    @Override
    public int hashCode() {
        return myMethodName.hashCode();
    }
}
