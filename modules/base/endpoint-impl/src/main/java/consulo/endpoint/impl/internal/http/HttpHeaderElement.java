// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.http;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.content.scope.SearchScope;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.impl.psi.FakePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SyntheticElement;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.RelativePoint2D;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import org.jspecify.annotations.Nullable;

public class HttpHeaderElement extends FakePsiElement implements SyntheticElement, PsiMetaOwner, PsiPresentableMetaData {
    private final PsiElement myParent;
    private final String myHeaderName;

    public HttpHeaderElement(PsiElement parent, String headerName) {
        myParent = parent;
        myHeaderName = headerName;
    }

    @Override
    public String getName() {
        return myHeaderName;
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public String getName(PsiElement context) {
        return getName();
    }

    @Override
    public PsiElement getDeclaration() {
        return this;
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return true;
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
    public Image getIcon() {
        return PlatformIconGroup.nodesType();
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
    public String getTypeName() {
        return EndpointLocalize.httpHeaderElement().get();
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return another instanceof HttpHeaderElement headerElement && headerElement.myHeaderName.equals(myHeaderName);
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        Project project = getProject();
        if (DumbService.getInstance(project).isDumb()) {
            return;
        }
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }
        RelativePoint2D popupPosition = EditorPopupHelper.getInstance().guessBestPopupLocation(editor);
        LanguageEditorInternalHelper.getInstance().startFindUsages(editor, project, this, popupPosition);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        HttpHeaderElement that = (HttpHeaderElement) other;

        return myHeaderName.equals(that.myHeaderName);
    }

    @Override
    public int hashCode() {
        return myHeaderName.hashCode();
    }
}
