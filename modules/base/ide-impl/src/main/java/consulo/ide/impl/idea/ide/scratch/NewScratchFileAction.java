// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ActionImpl;
import consulo.dataContext.DataContext;
import consulo.language.editor.util.IdeView;
import consulo.ide.impl.idea.ide.actions.NewActionGroup;
import consulo.language.Language;
import consulo.language.scratch.ScratchFileCreationHelper;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;

import java.util.function.Consumer;

import static consulo.ide.impl.idea.ide.scratch.ScratchFileActions.createContext;
import static consulo.ide.impl.idea.ide.scratch.ScratchFileActions.doCreateNewScratch;

@ActionImpl(id = "NewScratchFile")
public class NewScratchFileAction extends LegacyDumbAwareAction implements AnActionWithAsyncUpdate {
    private static final Image ICON = ImageEffects.layered(PlatformIconGroup.filetypesText(), PlatformIconGroup.actionsScratch());

    public NewScratchFileAction() {
        getTemplatePresentation().setIcon(ICON);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(CompletableFutureStep.<Object, Boolean>await(input -> NewActionGroup.isActionInNewPopupMenuAsync(e, this)))
            .then(CodeExecution.consume(inNewPopupMenu -> applyPresentation(e, Boolean.TRUE.equals(inNewPopupMenu))));
    }

    private void applyPresentation(AnActionEvent e, boolean inNewPopupMenu) {
        Presentation presentation = e.getPresentation();
        presentation.setText(inNewPopupMenu ? ActionLocalize.actionNewscratchfileText() : ActionLocalize.actionNewscratchfileTextWithNew());

        Project project = e.getData(Project.KEY);
        String place = e.getPlace();
        boolean enabled = project != null && (e.isFromActionToolbar() || ActionPlaces.isMainMenuOrActionSearch(place)
            || ActionPlaces.isPopupPlace(place) && e.getData(IdeView.KEY) != null);
        presentation.setEnabledAndVisible(enabled);

        presentation.setIcon(ICON);
        if (ActionPlaces.MAIN_MENU.equals(place) && !inNewPopupMenu) {
            presentation.setIcon(null);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        ScratchFileCreationHelper.Context context = createContext(e, project);
        Consumer<Language> consumer = l -> {
            context.language = l;
            ScratchFileCreationHelper.forLanguage(context.language).prepareText(project, context, DataContext.EMPTY_CONTEXT);
            doCreateNewScratch(project, context);
        };
        if (context.language != null) {
            consumer.accept(context.language);
        }
        else {
            LRUPopupBuilder.forFileLanguages(project, ActionLocalize.actionNewscratchfileTextWithNew().get(), null, consumer)
                .showCenteredInCurrentWindow(project);
        }
    }
}
