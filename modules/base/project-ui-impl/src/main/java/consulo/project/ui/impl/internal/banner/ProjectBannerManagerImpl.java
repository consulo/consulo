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
package consulo.project.ui.impl.internal.banner;

import consulo.annotation.component.ServiceImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectBannerManager;
import consulo.project.ui.internal.ProjectBannerProvider;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Component;
import consulo.ui.Toast;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.internal.DismissedHints;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The banners are shown over the list of running tasks of the status bar - the state they stand for is usually a task
 * running there. Closing a banner lasts until the state comes again; "don't show again" lasts for good. A revealed
 * banner is shown even so, for as long as its state lasts.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
@Singleton
@ServiceImpl
public class ProjectBannerManagerImpl implements ProjectBannerManager {
    private static final Logger LOG = Logger.getInstance(ProjectBannerManagerImpl.class);

    private final Project myProject;
    private final Provider<WindowManager> myWindowManager;
    private final Provider<DismissedHints> myDismissedHints;
    private final Provider<ActionToolbarFactory> myActionToolbarFactory;

    private final Map<String, Toast> myShown = new LinkedHashMap<>();
    private final Map<String, LocalizeValue> myMessages = new HashMap<>();
    private final Set<String> myRevealed = new HashSet<>();
    private final Set<String> myClosed = new HashSet<>();

    @Inject
    public ProjectBannerManagerImpl(
        Project project,
        Provider<WindowManager> windowManager,
        Provider<DismissedHints> dismissedHints,
        Provider<ActionToolbarFactory> actionToolbarFactory
    ) {
        myProject = project;
        myWindowManager = windowManager;
        myDismissedHints = dismissedHints;
        myActionToolbarFactory = actionToolbarFactory;
    }

    @Override
    public void update() {
        UIAccess uiAccess = uiAccess();
        if (uiAccess != null) {
            uiAccess.giveIfNeed(() -> refresh(null));
        }
    }

    @Override
    public void reveal(String id, LocalizeValue message) {
        UIAccess uiAccess = uiAccess();
        if (uiAccess == null) {
            return;
        }

        uiAccess.giveIfNeed(() -> {
            myMessages.put(id, message);
            myRevealed.add(id);

            Toast toast = myShown.get(id);
            if (toast != null) {
                toast.setText(message);
            }
            refresh(id);
        });
    }

    private @Nullable UIAccess uiAccess() {
        if (myProject.isDisposed()) {
            return null;
        }
        UIAccess uiAccess = myProject.getUIAccess();
        return uiAccess.isValid() ? uiAccess : null;
    }

    @RequiredUIAccess
    private void refresh(@Nullable String revealed) {
        if (myProject.isDisposed()) {
            return;
        }

        Map<String, ProjectBannerProvider> providers = new LinkedHashMap<>();
        myProject.getExtensionPoint(ProjectBannerProvider.class).forEach(provider -> providers.put(provider.getId(), provider));

        for (ProjectBannerProvider provider : providers.values()) {
            String id = provider.getId();

            boolean visible = isVisible(provider);
            if (!visible && !id.equals(revealed)) {
                myClosed.remove(id);
                myRevealed.remove(id);
                myMessages.remove(id);
            }

            boolean wanted = myRevealed.contains(id)
                || visible && !myClosed.contains(id) && !myDismissedHints.get().isDismissed(id);
            if (!wanted) {
                myShown.remove(id);
                continue;
            }

            if (!myShown.containsKey(id)) {
                myShown.put(id, createToast(provider));
            }
        }

        myShown.keySet().retainAll(providers.keySet());

        push(revealed != null && myShown.containsKey(revealed));
    }

    @RequiredUIAccess
    private void push(boolean reveal) {
        IdeFrame frame = myWindowManager.get().getIdeFrame(myProject);
        if (frame != null && frame.getStatusBar() instanceof StatusBarEx statusBar) {
            List<Component> banners = new ArrayList<>(myShown.values());
            statusBar.setProcessBanners(banners, reveal);
        }
    }

    private static boolean isVisible(ProjectBannerProvider provider) {
        try {
            return provider.isVisible();
        }
        catch (Throwable e) {
            LOG.error("Banner provider failed: " + provider.getId(), e);
            return false;
        }
    }

    @RequiredUIAccess
    private Toast createToast(ProjectBannerProvider provider) {
        String id = provider.getId();

        Toast toast = Toast.create();
        provider.customize(toast);

        LocalizeValue message = myMessages.get(id);
        if (message != null) {
            toast.setText(message);
        }

        toast.addAction(CommonLocalize.dialogOptionsDoNotShowShort(), () -> {
            myDismissedHints.get().dismiss(id);
            close(id);
        });

        ActionGroup group = ActionGroup.newImmutableBuilder()
            .add(new DumbAwareAction(CommonLocalize.actionClose(), LocalizeValue.empty(), PlatformIconGroup.actionsClose()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    close(id);
                }
            })
            .build();

        ActionToolbar toolbar =
            myActionToolbarFactory.get().createActionToolbar(ActionPlaces.NOTIFICATION, group, ActionToolbar.Style.INPLACE);
        toolbar.setTargetUIComponent(toast);
        toolbar.updateActionsAsync();
        toast.setToolbarComponent(toolbar.getUIComponent());
        return toast;
    }

    @RequiredUIAccess
    private void close(String id) {
        myClosed.add(id);
        myRevealed.remove(id);
        myShown.remove(id);
        push(false);
    }
}
