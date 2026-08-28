/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ide.navigation.GotoTargetPresentationProvider;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.navigation.ItemWithPresentation;
import consulo.language.editor.ui.navigation.PsiTargetPresentationFactory;
import consulo.language.editor.ui.navigation.TargetPresentationRender;
import consulo.language.editor.ui.navigation.TargetUpdaterTask;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.util.EditSourceUtil;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.TargetPresentation;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.GenericListComponentUpdater;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.usage.UsageView;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.*;
import java.util.function.Supplier;

public abstract class GotoTargetHandler implements CodeInsightActionHandler {
    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void invoke(Project project, Editor editor, PsiFile file) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed(getFeatureUsedKey());

        try {
            GotoData gotoData = getSourceAndTargetElements(editor, file);
            if (gotoData != null) {
                show(project, editor, file, gotoData);
            }
        }
        catch (IndexNotReadyException e) {
            DumbService.getInstance(project)
                .showDumbModeNotification(LocalizeValue.localizeTODO("Navigation is not available here during index update"));
        }
    }

    protected abstract String getFeatureUsedKey();

    protected abstract @Nullable GotoData getSourceAndTargetElements(Editor editor, PsiFile file);

    @RequiredUIAccess
    private void show(
        Project project,
        Editor editor,
        PsiFile file,
        GotoData gotoData
    ) {
        PsiElement[] targets = gotoData.targets;
        List<AdditionalAction> additionalActions = gotoData.additionalActions;

        if (targets.length == 0 && additionalActions.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor, getNotFoundMessage(project, editor, file));
            return;
        }

        boolean finished = gotoData.listUpdaterTask == null || gotoData.listUpdaterTask.isFinished();
        if (targets.length == 1 && additionalActions.isEmpty() && finished) {
            navigateToElement(targets[0]);
            return;
        }

        String name = ReadAction.compute(() -> ((PsiNamedElement)gotoData.source).getName());
        LocalizeValue title = getChooserTitle(gotoData.source, name, targets.length, finished);

        List<ItemWithPresentation<PsiElement>> items = ReadAction.compute(() -> present(gotoData));
        if (shouldSortTargets()) {
            items.sort(TargetUpdaterTask.presentationOrder());
        }

        List<Object> rows = new ArrayList<>(items.size() + additionalActions.size());
        rows.addAll(items);
        rows.addAll(additionalActions);

        MutableFlatDataModel<Object> model = FlatDataModel.of(rows);

        BaseListPopupStep<Object> step = new BaseListPopupStep<>(title.get(), model) {
            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public String getTextFor(Object value) {
                return value instanceof AdditionalAction action
                    ? action.getText()
                    : ((ItemWithPresentation<?>)value).getPresentation().getPresentableText().get();
            }

            @Override
            public PopupStep<?> onChosen(Object selectedValue, boolean finalChoice) {
                choose(project, selectedValue);
                return FINAL_CHOICE;
            }
        };

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(project, step);
        popup.setRender((presentation, item) -> {
            Object value = item.getValue();
            if (value instanceof AdditionalAction action) {
                presentation.withIcon(action.getIcon());
                presentation.append(LocalizeValue.of(action.getText()));
            }
            else if (value instanceof ItemWithPresentation<?> withPresentation) {
                TargetPresentationRender.renderPresentation(presentation, withPresentation.getPresentation());
            }
        });

        SimpleReference<UsageView> usageView = SimpleReference.create();
        popup.setCouldPin(pinned -> {
            usageView.set(FindUtil.showInUsageView(
                gotoData.source,
                currentElements(model),
                getFindUsagesTitle(gotoData.source, name, gotoData.targets.length).get(),
                project
            ));
            pinned.cancel();
            return false;
        });

        String adText = getAdText(gotoData.source, targets.length);
        if (adText != null) {
            popup.setAdText(adText);
        }

        TargetUpdaterTask<PsiElement> updaterTask = gotoData.listUpdaterTask;
        if (updaterTask != null) {
            popup.addListener(new JBPopupListener() {
                @Override
                public void onClosed(LightweightWindowEvent event) {
                    updaterTask.cancelTask();
                }
            });

            updaterTask.init(popup, new GenericListComponentUpdater<>() {
                @Override
                @RequiredUIAccess
                public void replaceModel(List<? extends ItemWithPresentation<PsiElement>> data) {
                    List<Object> newRows = new ArrayList<>(data.size() + additionalActions.size());
                    newRows.addAll(data);
                    newRows.addAll(additionalActions);
                    model.replaceAll(newRows);
                }

                @Override
                public void paintBusy(boolean paintBusy) {
                }
            }, usageView);

            // a search which finds a single target has no popup to show at all, so the popup is held back
            // long enough for a fast one to finish and cancel it
            Alarm alarm = new Alarm(popup);
            alarm.addRequest(() -> editor.showPopupInBestPositionFor(popup), 300);
            ProgressManager.getInstance().run(updaterTask);
        }
        else {
            editor.showPopupInBestPositionFor(popup);
        }
    }

    @RequiredReadAction
    private static List<ItemWithPresentation<PsiElement>> present(GotoData gotoData) {
        SmartPointerManager pointerManager = SmartPointerManager.getInstance(gotoData.source.getProject());

        List<ItemWithPresentation<PsiElement>> items = new ArrayList<>(gotoData.targets.length);
        for (PsiElement target : gotoData.targets) {
            items.add(new ItemWithPresentation<>(pointerManager.createSmartPsiElementPointer(target), createPresentation(gotoData, target)));
        }
        return items;
    }

    private static PsiElement[] currentElements(FlatDataModel<Object> model) {
        return ReadAction.compute(() -> {
            List<PsiElement> elements = new ArrayList<>(model.getSize());
            for (Object row : model) {
                if (row instanceof ItemWithPresentation<?> item) {
                    PsiElement element = item.dereference();
                    if (element != null) {
                        elements.add(element);
                    }
                }
            }
            return PsiUtilCore.toPsiElementArray(elements);
        });
    }

    @RequiredUIAccess
    private void choose(Project project, Object row) {
        if (row instanceof AdditionalAction action) {
            action.execute();
            return;
        }

        PsiElement element = ((ItemWithPresentation<?>)row).dereference();
        if (element == null) {
            return;
        }

        try {
            Navigatable nav = element instanceof Navigatable navigatable ? navigatable : EditSourceUtil.getDescriptor(element);
            if (nav != null && nav.canNavigate()) {
                navigateToElement(nav);
            }
        }
        catch (IndexNotReadyException e) {
            DumbService.getInstance(project).showDumbModeNotification("Navigation is not available while indexing");
        }
    }

    @RequiredReadAction
    public static TargetPresentation createPresentation(GotoData gotoData, PsiElement element) {
        TargetPresentation presentation = Application.get()
            .getExtensionPoint(GotoTargetPresentationProvider.class)
            .computeSafeIfAny(it -> it.getPresentation(element, gotoData));
        return presentation != null
            ? presentation
            : Application.get().getInstance(PsiTargetPresentationFactory.class).presentation(element);
    }

    protected boolean navigateToElement(PsiElement target) {
        Navigatable descriptor = target instanceof Navigatable navigatable ? navigatable : EditSourceUtil.getDescriptor(target);
        if (descriptor != null && descriptor.canNavigate()) {
            navigateToElement(descriptor);
            return true;
        }
        return false;
    }

    protected void navigateToElement(Navigatable descriptor) {
        descriptor.navigate(true);
    }

    protected boolean shouldSortTargets() {
        return true;
    }

    @Nonnull
    protected abstract LocalizeValue getChooserTitle(PsiElement sourceElement, String name, int length, boolean finished);

    @Nonnull
    protected LocalizeValue getFindUsagesTitle(PsiElement sourceElement, String name, int length) {
        return getChooserTitle(sourceElement, name, length, true);
    }

    protected abstract String getNotFoundMessage(Project project, Editor editor, PsiFile file);

    protected @Nullable String getAdText(PsiElement source, int length) {
        return null;
    }

    public interface AdditionalAction {
        String getText();

        Image getIcon();

        void execute();
    }

    public static class GotoData implements GotoTargetPresentationProvider.Options {
        public final PsiElement source;
        public PsiElement[] targets;
        public final List<AdditionalAction> additionalActions;

        private boolean hasDifferentNames;
        public TargetUpdaterTask<PsiElement> listUpdaterTask;
        protected final Set<String> myNames;

        @RequiredReadAction
        public GotoData(PsiElement source, PsiElement[] targets, List<AdditionalAction> additionalActions) {
            this.source = source;
            this.targets = targets;
            this.additionalActions = additionalActions;

            myNames = new HashSet<>();
            for (PsiElement target : targets) {
                if (target instanceof PsiNamedElement namedElement) {
                    myNames.add(namedElement.getName());
                    if (myNames.size() > 1) {
                        break;
                    }
                }
            }

            hasDifferentNames = myNames.size() > 1;
        }

        @Override
        public boolean hasDifferentNames() {
            return hasDifferentNames;
        }

        public boolean addTarget(PsiElement element) {
            if (ArrayUtil.find(targets, element) > -1) {
                return false;
            }
            targets = ArrayUtil.append(targets, element);
            if (!hasDifferentNames && element instanceof PsiNamedElement namedElement) {
                String name = Application.get().runReadAction((Supplier<String>)() -> namedElement.getName());
                myNames.add(name);
                hasDifferentNames = myNames.size() > 1;
            }
            return true;
        }
    }
}
