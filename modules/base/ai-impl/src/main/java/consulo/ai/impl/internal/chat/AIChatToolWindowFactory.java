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
package consulo.ai.impl.internal.chat;

import consulo.ai.AIProviderTable;
import consulo.ai.AIToolWindowAvailabilityListener;
import consulo.ai.AIToolWindowTabProvider;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
public class AIChatToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String ID = "AI";

    private static final Key<String> CONTENT_ID = Key.create("AIToolWindowContentId");
    private static final String CHATS_ID = "chats";

    @Override
    public String getId() {
        return ID;
    }

    /**
     * The chats while there is an account to chat with, the tabs while they are available - the tool window while one
     * of them is.
     */
    @Override
    public boolean shouldBeAvailable(Project project) {
        return hasChats() || hasTabs(project);
    }

    @Override
    public void init(Project project, ToolWindow toolWindow) {
        UIAccess uiAccess = project.getUIAccess();
        project.getApplication().getMessageBus().connect(project).subscribe(AIToolWindowAvailabilityListener.class, () -> uiAccess.give(() -> {
            if (project.isDisposed()) {
                return;
            }
            toolWindow.setAvailable(shouldBeAvailable(project), null);
            // the contents are made once the window is first shown - until then there is nothing to change
            if (toolWindow.getContentManager().getContentCount() > 0) {
                syncContents(project, toolWindow);
            }
        }));
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        syncContents(project, toolWindow);
    }

    /**
     * Adds the chats and the tabs which are available and are not shown, removes the ones shown which are not.
     */
    @RequiredUIAccess
    private void syncContents(Project project, ToolWindow toolWindow) {
        ContentManager manager = toolWindow.getContentManager();
        Content chats = find(manager, CHATS_ID);
        if (hasChats() && chats == null) {
            manager.addContent(createChats(project, toolWindow), 0);
        }
        else if (!hasChats() && chats != null) {
            manager.removeContent(chats, true);
        }

        project.getApplication().getExtensionPoint(AIToolWindowTabProvider.class).forEach(provider -> {
            String id = provider.getClass().getName();
            Content tab = find(manager, id);
            boolean available = provider.isAvailable(project);
            if (available && tab == null) {
                Disposable disposable = Disposable.newDisposable("AIToolWindowTab");
                Content created = ContentFactory.getInstance().createUIContent(provider.createComponent(project, disposable), provider.getDisplayName().get(), false);
                created.setDisposer(disposable);
                created.putUserData(CONTENT_ID, id);
                manager.addContent(created);
            }
            else if (!available && tab != null) {
                manager.removeContent(tab, true);
            }
        });
    }

    private static @Nullable Content find(ContentManager manager, String id) {
        for (Content content : manager.getContents()) {
            if (id.equals(content.getUserData(CONTENT_ID))) {
                return content;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private static Content createChats(Project project, ToolWindow toolWindow) {
        AIChatTreePanel panel = new AIChatTreePanel(
            project,
            project.getInstance(AIChatView.class),
            AIProviderTable.getInstance(),
            project.getApplication().getInstance(ShowConfigurableService.class)
        );
        Content content = ContentFactory.getInstance().createUIContent(panel.getComponent(), AILocalize.tabChats().get(), false);
        content.setDisposer(panel);
        content.putUserData(CONTENT_ID, CHATS_ID);

        // chats a provider keeps on its side may have come from outside the IDE - a terminal - since it was last shown
        project.getMessageBus().connect(panel).subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
            private boolean myVisible;

            @Override
            public void stateChanged(ToolWindowManager manager) {
                boolean visible = toolWindow.isVisible();
                if (visible && !myVisible) {
                    panel.refresh();
                }
                myVisible = visible;
            }
        });
        return content;
    }

    private static boolean hasChats() {
        return !AIProviderTable.getInstance().getProviders().isEmpty();
    }

    private static boolean hasTabs(Project project) {
        boolean[] available = {false};
        project.getApplication().getExtensionPoint(AIToolWindowTabProvider.class).forEach(provider -> available[0] |= provider.isAvailable(project));
        return available[0];
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsToolwindowaskai();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.toolwindowDisplayName();
    }
}
