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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.review.create.CodeReviewCreateReviewLayoutBuilder;
import consulo.review.create.CodeReviewTitleDescriptionComponentFactory;
import consulo.review.create.CodeReviewTitleDescriptionViewModel;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.ex.tester.UITesterTab;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Inject;

import java.util.concurrent.TimeUnit;

/**
 * The review creation screen: the template loads for a moment, the title and description are markdown editors, the
 * generation toolbar sits at the end of the description.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl(id = "createReview", order = "after reactions")
public class CreateReviewUITesterTab implements UITesterTab {
    private static final class SandTitleDescriptionViewModel implements CodeReviewTitleDescriptionViewModel {
        private final MutableObservableValue<String> myTitle = MutableObservableValue.of("");
        private final MutableObservableValue<String> myDescription = MutableObservableValue.of("");
        private final MutableObservableValue<Boolean> myTemplateLoading = MutableObservableValue.of(true);

        private SandTitleDescriptionViewModel() {
            AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                myDescription.set("## Summary\n\n## Testing\n");
                myTemplateLoading.set(false);
            }, 1200, TimeUnit.MILLISECONDS);
        }

        @Override
        public ObservableValue<String> getTitleText() {
            return myTitle;
        }

        @Override
        public ObservableValue<String> getDescriptionText() {
            return myDescription;
        }

        @Override
        public ObservableValue<Boolean> isTemplateLoading() {
            return myTemplateLoading;
        }

        @Override
        public void setTitle(String text) {
            myTitle.set(text);
        }

        @Override
        public void setDescription(String text) {
            myDescription.set(text);
        }
    }

    private static final class GenerateAction extends AnAction implements DumbAware {
        private final SandTitleDescriptionViewModel myVm;

        private GenerateAction(SandTitleDescriptionViewModel vm) {
            super(LocalizeValue.localizeTODO("Generate Title and Description"), LocalizeValue.empty(), PlatformIconGroup.actionsLightning());
            myVm = vm;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            myVm.setTitle("Add review creation to the platform");
            myVm.setDescription("## Summary\nPorts the title and description editors.\n\n## Testing\nSand UI Tester.\n");
        }
    }

    private final CodeReviewTitleDescriptionComponentFactory myFactory;
    private final ProjectManager myProjectManager;

    @Inject
    public CreateReviewUITesterTab(CodeReviewTitleDescriptionComponentFactory factory, ProjectManager projectManager) {
        myFactory = factory;
        myProjectManager = projectManager;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Create Review");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Project project = myProjectManager.getDefaultProject();
        SandTitleDescriptionViewModel vm = new SandTitleDescriptionViewModel();

        Component titleEditor = myFactory.createTitleEditorIn(uiDisposable, project, vm, LocalizeValue.localizeTODO("Title"));
        Component descriptionEditor = myFactory.createDescriptionEditorIn(uiDisposable, project, vm, LocalizeValue.localizeTODO("Description"));
        Component textPanel = myFactory.createIn(uiDisposable, vm, titleEditor, descriptionEditor);

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar("SandCreateReviewTitle", ActionGroup.of(new GenerateAction(vm)), ActionToolbar.Style.INPLACE);
        Component editorPanel = myFactory.createGenerationToolbarOverlay(textPanel, descriptionEditor, toolbar);

        Button create = Button.create(LocalizeValue.localizeTODO("Create Review"));
        create.addStyle(ButtonStyle.PRIMARY);

        return CodeReviewCreateReviewLayoutBuilder.create()
            .addComponent(HorizontalLayout.create(Space.SMALL).add(Label.create(LocalizeValue.localizeTODO("feature/review-demo → master"))))
            .addSeparator()
            .addComponent(editorPanel, true, false)
            .addSeparator()
            .addComponent(HorizontalLayout.create(Space.LARGE).add(create))
            .build();
    }
}
