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
package consulo.execution.impl.internal.ui.layout;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ui.layout.LayoutAttractionPolicy;
import consulo.execution.ui.layout.LayoutStateDefaults;
import consulo.execution.ui.layout.LayoutViewOptions;
import consulo.execution.ui.layout.PlaceInGrid;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.logging.Logger;
import consulo.ui.ex.ComponentContainer;
import consulo.ui.ex.ComponentWithActions;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.image.Image;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.util.concurrent.ActionCallback;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Tabs of a run, for the frontends which render {@link consulo.ui} components rather than swing. Analog of
 * {@code consulo.desktop.awt.execution.ui.DesktopAWTRunnerLayoutUiImpl} - the very same bookkeeping on the
 * layout of the runner and on the content manager, with the views shown as plain tabs instead of a grid which
 * can be split, docked and minimized.
 *
 * @author VISTALL
 * @since 2026-09-23
 */
public class UnifiedRunnerLayoutUiImpl implements RunnerLayoutUi, LayoutStateDefaults, LayoutViewOptions, Disposable.Parent {
    private static final Logger LOG = Logger.getInstance(UnifiedRunnerLayoutUiImpl.class);

    private final RunnerLayoutImpl myLayout;
    private final ContentManager myViewsContentManager;

    private final DefaultActionGroup myTopActions = new DefaultActionGroup();
    private final ActionToolbar myTopToolbar;
    private final DockLayout myRoot;

    @RequiredUIAccess
    public UnifiedRunnerLayoutUiImpl(
        Project project,
        Disposable parent,
        String runnerId,
        String runnerTitle,
        String sessionName
    ) {
        myLayout = RunnerLayoutSettings.getInstance().getLayout(runnerId);
        Disposer.register(parent, this);

        myViewsContentManager = ContentFactory.getInstance().createContentManager(true, project);
        Disposer.register(this, myViewsContentManager);

        myTopToolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.RUNNER_TOOLBAR, myTopActions, ActionToolbar.Style.HORIZONTAL);

        myRoot = DockLayout.create();
        myRoot.top(myTopToolbar.getUIComponent());
        myRoot.center(myViewsContentManager.getUIComponent());

