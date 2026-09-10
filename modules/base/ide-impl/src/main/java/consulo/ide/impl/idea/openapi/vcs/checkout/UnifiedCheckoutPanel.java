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
package consulo.ide.impl.idea.openapi.vcs.checkout;

import consulo.application.Application;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.externalService.pluginAdvertiser.PluginAdvertiserHelper;
import consulo.ide.impl.welcomeScreen.WelcomeSlide;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.action.Presentation;
import consulo.ui.layout.*;
import consulo.versionControlSystem.checkout.CheckoutCallback;
import consulo.versionControlSystem.checkout.CheckoutPage;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import consulo.versionControlSystem.internal.ProjectLevelVcsManagerEx;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 2026-08-25
 */
public class UnifiedCheckoutPanel implements WelcomeSlide {
    private static final String EMPTY_PANEL = "empty-panel";

    private final Project myProject;
    private final TitlelessDecorator myTitlelessDecorator;
    private final List<CheckoutProvider> myProviders;

    private final Map<CheckoutProvider, String> myProviderIds = new HashMap<>();
    private final Map<CheckoutProvider, Boolean> myProviderStates = new HashMap<>();
    private final Map<CheckoutProvider, CheckoutPage> myProviderPages = new HashMap<>();

    private @Nullable DockLayout myRoot;
    private SwipeLayout myProviderLayout;
    private @Nullable Button myOkButton;

    private CheckBox myOpenProjectFromDirectory;

    private @Nullable Runnable myCloseAction;

    private @Nullable CheckoutProvider myCurrentProvider;
    private int myProviderCounter;

    public UnifiedCheckoutPanel(Application application, Project project, TitlelessDecorator titlelessDecorator) {
        myProject = project;
        myTitlelessDecorator = titlelessDecorator;

        myProviders = new ArrayList<>(application.getExtensionPoint(CheckoutProvider.class).getExtensionList());
        myProviders.sort(Comparator.comparing(provider -> provider.getName().map(Presentation.NO_MNEMONIC).get()));
    }

    @Override
    public void setCloseAction(Runnable closeAction) {
        myCloseAction = closeAction;
    }

    @Override
    @RequiredUIAccess
    public Layout getLayout() {
        DockLayout root = myRoot;
        if (root == null) {
            myRoot = root = buildLayout();
        }
        return root;
    }

    @RequiredUIAccess
    private DockLayout buildLayout() {
        DockLayout root = DockLayout.create();

        Component leftPanel = buildLeftPanel();
        myTitlelessDecorator.makeLeftComponentLower(leftPanel);
        root.left(leftPanel);

        myProviderLayout = SwipeLayout.create();
        myProviderLayout.register(EMPTY_PANEL, this::buildEmptyPanel);

        DockLayout rightPanel = DockLayout.create();
        rightPanel.center(myProviderLayout);

        Component southPanel = buildSouthPanel();
        if (southPanel != null) {
            rightPanel.bottom(southPanel);
        }

        root.center(myTitlelessDecorator.modifyRightComponent(root, rightPanel));
        return root;
    }

    @RequiredUIAccess
    private Component buildLeftPanel() {
        ListBox<CheckoutProvider> listBox = ListBox.create(myProviders);
        listBox.setRender((presentation, item) -> {
            CheckoutProvider provider = item.getValue();
            if (provider == null) {
                return;
            }

            presentation.withIcon(provider.getIcon());
            presentation.append(provider.getName().map(Presentation.NO_MNEMONIC), TextAttribute.REGULAR);
        });
        listBox.addValueListener(event -> providerSelected(event.getValue()));

        Hyperlink moreViaPlugins = Hyperlink.create(
            LocalizeValue.localizeTODO("More via plugins..."),
            event -> PluginAdvertiserHelper.getInstance().showDialogForExtension(ExtensionPreview.of(CheckoutProvider.class, "*"))
        );

        WrappedLayout southPanel = WrappedLayout.create(moreViaPlugins);
        southPanel.paddingBuilder().allSet(Space.LARGE).apply();

        DockLayout leftPanel = DockLayout.create();
        leftPanel.center(ScrollableLayout.create(listBox));
        leftPanel.bottom(southPanel);
        leftPanel.borderBuilder().rightSet().apply();
        leftPanel.setSize(new Size2D(300, -1));
        return leftPanel;
    }

