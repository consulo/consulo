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
package consulo.ide.impl.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.language.editor.hint.HintColorUtil;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.editor.ui.navigation.ItemWithPresentation;
import consulo.language.editor.ui.navigation.PsiTargetNavigator;
import consulo.language.editor.ui.navigation.PsiTargetPresentationFactory;
import consulo.language.editor.ui.navigation.TargetPresentationProvider;
import consulo.language.editor.ui.navigation.TargetPresentationRender;
import consulo.language.editor.ui.navigation.TargetUpdaterTask;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.EditSourceUtil;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.TargetPresentation;
import consulo.project.Project;
import consulo.ui.DelayedAction;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.GenericListComponentUpdater;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.usage.UsageView;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Targets are collected and turned into rows before anything is shown, so the list the popup is handed
 * is already drawable and the ui thread never reads the model. The popup itself is an ordinary
 * {@link ListPopup} - what makes a target popup different is only where its rows come from.
 *
 * @author VISTALL
 * @since 2026-08-27
 */
class PsiTargetNavigatorImpl<T extends PsiElement> implements PsiTargetNavigator<T> {
    private final CoroutineStep<Void, Collection<T>> myTargets;
    private final PsiTargetPresentationFactory myPresentationFactory;

    private @Nullable TargetPresentationProvider<? super T> myPresentationProvider;
    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue myFindUsagesTitle = LocalizeValue.empty();
    private LocalizeValue myEmptyText = LocalizeValue.empty();
    private @Nullable TargetUpdaterTask<T> myUpdater;
    private @Nullable T mySelection;
    private @Nullable PsiElementProcessor<T> myProcessor;
    private int mySelectedIndex = -1;

    private @Nullable ComponentEvent<?> myEvent;
    private @Nullable Editor myEditor;

    PsiTargetNavigatorImpl(CoroutineStep<Void, Collection<T>> targets, PsiTargetPresentationFactory presentationFactory) {
        myTargets = targets;
        myPresentationFactory = presentationFactory;
    }

    @Override
    public PsiTargetNavigator<T> presentationProvider(TargetPresentationProvider<? super T> provider) {
        myPresentationProvider = provider;
        return this;
    }