        // what the actions of a run need - its environment, its descriptor - is published by the content
        // manager, so the toolbar reads its context from there rather than from the frame around it
        myTopToolbar.setTargetUIComponent(myViewsContentManager.getUIComponent());
    }

    @Override
    public LayoutStateDefaults getDefaults() {
        return this;
    }

    @Override
    public LayoutViewOptions getOptions() {
        return this;
    }

    @Override
    public ContentManager getContentManager() {
        return myViewsContentManager;
    }

    public RunnerLayoutImpl getLayout() {
        return myLayout;
    }

    @Override
    public LayoutStateDefaults initTabDefaults(int id, String text, Image icon) {
        getLayout().setDefault(id, text, icon);
        return this;
    }

    @Override
    public LayoutStateDefaults initFocusContent(String id, String condition) {
        return initFocusContent(id, condition, new LayoutAttractionPolicy.FocusOnce());
    }

    @Override
    public LayoutStateDefaults initFocusContent(String id, String condition, LayoutAttractionPolicy policy) {
        getLayout().setDefaultToFocus(id, condition, policy);
        return this;
    }

    @Override
    public Content addContent(Content content) {
        return addContent(content, false, -1, PlaceInGrid.center, false);
    }

    @Override
    public Content addContent(Content content, int defaultTabId, PlaceInGrid defaultPlace, boolean defaultIsMinimized) {
        return addContent(content, true, defaultTabId, defaultPlace, defaultIsMinimized);
    }

    private Content addContent(
        Content content,
        boolean applyDefaults,
        int defaultTabId,
        PlaceInGrid defaultPlace,
        boolean defaultIsMinimized
    ) {
        String id = content.getUserData(ViewImpl.ID);

        assert id != null : "Content id is missing, use RunnerLayoutUi to create content instances";

        if (applyDefaults) {
            getLayout().setDefault(id, defaultTabId, defaultPlace, defaultIsMinimized);
        }

        ContentManager contentManager = getContentManager();
        contentManager.addContent(content);

        // a row of tabs shows the one which is selected, and the first view to arrive has nobody to select it
        if (contentManager.getSelectedContent() == null) {
            contentManager.setSelectedContent(content);
        }

        return content;
    }

    @Override
    @RequiredUIAccess
    public Content createContent(String contentId, ComponentContainer container, String displayName, @Nullable Image icon) {
        return createUIContent(contentId, container.getUIComponent(), displayName, icon, null);
    }

    @Override
    @RequiredUIAccess
    public Content createUIContent(
        String contentId,
        @Nullable Component component,
        String displayName,
        @Nullable Image icon,
        @Nullable Component toFocus
    ) {
        Content content = ContentFactory.getInstance().createUIContent(component, displayName, false);
        content.putUserData(ViewImpl.ID, contentId);
        content.setIcon(icon);

        return content;
    }

    @Override
    public Component getUIComponent() {
        return myRoot;
    }

    /**
     * A view built of swing cannot be drawn by a frontend without an awt hierarchy. Such a view is still part of
     * the run, so it is taken as a tab of its own which says as much, and the rest of the tabs come up.
     */
    @Override
    @RequiredUIAccess
    public Content createContent(
        String contentId,
        JComponent component,
        String displayName,
        @Nullable Image icon,
        @Nullable JComponent focusable
    ) {
        Component uiComponent = TargetAWT.from(component);
        if (uiComponent == null) {
            LOG.warn("View '" + displayName + "' of the run is a swing component, which this frontend cannot show");
        }

        return createUIContent(contentId, uiComponent, displayName, icon, null);
    }

    @Override
    @RequiredUIAccess
    public Content createContent(
        String contentId,
        ComponentWithActions withActions,
        String displayName,
        @Nullable Image icon,
        @Nullable JComponent toFocus
    ) {
        return createContent(contentId, withActions.getComponent(), displayName, icon, toFocus);
    }

    @Override
    public boolean removeContent(@Nullable Content content, boolean dispose) {
        return content != null && getContentManager().removeContent(content, dispose);
    }

    @Override
    public @Nullable Content findContent(String contentId) {
        for (Content content : getContentManager().getContents()) {
            if (contentId.equals(content.getUserData(ViewImpl.ID))) {
                return content;
            }
        }
        return null;
    }

    @Override
    public ActionCallback selectAndFocus(@Nullable Content content, boolean requestFocus, boolean forced) {
        return selectAndFocus(content, requestFocus, forced, false);
    }

    @Override
    public ActionCallback selectAndFocus(@Nullable Content content, boolean requestFocus, boolean forced, boolean implicit) {
        if (content == null) {
            return ActionCallback.REJECTED;
        }
        return getContentManager().setSelectedContent(content, requestFocus, forced, implicit);
    }

    @Override
    public RunnerLayoutUi addListener(ContentManagerListener listener, Disposable parent) {
        ContentManager contentManager = getContentManager();
        contentManager.addContentManagerListener(listener);
        Disposer.register(parent, () -> contentManager.removeContentManagerListener(listener));
        return this;
    }

    @Override
    public void removeListener(ContentManagerListener listener) {
        getContentManager().removeContentManagerListener(listener);
    }

    /**
     * Drawing attention to a view is the business of the tabs which can be made to blink. Plain tabs cannot,
     * and the view is reachable all the same, so nothing is done rather than refusing the call.
     */
    @Override
    public void attractBy(String condition) {
    }

    @Override
    public void clearAttractionBy(String condition) {
    }

    @Override
    public void setBouncing(Content content, boolean activate) {
    }

    @Override
    public boolean isDisposed() {
        return getContentManager().isDisposed();
    }

    @Override
    public void updateActionsNow(UIAccess uiAccess) {
        uiAccess.giveIfNeed(myTopToolbar::updateActionsImmediately);
    }

    @Override
    public Content[] getContents() {
        return getContentManager().getContents();
    }

    @Override
    @RequiredUIAccess
    public LayoutViewOptions setTopToolbar(ActionGroup actions, String place) {
        myTopActions.removeAll();
        myTopActions.add(actions);

        myTopToolbar.updateActionsImmediately();
        return this;
    }

    @Override
    public boolean isToFocus(Content content, String condition) {
        String id = content.getUserData(ViewImpl.ID);
        return getLayout().isToFocus(id, condition);
    }

    @Override
    public LayoutViewOptions setToFocus(@Nullable Content content, String condition) {
        getLayout().setToFocus(content != null ? content.getUserData(ViewImpl.ID) : null, condition);
        return this;
    }

    @Override
    public LayoutViewOptions setAttractionPolicy(String contentId, LayoutAttractionPolicy policy) {
        return this;
    }

    @Override
    public LayoutViewOptions setConditionAttractionPolicy(String condition, LayoutAttractionPolicy policy) {
        return this;
    }

    /**
     * What a grid of views offers - splitting, moving and minimizing them - has no counterpart in a row of
     * tabs, so the options are taken and the actions answer as an empty group.
     */
    @Override
    public LayoutViewOptions setMinimizeActionEnabled(boolean enabled) {
        return this;
    }

    @Override
    public LayoutViewOptions setMoveToGridActionEnabled(boolean enabled) {
        return this;
    }

    @Override
    public LayoutViewOptions setTabPopupActions(ActionGroup group) {
        return this;
    }

    @Override
    public LayoutViewOptions setAdditionalFocusActions(ActionGroup group) {
        return this;
    }

    @Override
    public AnAction getLayoutActions() {
        return new DefaultActionGroup();
    }

    @Override
    public AnAction[] getLayoutActionsList() {
        return AnAction.EMPTY_ARRAY;
    }

    @Override
    public AnAction getSettingsActions() {
        return new DefaultActionGroup();
    }

    @Override
    public AnAction[] getSettingsActionsList() {
        return AnAction.EMPTY_ARRAY;
    }

    @Override
    public void beforeTreeDispose() {
    }

    @Override
    public void dispose() {
    }
}
