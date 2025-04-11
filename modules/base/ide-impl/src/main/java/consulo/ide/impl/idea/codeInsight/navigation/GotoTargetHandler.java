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
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.ui.JBListWithHintProvider;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ide.navigation.GotoTargetRendererProvider;
import consulo.ide.ui.popup.HintUpdateSupply;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.util.EditSourceUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import consulo.usage.UsageView;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

public abstract class GotoTargetHandler implements CodeInsightActionHandler {
    private static final Logger LOG = Logger.getInstance(GotoTargetHandler.class);
    private final PsiElementListCellRenderer myDefaultTargetElementRenderer = new DefaultPsiElementListCellRenderer();
    private final ListCellRenderer<AdditionalAction> myActionElementRenderer = new ActionCellRenderer();

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
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

    @Nullable
    protected abstract GotoData getSourceAndTargetElements(Editor editor, PsiFile file);

    @RequiredUIAccess
    private void show(
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull GotoData gotoData
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

        for (PsiElement eachTarget : targets) {
            gotoData.renderers.put(eachTarget, createRenderer(gotoData, eachTarget));
        }

        String name = ((PsiNamedElement)gotoData.source).getName();
        String title = getChooserTitle(gotoData.source, name, targets.length, finished);

        if (shouldSortTargets()) {
            Arrays.sort(targets, createComparator(gotoData.renderers, gotoData));
        }

        List<Object> allElements = new ArrayList<>(targets.length + additionalActions.size());
        Collections.addAll(allElements, targets);
        allElements.addAll(additionalActions);

        JBListWithHintProvider<Object> list = new JBListWithHintProvider<Object>(new CollectionListModel<>(allElements)) {
            @Override
            protected PsiElement getPsiElementForHint(Object selectedValue) {
                return selectedValue instanceof PsiElement element ? element : null;
            }
        };

        list.setFont(EditorUtil.getEditorFont());

        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
                if (value instanceof AdditionalAction additionalAction) {
                    return myActionElementRenderer.getListCellRendererComponent(
                        list,
                        additionalAction,
                        index,
                        isSelected,
                        cellHasFocus
                    );
                }
                PsiElementListCellRenderer renderer = getRenderer(value, gotoData.renderers, gotoData);
                return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        Runnable runnable = () -> {
            int[] ids = list.getSelectedIndices();
            if (ids == null || ids.length == 0) {
                return;
            }
            Object[] selectedElements = list.getSelectedValues();
            for (Object element : selectedElements) {
                if (element instanceof AdditionalAction additionalAction) {
                    additionalAction.execute();
                }
                else {
                    Navigatable nav =
                        element instanceof Navigatable navigatable ? navigatable : EditSourceUtil.getDescriptor((PsiElement)element);
                    try {
                        if (nav != null && nav.canNavigate()) {
                            navigateToElement(nav);
                        }
                    }
                    catch (IndexNotReadyException e) {
                        DumbService.getInstance(project).showDumbModeNotification("Navigation is not available while indexing");
                    }
                }
            }
        };

        PopupChooserBuilder<?> builder = new PopupChooserBuilder(list);
        builder.setFilteringEnabled(o -> {
            if (o instanceof AdditionalAction additionalAction) {
                return additionalAction.getText();
            }
            return getRenderer(o, gotoData.renderers, gotoData).getElementText((PsiElement)o);
        });

        SimpleReference<UsageView> usageView = new SimpleReference<>();
        JBPopup popup = builder
            .setTitle(title)
            .setItemChoosenCallback(runnable)
            .setMovable(true)
            .setCancelCallback(() -> {
                HintUpdateSupply.hideHint(list);
                ListBackgroundUpdaterTask task = gotoData.listUpdaterTask;
                if (task != null) {
                    task.cancelTask();
                }
                return true;
            })
            .setCouldPin(popup1 -> {
                usageView.set(FindUtil.showInUsageView(
                    gotoData.source,
                    gotoData.targets,
                    getFindUsagesTitle(gotoData.source, name, gotoData.targets.length),
                    gotoData.source.getProject()
                ));
                popup1.cancel();
                return false;
            })
            .setAdText(getAdText(gotoData.source, targets.length))
            .createPopup();

        builder.getScrollPane().setBorder(null);
        builder.getScrollPane().setViewportBorder(null);

        if (gotoData.listUpdaterTask != null) {
            Alarm alarm = new Alarm(popup);
            alarm.addRequest(() -> editor.showPopupInBestPositionFor(popup), 300);
            gotoData.listUpdaterTask.init((AbstractPopup)popup, list, usageView);
            ProgressManager.getInstance().run(gotoData.listUpdaterTask);
        }
        else {
            editor.showPopupInBestPositionFor(popup);
        }
    }