    @Override
    public PsiTargetNavigator<T> title(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public PsiTargetNavigator<T> findUsagesTitle(LocalizeValue title) {
        myFindUsagesTitle = title;
        return this;
    }

    @Override
    public PsiTargetNavigator<T> emptyText(LocalizeValue emptyText) {
        myEmptyText = emptyText;
        return this;
    }

    @Override
    public PsiTargetNavigator<T> updater(TargetUpdaterTask<T> updater) {
        myUpdater = updater;
        return this;
    }

    @Override
    public PsiTargetNavigator<T> selection(T element) {
        mySelection = element;
        return this;
    }

    @Override
    public PsiTargetNavigator<T> processor(PsiElementProcessor<T> processor) {
        myProcessor = processor;
        return this;
    }

    @Override
    @RequiredUIAccess
    public void navigate(ComponentEvent<?> event, Project project) {
        myEvent = event;

        DelayedAction indicator = DelayedAction.start(event);

        collect(project, items -> {
            indicator.stop();
            show(project, items, popup -> popup.showBy(event));
        });
    }

    @Override
    @RequiredUIAccess
    public void navigate(Editor editor, Project project) {
        myEditor = editor;

        collect(project, items -> show(project, items, editor::showPopupInBestPositionFor));
    }

    @Override
    @RequiredUIAccess
    public void createPopup(Project project, Consumer<ListPopup> consumer) {
        collect(project, items -> consumer.accept(buildPopup(project, items)));
    }

    @RequiredUIAccess
    private void collect(Project project, Consumer<List<ItemWithPresentation<T>>> consumer) {
        CoroutineScope scope = CoroutineScope.of(project.coroutineContext());
        scope.putCopyableUserData(UIAccess.KEY, UIAccess.current());

        Coroutine.first("collecting.targets", myTargets)
            .then("building.presentations", ReadLock.<Collection<T>, List<ItemWithPresentation<T>>>apply(this::present))
            .then(UIAction.<List<ItemWithPresentation<T>>, List<ItemWithPresentation<T>>>apply(items -> {
                consumer.accept(items);
                return items;
            }))
            .runAsync(scope, null);
    }

    @RequiredReadAction
    private List<ItemWithPresentation<T>> present(Collection<T> targets) {
        SmartPointerManager pointerManager = null;
        List<ItemWithPresentation<T>> items = new ArrayList<>(targets.size());
        for (T target : targets) {
            if (pointerManager == null) {
                pointerManager = SmartPointerManager.getInstance(target.getProject());
            }
            // noted here rather than looked up later, so the popup never has to resolve a pointer to find its row
            if (target.equals(mySelection)) {
                mySelectedIndex = items.size();
            }
            items.add(new ItemWithPresentation<>(
                pointerManager.createSmartPsiElementPointer(target),
                myPresentationProvider == null ? defaultPresentation(target) : myPresentationProvider.getPresentation(target)
            ));
        }
        return items;
    }

    @RequiredReadAction
    private TargetPresentation defaultPresentation(T element) {
        return myPresentationFactory.presentation(element);
    }

    @RequiredUIAccess
    private void show(Project project, List<ItemWithPresentation<T>> items, Consumer<JBPopup> shower) {
        // an updater refines an already collected list rather than filling an empty one, so nothing to
        // show here means nothing was found at all
        if (items.isEmpty()) {
            showEmptyHint();
            return;
        }

        if (items.size() == 1 && myUpdater == null) {
            choose(items.get(0));
            return;
        }

        shower.accept(buildPopup(project, items));

        TargetUpdaterTask<T> updater = myUpdater;
        if (updater != null) {
            ProgressManager.getInstance().run(updater);
        }
    }

    @RequiredUIAccess
    private ListPopup buildPopup(Project project, List<ItemWithPresentation<T>> items) {
        MutableFlatDataModel<ItemWithPresentation<T>> model = FlatDataModel.of(items);

        TargetUpdaterTask<T> updater = myUpdater;
        LocalizeValue title = myTitle.isNotEmpty() || updater == null ? myTitle : updater.getCaption(items.size());

        BaseListPopupStep<ItemWithPresentation<T>> step = new BaseListPopupStep<>(title.get(), model) {
            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public String getTextFor(ItemWithPresentation<T> value) {
                return value.getPresentation().getPresentableText().get();
            }

            @Override
            public PopupStep<?> onChosen(ItemWithPresentation<T> selectedValue, boolean finalChoice) {
                choose(selectedValue);
                return FINAL_CHOICE;
            }
        };

        if (mySelectedIndex >= 0 && mySelectedIndex < items.size()) {
            step.setDefaultOptionIndex(mySelectedIndex);
        }

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(project, step);
        popup.setRender(new TargetPresentationRender<>());

        SimpleReference<UsageView> usageView = SimpleReference.create();

        if (myFindUsagesTitle.isNotEmpty()) {
            popup.setCouldPin(pinned -> {
                usageView.set(FindUtil.showInUsageView(null, currentElements(model), myFindUsagesTitle.get(), project));
                pinned.cancel();
                return false;
            });
        }

        if (updater != null) {
            updater.init(popup, new GenericListComponentUpdater<>() {
                @Override
                @RequiredUIAccess
                public void replaceModel(List<? extends ItemWithPresentation<T>> data) {
                    model.replaceAll(new ArrayList<>(data));
                }

                @Override
                public void paintBusy(boolean paintBusy) {
                }
            }, usageView);
        }

        return popup;
    }

    /**
     * The empty text as an error balloon at the invocation point. Anchorless invocations - a popup handed
     * to a consumer - stay silent, and a programmatic event carries no place worth pointing a balloon at.
     */
    @RequiredUIAccess
    private void showEmptyHint() {
        if (myEmptyText.isEmpty()) {
            return;
        }

        RelativePoint point;
        if (myEvent != null && !(myEvent.getInputDetails() instanceof ProgrammaticInputDetails)) {
            point = RelativePoint.fromScreen(myEvent);
        }
        else if (myEditor != null) {
            point = EditorPopupHelper.getInstance().guessBestPopupLocation(myEditor);
        }
        else {
            return;
        }

        JComponent label = HintUtil.createErrorLabel(myEmptyText.get());
        label.setBorder(IdeBorderFactory.createEmptyBorder(2, 7, 2, 7));
        JBPopupFactory.getInstance()
            .createBalloonBuilder(label)
            .setFadeoutTime(3000)
            .setFillColor(TargetAWT.to(HintColorUtil.getErrorColor()))
            .createBalloon()
            .show(point, Balloon.Position.above);
    }

    private PsiElement[] currentElements(FlatDataModel<ItemWithPresentation<T>> model) {
        return ReadAction.compute(() -> {
            List<PsiElement> elements = new ArrayList<>(model.getSize());
            for (ItemWithPresentation<T> item : model) {
                T element = item.dereference();
                if (element != null) {
                    elements.add(element);
                }
            }
            return PsiUtilCore.toPsiElementArray(elements);
        });
    }

    private void choose(ItemWithPresentation<T> item) {
        T element = item.dereference();
        if (element == null) {
            return;
        }

        if (myProcessor != null) {
            myProcessor.execute(element);
            return;
        }

        Navigatable navigatable = element instanceof Navigatable value ? value : EditSourceUtil.getDescriptor(element);
        if (navigatable != null && navigatable.canNavigate()) {
            navigatable.navigate(true);
        }
    }
}
