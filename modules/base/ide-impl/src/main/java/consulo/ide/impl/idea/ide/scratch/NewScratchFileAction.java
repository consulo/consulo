// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ActionImpl;
import consulo.application.util.NotNullLazyValue;
import consulo.dataContext.DataContext;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.ide.actions.NewActionGroup;
import consulo.language.Language;
import consulo.language.scratch.ScratchFileCreationHelper;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

import static consulo.ide.impl.idea.ide.scratch.ScratchFileActions.createContext;
import static consulo.ide.impl.idea.ide.scratch.ScratchFileActions.doCreateNewScratch;

@ActionImpl(id = "NewScratchFile")
public class NewScratchFileAction extends DumbAwareAction {
    private static final Image ICON = ImageEffects.layered(PlatformIconGroup.filetypesText(), PlatformIconGroup.actionsScratch());

    private final NotNullLazyValue<LocalizeValue> myActionText = NotNullLazyValue.createValue(
        () -> NewActionGroup.isActionInNewPopupMenu(this)
            ? ActionLocalize.actionNewscratchfileText()
            : ActionLocalize.actionNewscratchfileTextWithNew()
    );

    public NewScratchFileAction() {
        getTemplatePresentation().setIcon(ICON);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        getTemplatePresentation().setTextValue(myActionText.getValue());

        Project project = e.getData(Project.KEY);
        String place = e.getPlace();
        boolean enabled = project != null && (e.isFromActionToolbar() || ActionPlaces.isMainMenuOrActionSearch(place)
            || ActionPlaces.isPopupPlace(place) && e.getData(IdeView.KEY) != null);

        e.getPresentation().setEnabledAndVisible(enabled);
        updatePresentationTextAndIcon(e, e.getPresentation());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
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

    private void updatePresentationTextAndIcon(@Nonnull AnActionEvent e, @Nonnull Presentation presentation) {
        presentation.setTextValue(myActionText.getValue());
        presentation.setIcon(ICON);
        if (ActionPlaces.MAIN_MENU.equals(e.getPlace()) && !NewActionGroup.isActionInNewPopupMenu(this)) {
            presentation.setIcon(null);
        }
    }
}