    @Nonnull
    private PsiElementListCellRenderer getRenderer(
        Object value,
        Map<Object, PsiElementListCellRenderer> targetsWithRenderers,
        GotoData gotoData
    ) {
        PsiElementListCellRenderer renderer = targetsWithRenderers.get(value);
        if (renderer == null) {
            renderer = gotoData.getRenderer(value);
        }
        return renderer != null ? renderer : myDefaultTargetElementRenderer;
    }

    @Nonnull
    protected Comparator<PsiElement> createComparator(
        Map<Object, PsiElementListCellRenderer> targetsWithRenderers,
        GotoData gotoData
    ) {
        return new Comparator<>() {
            @Override
            public int compare(PsiElement o1, PsiElement o2) {
                return getComparingObject(o1).compareTo(getComparingObject(o2));
            }

            private Comparable getComparingObject(PsiElement o1) {
                return getRenderer(o1, targetsWithRenderers, gotoData).getComparingObject(o1);
            }
        };
    }

    public static PsiElementListCellRenderer createRenderer(@Nonnull GotoData gotoData, @Nonnull PsiElement eachTarget) {
        return GotoTargetRendererProvider.EP_NAME.computeSafeIfAny(Application.get(), it -> it.getRenderer(eachTarget, gotoData));
    }

    protected boolean navigateToElement(PsiElement target) {
        Navigatable descriptor = target instanceof Navigatable ? (Navigatable)target : EditSourceUtil.getDescriptor(target);
        if (descriptor != null && descriptor.canNavigate()) {
            navigateToElement(descriptor);
            return true;
        }
        return false;
    }

    protected void navigateToElement(@Nonnull Navigatable descriptor) {
        descriptor.navigate(true);
    }

    protected boolean shouldSortTargets() {
        return true;
    }

    @Nonnull
    @Deprecated // use getChooserTitle(PsiElement, String, int, boolean) instead
    protected String getChooserTitle(PsiElement sourceElement, String name, int length) {
        LOG.warn("Please override getChooserTitle(PsiElement, String, int, boolean) instead");
        return "";
    }

    @Nonnull
    protected String getChooserTitle(@Nonnull PsiElement sourceElement, String name, int length, boolean finished) {
        return getChooserTitle(sourceElement, name, length);
    }

    @Nonnull
    protected String getFindUsagesTitle(@Nonnull PsiElement sourceElement, String name, int length) {
        return getChooserTitle(sourceElement, name, length, true);
    }

    @Nonnull
    protected abstract String getNotFoundMessage(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file);

    @Nullable
    protected String getAdText(PsiElement source, int length) {
        return null;
    }

    public interface AdditionalAction {
        @Nonnull
        String getText();

        Image getIcon();

        void execute();
    }

    public static class GotoData implements GotoTargetRendererProvider.Options {
        @Nonnull
        public final PsiElement source;
        public PsiElement[] targets;
        public final List<AdditionalAction> additionalActions;

        private boolean hasDifferentNames;
        public ListBackgroundUpdaterTask listUpdaterTask;
        protected final Set<String> myNames;
        public Map<Object, PsiElementListCellRenderer> renderers = new HashMap<>();

        @RequiredReadAction
        public GotoData(@Nonnull PsiElement source, @Nonnull PsiElement[] targets, @Nonnull List<AdditionalAction> additionalActions) {
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
            renderers.put(element, createRenderer(this, element));
            if (!hasDifferentNames && element instanceof PsiNamedElement namedElement) {
                String name = Application.get().runReadAction((Supplier<String>)() -> namedElement.getName());
                myNames.add(name);
                hasDifferentNames = myNames.size() > 1;
            }
            return true;
        }

        public PsiElementListCellRenderer getRenderer(Object value) {
            return renderers.get(value);
        }
    }

    private static class DefaultPsiElementListCellRenderer extends PsiElementListCellRenderer {
        @Override
        @RequiredReadAction
        public String getElementText(PsiElement element) {
            if (element instanceof PsiNamedElement namedElement) {
                String name = namedElement.getName();
                if (name != null) {
                    return name;
                }
            }
            return element.getContainingFile().getName();
        }

        @Override
        protected String getContainerText(PsiElement element, String name) {
            if (element instanceof NavigationItem navigationItem) {
                ItemPresentation presentation = navigationItem.getPresentation();
                return presentation != null ? presentation.getLocationString() : null;
            }

            return null;
        }

        @Override
        protected int getIconFlags() {
            return 0;
        }
    }

    private static class ActionCellRenderer extends ColoredListCellRenderer<AdditionalAction> {
        @Override
        protected void customizeCellRenderer(
            @Nonnull JList<? extends AdditionalAction> list,
            AdditionalAction action,
            int index,
            boolean selected,
            boolean hasFocus
        ) {
            if (action != null) {
                append(action.getText());
                setIcon(action.getIcon());
            }
        }
    }
}
