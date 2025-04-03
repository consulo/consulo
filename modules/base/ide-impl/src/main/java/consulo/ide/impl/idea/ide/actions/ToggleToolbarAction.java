/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataProvider;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionGroupUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerAdapter;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.toolWindow.ToolWindow;

import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author gregsh
 */
public class ToggleToolbarAction extends ToggleAction implements DumbAware {
    @Nonnull
    public static ActionGroup createToggleToolbarGroup(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
        return new DefaultActionGroup(
            new OptionsGroup(toolWindow),
            new ToggleToolbarAction(toolWindow, PropertiesComponent.getInstance(project)),
            AnSeparator.getInstance()
        );
    }

    private final PropertiesComponent myPropertiesComponent;
    private final ToolWindow myToolWindow;

    private ToggleToolbarAction(@Nonnull ToolWindow toolWindow, @Nonnull PropertiesComponent propertiesComponent) {
        super("Show Toolbar");
        myPropertiesComponent = propertiesComponent;
        myToolWindow = toolWindow;
        myToolWindow.getContentManager().addContentManagerListener(new ContentManagerAdapter() {
            @Override
            public void contentAdded(ContentManagerEvent event) {
                JComponent component = event.getContent().getComponent();
                setContentToolbarVisible(component, getVisibilityValue());

                // support nested content managers, e.g. RunnerLayoutUi as content component
                ContentManager contentManager =
                    component instanceof DataProvider dataProvider ? dataProvider.getDataUnchecked(PlatformDataKeys.CONTENT_MANAGER) : null;
                if (contentManager != null) {
                    contentManager.addContentManagerListener(this);
                }
            }
        });
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        boolean hasToolbars = iterateToolbars(myToolWindow.getContentManager().getComponent()).iterator().hasNext();
        e.getPresentation().setVisible(hasToolbars);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return getVisibilityValue();
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        myPropertiesComponent.setValue(getProperty(), String.valueOf(state), String.valueOf(true));
        for (Content content : myToolWindow.getContentManager().getContents()) {
            setContentToolbarVisible(content.getComponent(), state);
        }
    }

    @Nonnull
    private String getProperty() {
        return getShowToolbarProperty(myToolWindow);
    }

    private boolean getVisibilityValue() {
        return myPropertiesComponent.getBoolean(getProperty(), true);
    }

    private static void setContentToolbarVisible(@Nonnull JComponent root, boolean state) {
        for (ActionToolbar toolbar : iterateToolbars(root)) {
            toolbar.getComponent().setVisible(state);
        }
    }

    @Nonnull
    @RequiredUIAccess
    public static String getShowToolbarProperty(@Nonnull ToolWindow window) {
        return "ToolWindow" + window.getStripeTitle() + ".ShowToolbar";
    }

    @Nonnull
    private static Iterable<ActionToolbar> iterateToolbars(JComponent root) {
        return UIUtil.uiTraverser().withRoot(root).preOrderDfsTraversal().filter(ActionToolbar.class);
    }

    private static class OptionsGroup extends ActionGroup implements DumbAware {
        private final ToolWindow myToolWindow;

        public OptionsGroup(ToolWindow toolWindow) {
            super("View Options", true);
            myToolWindow = toolWindow;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setVisible(!ActionGroupUtil.isGroupEmpty(this, e));
        }

        @Nonnull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            ContentManager contentManager = myToolWindow.getContentManager();
            Content selectedContent = contentManager.getSelectedContent();
            JComponent contentComponent = selectedContent != null ? selectedContent.getComponent() : null;
            if (contentComponent == null) {
                return EMPTY_ARRAY;
            }
            List<AnAction> result = new SmartList<>();
            for (final ActionToolbar toolbar : iterateToolbars(contentComponent)) {
                JComponent c = toolbar.getComponent();
                if (c.isVisible() || !c.isValid()) {
                    continue;
                }
                if (!result.isEmpty() && !(ContainerUtil.getLastItem(result) instanceof AnSeparator)) {
                    result.add(AnSeparator.getInstance());
                }

                List<AnAction> actions = toolbar.getActions();
                for (AnAction action : actions) {
                    if (action instanceof ToggleAction && !result.contains(action)) {
                        result.add(action);
                    }
                    else if (action instanceof AnSeparator) {
                        if (!result.isEmpty() && !(ContainerUtil.getLastItem(result) instanceof AnSeparator)) {
                            result.add(AnSeparator.getInstance());
                        }
                    }
                }
            }
            boolean popup = result.size() > 3;
            setPopup(popup);
            if (!popup && !result.isEmpty()) {
                result.add(AnSeparator.getInstance());
            }
            return result.toArray(new AnAction[result.size()]);
        }
    }
}
