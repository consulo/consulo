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

import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsUtil;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.documentation.DocumentationColorKey;
import consulo.language.editor.internal.DocumentationView;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class DocumentationViewImpl implements DocumentationView {
    private final Project myProject;
    private final DocumentationSettings mySettings;
    private final EditorColorsManager myEditorColorsManager;

    private final DocumentationBrowser myBrowser;
    private final HtmlView myHtmlView;
    private final @Nullable ActionToolbar myToolbar;
    private final @Nullable DockLayout myToolbarRow;
    private final DockLayout myRoot;

    private final List<Runnable> myRenderListeners = new CopyOnWriteArrayList<>();
    private boolean myRendered;
    private final List<Runnable> myNavigateListeners = new CopyOnWriteArrayList<>();

    @RequiredUIAccess
    public DocumentationViewImpl(
        Project project,
        DocumentationSettings settings,
        EditorColorsManager editorColorsManager,
        @Nullable Function<DocumentationBrowser, List<? extends AnAction>> secondaryActions
    ) {
        myProject = project;
        mySettings = settings;
        myEditorColorsManager = editorColorsManager;

        myBrowser = new DocumentationBrowser(project);
        Disposer.register(this, myBrowser);

        myHtmlView = HtmlView.create();
        myHtmlView.addHyperlinkListener(this::onHyperlink);

        myRoot = DockLayout.create(Space.NONE);
        myRoot.center(myHtmlView);
        myRoot.setBackgroundColor(getBackground());

        if (secondaryActions != null) {
            Supplier<DocumentationBrowser> browser = () -> myBrowser;

            List<AnAction> secondary = new ArrayList<>(secondaryActions.apply(myBrowser));
            secondary.add(new DocumentationFontSizeGroup(settings));

            List<AnAction> primary = List.of(
                new DocumentationBackAction(browser),
                new DocumentationForwardAction(browser),
                new DocumentationExternalAction(browser)
            );

            ActionGroup group = ActionGroup.newImmutableBuilder()
                .add(new DocumentationEditSourceAction(browser, this::fireNavigated))
                .add(new DocumentationMoreGroup(secondary, primary))
                .build();

            ActionToolbar toolbar =
                ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.JAVADOC_TOOLBAR, group, ActionToolbar.Style.HORIZONTAL);
            toolbar.setTargetUIComponent(myRoot);
            toolbar.getUIComponent().setBackgroundColor(getBackground());

            DockLayout toolbarRow = DockLayout.create(Space.NONE);
            toolbarRow.right(toolbar.getUIComponent());
            toolbarRow.setBackgroundColor(getBackground());
            myRoot.bottom(toolbarRow);

            myToolbar = toolbar;
            myToolbarRow = toolbarRow;
        }
        else {
            myToolbar = null;
            myToolbarRow = null;
        }

        Disposer.register(this, myBrowser.addPageListener(page -> render()));
        Disposer.register(this, myBrowser.addFragmentListener(myHtmlView::scrollToFragment));
        Disposer.register(this, settings.addFontSizeListener(() -> {
            UIAccess uiAccess = myProject.getUIAccess();
            if (uiAccess.isValid()) {
                uiAccess.give(this::render);
            }
        }));

        updateToolbar();
    }

    @RequiredUIAccess
    private void updateToolbar() {
        ActionToolbar toolbar = myToolbar;
        if (toolbar == null) {
            return;
        }

        toolbar.updateActionsAsync().whenComplete((actions, error) -> {
            UIAccess uiAccess = myProject.getUIAccess();
            if (uiAccess.isValid()) {
                uiAccess.give(this::fireLayoutChanged);
            }
        });
    }

    @RequiredUIAccess
    private void fireLayoutChanged() {
        if (!myRendered) {
            return;
        }

        for (Runnable listener : myRenderListeners) {
            listener.run();
        }
    }

    public Project getProject() {
        return myProject;
    }

    public DocumentationBrowser getBrowser() {
        return myBrowser;
    }

    public HtmlView getHtmlView() {
        return myHtmlView;
    }

    @RequiredUIAccess
    public void setSizeToContent(boolean sizeToContent) {
        myHtmlView.setSizeToContent(sizeToContent);
    }

    public Disposable addRenderListener(Runnable listener) {
        myRenderListeners.add(listener);
        return () -> myRenderListeners.remove(listener);
    }

    public Disposable addNavigateListener(Runnable listener) {
        myNavigateListeners.add(listener);
        return () -> myNavigateListeners.remove(listener);
    }

    @RequiredUIAccess
    void fireNavigated() {
        for (Runnable listener : myNavigateListeners) {
            listener.run();
        }
    }

    @RequiredUIAccess
    @Override
    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    @Override
    public void showElement(
        SmartPsiElementPointer<? extends PsiElement> element,
        @Nullable SmartPsiElementPointer<? extends PsiElement> originalElement
    ) {
        myBrowser.reset(new DocumentationElementSource(element, originalElement));
    }

    @RequiredUIAccess
    public void show(DocumentationSource source) {
        myBrowser.reset(source);
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        myBrowser.showPage(DocumentationPage.message(""));
    }

    @Override
    public Disposable addDoubleClickListener(Runnable listener) {
        return myHtmlView.addDoubleClickListener(event -> listener.run());
    }

    @RequiredUIAccess
    private void onHyperlink(HyperlinkEvent event) {
        myBrowser.navigate(event.getDescription(), DataManager.getInstance().getDataContext(myHtmlView));
    }

    @RequiredUIAccess
    private void render() {
        DocumentationPage page = myBrowser.getPage();
        if (page == null) {
            return;
        }

        ColorValue background = getBackground();
        myRoot.setBackgroundColor(background);
        DockLayout toolbarRow = myToolbarRow;
        if (toolbarRow != null) {
            toolbarRow.setBackgroundColor(background);
        }
        ActionToolbar toolbar = myToolbar;
        if (toolbar != null) {
            toolbar.getUIComponent().setBackgroundColor(background);
        }

        String css = DocumentationStyle.build(
            StyleManager.get().getCurrentStyle(),
            background,
            mySettings.getFontSize(),
            myEditorColorsManager.getGlobalScheme().getEditorFontName()
        );

        myHtmlView.setImageResolver(page.images()::get);
        myHtmlView.render(new HtmlView.RenderData(page.html(), css, new URL[0]))
            .whenComplete((result, error) -> {
                UIAccess uiAccess = myProject.getUIAccess();
                if (uiAccess.isValid()) {
                    uiAccess.give(() -> rendered(page));
                }
            });

        updateToolbar();
    }

    private static ColorValue getBackground() {
        ColorValue color = EditorColorsUtil.getGlobalOrDefaultColor(DocumentationColorKey.BACKGROUND);
        if (color != null) {
            return color;
        }

        ColorValue defaultColor = DocumentationColorKey.BACKGROUND.getDefaultColorValue();
        return defaultColor != null ? defaultColor : StyleManager.get().getCurrentStyle().getColorValue(ComponentColors.LAYOUT);
    }

    @RequiredUIAccess
    private void rendered(DocumentationPage page) {
        if (myBrowser.getPage() != page) {
            return;
        }

        String anchor = page.anchor();
        if (anchor != null) {
            myHtmlView.scrollToFragment(anchor);
        }

        myRendered = true;
        fireLayoutChanged();
    }

    @Override
    public void dispose() {
        myRenderListeners.clear();
        myNavigateListeners.clear();
    }
}