    @RequiredUIAccess
    private Layout buildEmptyPanel() {
        VerticalLayout layout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER);
        layout.add(Label.create(VcsLocalize.checkoutSelectVcsPrompt()));
        return layout;
    }

    @RequiredUIAccess
    protected @Nullable Component buildSouthPanel() {
        HorizontalLayout buttonsPanel = HorizontalLayout.create();

        Button cancelButton = Button.create(CommonLocalize.buttonCancel());
        cancelButton.addClickListener(e -> doCancelAction());
        buttonsPanel.add(cancelButton);

        myOkButton = Button.create(VcsLocalize.checkoutButtonText());
        myOkButton.addStyle(ButtonStyle.PRIMARY);
        myOkButton.setEnabled(false);
        myOkButton.addClickListener(e -> doOkAction());
        buttonsPanel.add(myOkButton);

        DockLayout south = DockLayout.create();
        south.right(buttonsPanel);
        south.paddingBuilder().allSet(Space.LARGE).apply();
        return south;
    }

    @RequiredUIAccess
    private void providerSelected(@Nullable CheckoutProvider provider) {
        myCurrentProvider = provider;

        if (provider == null) {
            myProviderLayout.swipeRightTo(EMPTY_PANEL);
            setOKActionText(VcsLocalize.checkoutButtonText());
            setOKActionEnabled(false);
            return;
        }

        String id = myProviderIds.get(provider);
        if (id == null) {
            id = "provider-" + (++myProviderCounter);
            myProviderIds.put(provider, id);
            myProviderLayout.register(id, buildProviderPanel(provider));
        }

        myProviderLayout.swipeLeftTo(id);
        setOKActionText(provider.getActionName());
        setOKActionEnabled(Boolean.TRUE.equals(myProviderStates.get(provider)));
    }

    @RequiredUIAccess
    private Layout buildProviderPanel(CheckoutProvider provider) {
        myProviderStates.put(provider, false);

        CheckoutPage page = provider.createPage(myProject, this);
        myProviderPages.put(provider, page);

        Component form = page.createComponent(enabled -> {
            myProviderStates.put(provider, enabled);

            if (myCurrentProvider == provider) {
                setOKActionEnabled(enabled);
            }
        });

        DockLayout layout = DockLayout.create();
        layout.center(form);

        myOpenProjectFromDirectory = CheckBox.create(LocalizeValue.localizeTODO("Open Project From Directory"));
        myOpenProjectFromDirectory.setValue(true);

        layout.bottom(myOpenProjectFromDirectory);

        layout.paddingBuilder().allSet(Space.MEDIUM).apply();

        return layout;
    }

    @RequiredUIAccess
    public void setOKActionEnabled(boolean enabled) {
        if (myOkButton != null) {
            myOkButton.setEnabled(enabled);
        }
    }

    @RequiredUIAccess
    public void setOKActionText(LocalizeValue text) {
        if (myOkButton != null) {
            myOkButton.setText(text);
        }
    }

    @RequiredUIAccess
    public void doOkAction() {
        CheckoutPage page = myProviderPages.get(myCurrentProvider);
        if (page != null) {
            UIAccess uiAccess = UIAccess.current();

            CheckoutCallback callback =
                ProjectLevelVcsManagerEx.getInstanceEx(myProject).getCompositeCheckoutCallback(uiAccess, myOpenProjectFromDirectory.getValueOrError());

            page.doCheckout(callback);
        }

        doCancelAction();
    }

    @RequiredUIAccess
    public void doCancelAction() {
        if (myCloseAction != null) {
            myCloseAction.run();
        }
    }

    @Override
    public void dispose() {
    }
}
