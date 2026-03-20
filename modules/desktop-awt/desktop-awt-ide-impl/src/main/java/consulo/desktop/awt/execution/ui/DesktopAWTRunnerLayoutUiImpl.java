/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.desktop.awt.execution.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.impl.internal.ui.layout.RunnerLayoutImpl;
import consulo.execution.impl.internal.ui.layout.RunnerLayoutSettings;
import consulo.execution.impl.internal.ui.layout.ViewImpl;
import consulo.execution.internal.layout.RunnerContentUi;
import consulo.execution.internal.layout.RunnerLayoutUiImpl;
import consulo.execution.ui.layout.*;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.UIAccess;
import consulo.ui.ex.ComponentWithActions;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.QuickActionProvider;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.image.Image;
import consulo.util.concurrent.ActionCallback;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DesktopAWTRunnerLayoutUiImpl implements RunnerLayoutUiImpl {
    private final RunnerLayoutImpl myLayout;
    private final RunnerContentUiImpl myContentUI;

    private final ContentManager myViewsContentManager;
    public static final Key<String> CONTENT_TYPE = Key.create("ContentType");

    public DesktopAWTRunnerLayoutUiImpl(Project project,
                                        Disposable parent,
                                        String runnerId,
                                        String runnerTitle,
                                        String sessionName) {
        myLayout = RunnerLayoutSettings.getInstance().getLayout(runnerId);
        Disposer.register(parent, this);

        myContentUI = new RunnerContentUiImpl(project, this, ActionManager.getInstance(), ProjectIdeFocusManager.getInstance(project), myLayout,
            runnerTitle + " - " + sessionName);
        Disposer.register(this, myContentUI);

        myViewsContentManager = getContentFactory().createContentManager(myContentUI.getContentUI(), true, project);
        myViewsContentManager.addDataProvider(this);
        Disposer.register(this, myViewsContentManager);
    }

    @Override
    
    public LayoutViewOptions setTopToolbar(ActionGroup actions, String place) {
        myContentUI.setTopActions(actions, place);
        return this;
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
    public LayoutStateDefaults initFocusContent(String id,
                                                String condition,
                                                LayoutAttractionPolicy policy) {
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

    public Content addContent(Content content,
                              boolean applyDefaults,
                              int defaultTabId,
                              PlaceInGrid defaultPlace,
                              boolean defaultIsMinimized) {
        String id = content.getUserData(CONTENT_TYPE);

        assert id != null : "Content id is missing, use RunnerLayoutUi to create content instances";

        if (applyDefaults) {
            getLayout().setDefault(id, defaultTabId, defaultPlace, defaultIsMinimized);
        }

        getContentManager().addContent(content);
        return content;
    }

    @Override
    
    public Content createContent(String id,
                                 JComponent component,
                                 String displayName,
                                 @Nullable Image icon,
                                 @Nullable JComponent focusable) {
        return createContent(id, new ComponentWithActions.Impl(component), displayName, icon, focusable);
    }

    @Override
    
    public Content createContent(String contentId,
                                 ComponentWithActions withActions,
                                 String displayName,
                                 @Nullable Image icon,
                                 @Nullable JComponent toFocus) {
        Content content = getContentFactory().createContent(withActions.getComponent(), displayName, false);
        content.putUserData(CONTENT_TYPE, contentId);
        content.putUserData(ViewImpl.ID, contentId);
        content.setIcon(icon);
        if (toFocus != null) {
            content.setPreferredFocusableComponent(toFocus);
        }

        if (!withActions.isContentBuiltIn()) {
            content.setSearchComponent(withActions.getSearchComponent());
            content.setActions(withActions.getToolbarActions(), withActions.getToolbarPlace(), withActions.getToolbarContextComponent());
        }

        return content;
    }

    @Override
    
    public JComponent getComponent() {
        return myViewsContentManager.getComponent();
    }

    private static ContentFactory getContentFactory() {
        return ContentFactory.getInstance();
    }

    public RunnerLayoutImpl getLayout() {
        return myLayout;
    }

    @Override
    public void updateActionsNow(UIAccess uiAccess) {
        myContentUI.updateActionsImmediately(uiAccess);
    }

    @Override
    public void beforeTreeDispose() {
        myContentUI.saveUiState();
    }

    @Override
    public void dispose() {
    }

    @Override
    
    public ContentManager getContentManager() {
        return myViewsContentManager;
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
        return getContentManager(content).setSelectedContent(content, requestFocus || shouldRequestFocus(), forced, implicit);
    }

    private ContentManager getContentManager(Content content) {
        return myContentUI.getContentManager(content);
    }

    private boolean shouldRequestFocus() {
        Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focused != null && SwingUtilities.isDescendingFrom(focused, getContentManager().getComponent());
    }

    @Override
    public boolean removeContent(@Nullable Content content, boolean dispose) {
        return content != null && getContentManager().removeContent(content, dispose);
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
    public void attractBy(String condition) {
        myContentUI.attractByCondition(condition, true);
    }

    @Override
    public void clearAttractionBy(String condition) {
        myContentUI.clearAttractionByCondition(condition, true);
    }

    public void removeContent(String id, boolean dispose) {
        Content content = findContent(id);
        if (content != null) {
            getContentManager().removeContent(content, dispose);
        }
    }

    @Override
    public AnAction getLayoutActions() {
        return myContentUI.getLayoutActions();
    }

    
    @Override
    public AnAction[] getLayoutActionsList() {
        ActionGroup group = (ActionGroup) getLayoutActions();
        return group.getChildren(null);
    }

    
    @Override
    public LayoutViewOptions setTabPopupActions(ActionGroup group) {
        myContentUI.setTabPopupActions(group);
        return this;
    }

    
    @Override
    public LayoutViewOptions setLeftToolbar(ActionGroup leftToolbar, String place) {
        myContentUI.setLeftToolbar(leftToolbar, place);
        return this;
    }

    @Override
    public @Nullable Content findContent(String key) {
        return myContentUI.findContent(key);
    }

    
    @Override
    public RunnerLayoutUi addListener(ContentManagerListener listener, Disposable parent) {
        ContentManager mgr = getContentManager();
        mgr.addContentManagerListener(listener);
        Disposer.register(parent, () -> mgr.removeContentManagerListener(listener));
        return this;
    }

    @Override
    public void removeListener(ContentManagerListener listener) {
        getContentManager().removeContentManagerListener(listener);
    }

    @Override
    public void setBouncing(Content content, boolean activate) {
        myContentUI.processBounce(content, activate);
    }

    @Override
    public boolean isDisposed() {
        return getContentManager().isDisposed();
    }

    @Override
    
    public LayoutViewOptions setMinimizeActionEnabled(boolean enabled) {
        myContentUI.setMinimizeActionEnabled(enabled);
        return this;
    }

    public LayoutViewOptions setToDisposeRemoveContent(boolean toDispose) {
        myContentUI.setToDisposeRemovedContent(toDispose);
        return this;
    }

    @Override
    
    public LayoutViewOptions setMoveToGridActionEnabled(boolean enabled) {
        myContentUI.setMovetoGridActionEnabled(enabled);
        return this;
    }

    @Override
    
    public LayoutViewOptions setAttractionPolicy(String contentId, LayoutAttractionPolicy policy) {
        myContentUI.setPolicy(contentId, policy);
        return this;
    }

    
    @Override
    public LayoutViewOptions setConditionAttractionPolicy(String condition, LayoutAttractionPolicy policy) {
        myContentUI.setConditionPolicy(condition, policy);
        return this;
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
    public LayoutViewOptions setAdditionalFocusActions(ActionGroup group) {
        myContentUI.setAdditionalFocusActions(group);
        return this;
    }

    @Override
    public AnAction getSettingsActions() {
        return myContentUI.getSettingsActions();
    }

    
    @Override
    public AnAction[] getSettingsActionsList() {
        ActionGroup group = (ActionGroup) getSettingsActions();
        return group.getChildren(null);
    }

    
    @Override
    public Content[] getContents() {
        Content[] contents = new Content[getContentManager().getContentCount()];
        for (int i = 0; i < contents.length; i++) {
            contents[i] = getContentManager().getContent(i);
        }
        return contents;
    }

    @Override
    public @Nullable Object getData(Key dataId) {
        if (QuickActionProvider.KEY == dataId || RunnerContentUi.KEY == dataId) {
            return myContentUI;
        }
        return null;
    }

    @Override
    public void setLeftToolbarVisible(boolean value) {
        myContentUI.setLeftToolbarVisible(value);
    }

    @Override
    public void setTopLeftActionsBefore(boolean value) {
        myContentUI.setTopLeftActionsBefore(value);
    }

    @Override
    public void setContentToolbarBefore(boolean value) {
        myContentUI.setContentToolbarBefore(value);
    }

    @Override
    public void setTopLeftActionsVisible(boolean value) {
        myContentUI.setTopLeftActionsVisible(value);
    }

    @Override
    public List<AnAction> getActions() {
        return myContentUI.getActions(true);
    }

    @Override
    public RunnerContentUiImpl getContentUI() {
        return myContentUI;
    }
}
