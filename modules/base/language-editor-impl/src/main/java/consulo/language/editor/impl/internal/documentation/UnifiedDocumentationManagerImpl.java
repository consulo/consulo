/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.editor.impl.internal.documentation;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.JBPopup;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-19
 */
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@Singleton
public class UnifiedDocumentationManagerImpl implements DocumentationManager {
    @Override
    public void showJavaDocInfo(PsiElement element, PsiElement original, boolean requestFocus, Runnable closeCallback, String documentation, boolean useStoredPopupSize) {

    }

    @Override
    public void showJavaDocInfo(Editor editor, PsiElement element, PsiElement original, Runnable closeCallback, String documentation, boolean closeOnSneeze, boolean useStoredPopupSize) {

    }

    @Override
    public void showJavaDocInfo(Editor editor, PsiElement element, PsiElement original, RelativePoint popupAnchor) {

    }

    @Override
    public void showJavaDocInfoAtToolWindow(PsiElement element, PsiElement original) {

    }

    @Override
    public void showJavaDocInfo(Editor editor, PsiFile file, boolean requestFocus, Runnable closeCallback) {

    }

    @Override
    public @Nullable PsiElement getElementFromLookup(Editor editor, PsiFile file) {
        return null;
    }

    @Override
    public @Nullable JBPopup getDocInfoHint() {
        return null;
    }

    @Override
    public boolean hasActiveDockedDocWindow() {
        return false;
    }

    @Override
    public void setAllowContentUpdateFromContext(boolean allow) {

    }

    @Override
    public void updateToolwindowContext() {

    }

    @Override
    public boolean isCloseOnSneeze() {
        return false;
    }

    @Override
    public Project getProject(PsiElement element) {
        return null;
    }

    @Override
    public Editor getEditor() {
        return null;
    }

    @Override
    public @Nullable PsiElement findTargetElement(Editor editor, int offset, PsiFile file, PsiElement contextElement) {
        return null;
    }

    @Override
    public String generateDocumentation(PsiElement element, PsiElement originalElement, boolean onHover) {
        return null;
    }

    @Override
    public void createToolWindow(PsiElement element, PsiElement originalElement) {

    }
}
