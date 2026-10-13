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

import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.impl.internal.setting.AIProviderConfigurable;
import consulo.ai.icon.AIIconGroup;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.util.text.UniqueNameGenerator;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.InputBoxBuilder;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.DockLayout;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The chats of a project as the tabs of the AI chat tool window - a chat is a tab bound to one account, "New Chat"
 * opens another with an account of the user's choice, and closing a tab ends its chat.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class AIChatView {
    private static final Key<Boolean> NAMED_BY_USER = Key.create("AIChatView.namedByUser");
    private static final int TITLE_LENGTH = 40;

    private final Project myProject;
    private final Provider<AIProviderTable> myProviderTable;
    private final Provider<ShowConfigurableService> myShowConfigurableService;

    @Inject
    public AIChatView(Project project, Provider<AIProviderTable> providerTable, Provider<ShowConfigurableService> showConfigurableService) {
        myProject = project;
        myProviderTable = providerTable;
        myShowConfigurableService = showConfigurableService;
    }

    @RequiredUIAccess
    public void init(ToolWindow toolWindow) {
        toolWindow.setToHideOnEmptyContent(true);
        toolWindow.setTabActions(new NewChatAction(toolWindow));
        toolWindow.setTabDoubleClickActions(new RenameChatAction(toolWindow));

        addDefaultChat(toolWindow);
    }

    /**
     * Opens a chat with the default account, or says how to get one when there is none.
     */
    @RequiredUIAccess
    public void addDefaultChat(ToolWindow toolWindow) {
        AIProviderTable table = myProviderTable.get();
        UIAccess uiAccess = UIAccess.current();
        table.getDefaultProvider()
            .thenCombine(table.getDefaultModel(), (provider, model) -> new Object[]{provider, model})
            .whenCompleteAsync((pair, error) -> {
                if (toolWindow.isDisposed() || myProject.isDisposed()) {
                    return;
                }
                AIProvider provider = pair == null ? null : (AIProvider) pair[0];
                if (provider == null) {
                    addEmptyContent(toolWindow);
                    return;
                }
                addChat(toolWindow, provider, (AIModel) pair[1]);
            }, uiAccess);
    }

    @RequiredUIAccess
    public void addChat(ToolWindow toolWindow, AIProvider provider, @Nullable AIModel model) {
        ContentManager contentManager = toolWindow.getContentManager();
        for (Content content : contentManager.getContents()) {
            if (!(content.getDisposer() instanceof AIChatPanel)) {
                contentManager.removeContent(content, true);
            }
        }

        Content[] holder = new Content[1];
        AIChatPanel panel = new AIChatPanel(myProject, UIAccess.current(), provider, model, firstMessage -> {
            Content content = holder[0];
            if (content != null && !Boolean.TRUE.equals(content.getUserData(NAMED_BY_USER))) {
                rename(contentManager, content, title(firstMessage));
            }
        }, () -> {
            if (!toolWindow.isVisible()) {
                toolWindow.setIcon(AIIconGroup.toolwindowAiassistantunread());
            }
        });

        Content content = contentManager.getFactory().createUIContent(panel.getComponent(), uniqueName(contentManager, provider.getName()), false);
        content.setCloseable(true);
        content.setDisposer(panel);
        holder[0] = content;

        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }

    @RequiredUIAccess
    private void addEmptyContent(ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        if (contentManager.getContentCount() > 0) {
            return;
        }
        DockLayout layout = DockLayout.create().top(Label.create(AILocalize.labelNoModel()));
        Content content = contentManager.getFactory().createUIContent(layout, AILocalize.toolwindowDisplayName().get(), false);
        content.setCloseable(false);
        contentManager.addContent(content);
    }

    private static void rename(ContentManager contentManager, Content content, String name) {
        String unique = uniqueName(contentManager, name, content);
        content.setDisplayName(unique);
        content.setTabName(unique);
    }

    private static String uniqueName(ContentManager contentManager, String name) {
        return uniqueName(contentManager, name, null);
    }

    private static String uniqueName(ContentManager contentManager, String name, @Nullable Content self) {
        List<String> names = new ArrayList<>();
        for (Content content : contentManager.getContents()) {
            if (content != self) {
                names.add(content.getDisplayName());
            }
        }
        return UniqueNameGenerator.generateUniqueName(name, "", "", " ", "", candidate -> !names.contains(candidate));
    }

    private static String title(String message) {
        String line = message.strip().lines().findFirst().orElse("").strip();
        return line.length() <= TITLE_LENGTH ? line : line.substring(0, TITLE_LENGTH).strip() + "\u2026";
    }

    private class NewChatAction extends DumbAwareAction {
        private final ToolWindow myToolWindow;

        NewChatAction(ToolWindow toolWindow) {
            super(AILocalize.actionNewChat(), LocalizeValue.empty(), PlatformIconGroup.generalAdd());
            myToolWindow = toolWindow;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            UIAccess uiAccess = UIAccess.current();
            myProviderTable.get().getConfiguredProviders().whenCompleteAsync((configured, error) -> {
                List<AIProvider> providers = configured == null ? List.of() : configured;

                DefaultActionGroup group = new DefaultActionGroup();
                for (AIProvider provider : providers) {
                    DefaultActionGroup models = new DefaultActionGroup(LocalizeValue.of(provider.getName()), true);
                    models.getTemplatePresentation().setIcon(provider.getType().getIcon());
                    for (AIModel model : provider.getModels()) {
                        models.add(new DumbAwareAction(model.getDisplayName()) {
                            @RequiredUIAccess
                            @Override
                            public void actionPerformed(AnActionEvent event) {
                                addChat(myToolWindow, provider, model);
                            }
                        });
                    }
                    group.add(models);
                }
                if (!providers.isEmpty()) {
                    group.addSeparator();
                }
                group.add(new DumbAwareAction(AILocalize.actionAddAccountEllipsis(), LocalizeValue.empty(), PlatformIconGroup.generalAdd()) {
                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(AnActionEvent event) {
                        myShowConfigurableService.get().showAndSelect(myProject, AIProviderConfigurable.class);
                    }
                });
                ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                    AILocalize.actionNewChat().get(),
                    group,
                    e.getDataContext(),
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    false
                );
                popup.showUnderneathOf(e);
            }, uiAccess);
        }
    }

    private class RenameChatAction extends DumbAwareAction {
        private final ToolWindow myToolWindow;

        RenameChatAction(ToolWindow toolWindow) {
            super(AILocalize.actionRenameChat());
            myToolWindow = toolWindow;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ContentManager contentManager = myToolWindow.getContentManager();
            Content content = contentManager.getSelectedContent();
            if (content == null || !(content.getDisposer() instanceof AIChatPanel panel)) {
                return;
            }

            UIAccess uiAccess = UIAccess.current();
            InputBoxBuilder.text()
                .title(AILocalize.dialogRenameChatTitle())
                .text(AILocalize.dialogRenameChatPrompt())
                .value(content.getDisplayName())
                .showAsync(panel.getComponent())
                .whenCompleteAsync((name, error) -> {
                    if (error != null || name == null || name.isBlank()) {
                        return;
                    }
                    content.putUserData(NAMED_BY_USER, Boolean.TRUE);
                    rename(contentManager, content, name.strip());
                }, uiAccess);
        }
    }
}
