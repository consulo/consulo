/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.ui.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author peter
 */
public abstract class NavigationGutterIconRenderer extends GutterIconRenderer implements GutterIconNavigationHandler<PsiElement> {
    private final LocalizeValue myPopupTitle;

    private final LocalizeValue myEmptyText;
    private final @Nullable TargetPresentationProvider<PsiElement> myPresentationProvider;
    private final Supplier<List<SmartPsiElementPointer>> myPointers;

    protected NavigationGutterIconRenderer(
        LocalizeValue popupTitle,
        LocalizeValue emptyText,
        @Nullable TargetPresentationProvider<PsiElement> presentationProvider,
        Supplier<List<SmartPsiElementPointer>> pointers
    ) {
        myPopupTitle = popupTitle;
        myEmptyText = emptyText;
        myPresentationProvider = presentationProvider;
        myPointers = pointers;
    }

    @Override
    public boolean isNavigateAction() {
        return true;
    }

    @RequiredReadAction
    public List<PsiElement> getTargetElements() {
        return ContainerUtil.mapNotNull(myPointers.get(), SmartPsiElementPointer::getElement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NavigationGutterIconRenderer renderer = (NavigationGutterIconRenderer)o;

        return myEmptyText.equals(renderer.myEmptyText)
            && myPointers.get().equals(renderer.myPointers.get())
            && myPopupTitle.equals(renderer.myPopupTitle);
    }

    @Override
    public int hashCode() {
        int result;
        result = myPopupTitle.hashCode();
        result = 31 * result + myEmptyText.hashCode();
        result = 31 * result + myPointers.get().hashCode();
        return result;
    }

    @Override
    public @Nullable AnAction getClickAction() {
        return new AnAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                Component component = e.getData(Component.KEY);
                Project project = e.getData(Project.KEY);
                if (component == null || project == null) {
                    return;
                }

                InputDetails inputDetails = e.getInputDetails();
                navigate(inputDetails == null ? new ComponentEvent<>(component) : new ComponentEvent<>(component, inputDetails), project);
            }
        };
    }

    @Override
    @RequiredUIAccess
    public void navigate(ComponentEvent<?> event, PsiElement elt) {
        navigate(event, elt.getProject());
    }

    /**
     * The pointers are dereferenced by the navigator inside a read action off the ui thread, never here.
     */
    @RequiredUIAccess
    public void navigate(ComponentEvent<?> event, Project project) {
        PsiTargetNavigator<PsiElement> navigator = Application.get().getInstance(PsiTargetNavigationService.class)
            .<PsiElement>newNavigator(this::getTargetElements);

        if (myPresentationProvider != null) {
            navigator = navigator.presentationProvider(myPresentationProvider);
        }

        navigator
            .title(myPopupTitle)
            .emptyText(myEmptyText)
            .navigate(event, project);
    }
}
