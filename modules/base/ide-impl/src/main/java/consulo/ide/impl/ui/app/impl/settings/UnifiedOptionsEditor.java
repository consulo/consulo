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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.Settings;
import consulo.configurable.internal.ConfigurableSearchHits;
import consulo.configurable.internal.ConfigurableSearchWalker;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.configurable.ConfigurablePreselectStrategy;
import consulo.ide.impl.configurable.ProjectStructureSelectorOverSettings;
import consulo.ide.setting.ProjectStructureSelector;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.AdvancedLabel;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.KeyPressedEvent;
import consulo.ui.event.MousePressedEvent;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class UnifiedOptionsEditor implements OptionsEditorHost, Disposable {
    private static final Logger LOG = Logger.getInstance(UnifiedOptionsEditor.class);

    private static final long MODIFICATION_CHECK_PERIOD = 1000;
    private static final long ACTION_MODIFICATION_CHECK_DELAY = 300;
    private static final long FILTER_DELAY = 300;
    private static final long SPOTLIGHT_DELAY = 200;

    private final Project myProject;
    private final OptionsEditorCore myCore;

    private final UnifiedOptionsTree myTree;
    private final TextBox mySearch;
    private final DockLayout mySearchPanel;

    private final UnifiedOptionsBanner myBanner;
    private final LoadingLayout<DockLayout> myContentLoading;
    private final Label myEmptyContent;
    private final AdvancedLabel myErrorMessage;
    private final VerticalLayout myErrorStrip;

    private final DockLayout myLeft;
    private final DockLayout myCenter;

    private final Map<Configurable, UnifiedOptionsPage> myPages = new HashMap<>();
    private final Map<Configurable, CompletableFuture<UnifiedOptionsPage>> myPageLoads = new HashMap<>();
    private final Set<Configurable> myBrokenPages = new HashSet<>();
    private final OptionsLiveIndex myLiveIndex = new OptionsLiveIndex();

    private Configurable @Nullable [] myRegisteredConfigurables;
    private @Nullable Component myShownContent;
    private boolean myTreeShown;
    private boolean myModificationCheckFailed;
    private String myFilterText = "";
    private @Nullable UnifiedOptionsPage myLitPage;
    private boolean mySearchFieldActive;

    private volatile @Nullable ScheduledFuture<?> myQueuedModificationCheck;
    private @Nullable ScheduledFuture<?> myQueuedRefilter;
    private @Nullable ScheduledFuture<?> myQueuedSpotlight;
    private volatile boolean myDisposed;

    @RequiredUIAccess
    public UnifiedOptionsEditor(Project project, Function<Project, Configurable[]> configurablesBuilder) {
        myProject = project;

        myCore = new OptionsEditorCore(project, configurablesBuilder, this);
        myCore.getFilter().setLiveIndex(myLiveIndex);
        myCore.getContext().addColleague(new OptionsEditorColleague() {
            @Override
            @RequiredUIAccess
            public CompletableFuture<Void> onSelected(@Nullable Configurable configurable, @Nullable Configurable oldConfigurable) {
                return processSelected(configurable, oldConfigurable);
            }

            @Override
            @RequiredUIAccess
            public CompletableFuture<Void> onModifiedAdded(Configurable configurable) {
                return updateIfCurrent(configurable);
            }

            @Override
            @RequiredUIAccess
            public CompletableFuture<Void> onModifiedRemoved(Configurable configurable) {
                return updateIfCurrent(configurable);
            }

            @Override
            @RequiredUIAccess
            public CompletableFuture<Void> onErrorsChanged() {
                updateErrorStrip();
                return updateIfCurrent(myCore.getContext().getCurrentConfigurable());
            }
        });

        myTree = new UnifiedOptionsTree(
            project.getApplication().getInstance(ApplicationTreeExecutorFactory.class),
            myCore::getOrBuildConfigurables,
            myCore.getContext(),
            configurable -> myCore.getFilter().shouldBeShowing(configurable),
            myCore::getProjectIconTooltip
        );
        Disposer.register(this, myTree);
        myTree.addKeyListener(this::onTreeKeyPressed);
        myTree.getComponent().addMousePressedListener(event -> mySearchFieldActive = false);

        mySearch = TextBox.create();
        mySearch.setPlaceholder(ConfigurableLocalize.configurableSearchPlaceholder());
        mySearch.addStyle(TextBoxStyle.HAS_CLEAR_BUTTON);
        Label searchIcon = Label.create();
        searchIcon.setImage(PlatformIconGroup.actionsSearch());
        mySearch.setPrefixComponent(searchIcon);
        mySearch.addValueListener(event -> onSearchTextChanged(StringUtil.notNullize(event.getValue())));
        mySearch.addKeyPressedListener(this::onSearchKeyPressed);
        mySearch.addMousePressedListener(this::onSearchPressed);

        mySearchPanel = DockLayout.create(Space.NONE);
        mySearchPanel.center(mySearch);
        mySearchPanel.paddingBuilder().allSet(Space.MEDIUM).apply();

        myLeft = DockLayout.create(Space.NONE);
        myLeft.top(mySearchPanel);
        myLeft.center(ScrollableLayout.create(myTree.getComponent()));
        myLeft.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);

        myBanner = new UnifiedOptionsBanner();
        myBanner.getComponent().setVisible(false);

        myEmptyContent = Label.create(
            ConfigurableLocalize.configurableEmptyContentText(),
            LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build()
        );

        myContentLoading = LoadingLayout.create(DockLayout.create(Space.NONE), this);
        showContent(myEmptyContent);

        AdvancedLabel errorTitle = AdvancedLabel.create();
        errorTitle.updatePresentation(presentation -> {
            presentation.withIcon(PlatformIconGroup.generalError());
            presentation.append(ConfigurableLocalize.configurableChangesNotAppliedError(), TextAttribute.ERROR);
        });

        myErrorMessage = AdvancedLabel.create();

        myErrorStrip = VerticalLayout.create(Space.X_SMALL);
        myErrorStrip.add(errorTitle);
        myErrorStrip.add(myErrorMessage);
        myErrorStrip.paddingBuilder().allSet(Space.MEDIUM).apply();
        myErrorStrip.setVisible(false);

        myCenter = DockLayout.create(Space.NONE);
        myCenter.top(myBanner.getComponent());
        myCenter.center(myContentLoading);
        myCenter.bottom(myErrorStrip);
        myCenter.addMousePressedListener(this::onContentPressed);
        myCenter.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);

        project.getApplication().getInstance(ActionManager.class).addAnActionListener(new AnActionListener() {
            @Override
            public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                queueModificationCheck();
            }
        }, this);
    }

    public Component getLeftComponent() {
        return myLeft;
    }

    public Component getCenterComponent() {
        return myCenter;
    }

    public Component getPreferredFocusedComponent() {
        return myTree.getComponent();
    }

    public OptionsEditorCore getCore() {
        return myCore;
    }

    public Settings getSettings() {
        return myCore.getSettings();
    }

    @RequiredUIAccess
    public void load(ConfigurablePreselectStrategy preselectStrategy, String filter, Runnable afterLoad) {
        myCore.doSelect(preselectStrategy, filter, afterLoad);
    }

    @RequiredUIAccess
    public void doSelect(ConfigurablePreselectStrategy preselectStrategy, String filter, Runnable afterLoad) {
        myCore.doSelect(preselectStrategy, filter, afterLoad);
    }

    @RequiredUIAccess
    public boolean hasErrors() {
        return !myCore.getContext().getErrors().isEmpty();
    }

    @Override
    public Collection<Configurable> getTreeConfigurables() {
        List<Configurable> result = new ArrayList<>();
        collectConfigurables(myCore.getConfigurables(), result, new HashSet<>());
        return result;
    }

    @Override
    public String getFilterText() {
        return StringUtil.notNullize(mySearch.getValue());
    }

    @Override
    @RequiredUIAccess
    public void clearFilter() {
        mySearch.setValue("");
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> refilter(String text, boolean adjustSelection, boolean now) {
        mySearch.setValue(text, false);
        return updateFilter(text, OptionsEditorFilterChange.CHANGE, adjustSelection, true);
    }

    @Override
    @RequiredUIAccess
    public void rebuildTree(Runnable afterRebuild) {
        UIAccess uiAccess = UIAccess.current();

        myTree.rebuild().whenCompleteAsync((value, error) -> {
            if (myDisposed) {
                return;
            }

            registerKids();

            if (!myTreeShown) {
                myTreeShown = true;

                startModificationPolling(uiAccess);
            }

            try {
                afterRebuild.run();
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }, uiAccess);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> selectInTree(@Nullable Configurable configurable) {
        return myTree.select(configurable);
    }

    @Override
    public boolean isPageCreated(Configurable configurable) {
        return myPages.containsKey(configurable);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> createPage(Configurable configurable) {
        try {
            getOrCreatePage(configurable);
            return CompletableFuture.completedFuture(null);
        }
        catch (Throwable e) {
            LOG.warn(e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public boolean isPageEditable(Configurable configurable) {
        UnifiedOptionsPage page = myPages.get(configurable);
        return page != null && page.isSupported() && !myBrokenPages.contains(configurable);
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @RequiredUIAccess
    private CompletableFuture<Void> processSelected(@Nullable Configurable configurable, @Nullable Configurable oldConfigurable) {
        if (configurable == null) {
            myBanner.getComponent().setVisible(false);
            showContent(myEmptyContent);

            updateSpotlight();

            myCore.checkModified(oldConfigurable);
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<Void> result = new CompletableFuture<>();

        getPage(configurable, uiAccess).whenCompleteAsync((page, error) -> {
            if (error != null) {
                if (myCore.getContext().getCurrentConfigurable() == configurable) {
                    myContentLoading.stopLoading(layout -> {
                    });
                }
                result.completeExceptionally(error);
                return;
            }

            if (myDisposed || myCore.getContext().getCurrentConfigurable() != configurable) {
                result.completeExceptionally(new CancellationException());
                return;
            }

            showPage(page);

            myCore.checkModified(oldConfigurable);
            myCore.checkModified(configurable);

            result.complete(null);
        }, uiAccess);

        return result;
    }

    @RequiredUIAccess
    private CompletableFuture<UnifiedOptionsPage> getPage(Configurable configurable, UIAccess uiAccess) {
        if (myDisposed) {
            return CompletableFuture.failedFuture(new CancellationException());
        }

        UnifiedOptionsPage page = myPages.get(configurable);
        if (page != null) {
            return CompletableFuture.completedFuture(page);
        }

        CompletableFuture<UnifiedOptionsPage> pending = myPageLoads.get(configurable);
        if (pending != null) {
            return pending;
        }

        CompletableFuture<UnifiedOptionsPage> result = new CompletableFuture<>();
        myPageLoads.put(configurable, result);

        myBanner.getComponent().setVisible(false);
        myShownContent = null;
        myContentLoading.startLoading();

        uiAccess.give(() -> {
            myPageLoads.remove(configurable);

            if (myDisposed || myProject.isDisposed()) {
                result.completeExceptionally(new CancellationException());
                return;
            }

            try {
                result.complete(getOrCreatePage(configurable));
            }
            catch (Throwable e) {
                LOG.warn(e);
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    @RequiredUIAccess
    private UnifiedOptionsPage getOrCreatePage(Configurable configurable) {
        UnifiedOptionsPage page = myPages.get(configurable);
        if (page != null) {
            return page;
        }

        UnifiedOptionsPage newPage = new UnifiedOptionsPage(configurable);
        myPages.put(configurable, newPage);

        if (newPage.isSupported()) {
            try {
                configurable.initialize();
                configurable.reset();
            }
            catch (Throwable e) {
                myBrokenPages.add(configurable);
                LOG.error(e);
            }
        }

        indexPage(newPage);

        return newPage;
    }

    @RequiredUIAccess
    private void showPage(UnifiedOptionsPage page) {
        Configurable configurable = page.getConfigurable();

        List<LocalizeValue> bannerText = myCore.getBannerText(configurable);
        LocalizeValue projectTooltip = myCore.getProjectIconTooltip(configurable);

        page.update(bannerText, projectTooltip, myCore.getContext().getErrors().get(configurable));

        boolean standardBanner = page.hasContent() && !page.isFullContent();
        if (standardBanner) {
            myBanner.update(bannerText, projectTooltip);
        }
        myBanner.getComponent().setVisible(standardBanner);

        showContent(page.getComponent());

        queueSpotlightUpdate();
    }

    @RequiredUIAccess
    private void indexPage(UnifiedOptionsPage page) {
        Component content = page.getContent();
        Configurable configurable = page.getConfigurable();
        if (content == null || myBrokenPages.contains(configurable)) {
            return;
        }

        try {
            myLiveIndex.index(configurable, content);
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    @RequiredUIAccess
    private void queueSpotlightUpdate() {
        if (myDisposed) {
            return;
        }

        ScheduledFuture<?> queued = myQueuedSpotlight;
        if (queued != null) {
            queued.cancel(false);
        }

        myQueuedSpotlight = UIAccess.current().getScheduler().schedule(() -> {
            myQueuedSpotlight = null;
            updateSpotlight();
        }, SPOTLIGHT_DELAY, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private void updateSpotlight() {
        if (myDisposed) {
            return;
        }

        Configurable current = myCore.getContext().getCurrentConfigurable();
        UnifiedOptionsPage page = current == null ? null : myPages.get(current);
        if (current == null || page == null || myShownContent != page.getComponent()) {
            clearLitPage();
            return;
        }

        if (myLitPage != page) {
            clearLitPage();
        }

        indexPage(page);

        String text = getFilterText().trim();
        Component content = page.getContent();
        if (content == null || text.isEmpty() || !myCore.getContext().isHoldingFilter() || !isPageEditable(current)) {
            page.clearSpotlight();
        }
        else {
            lightOptions(page, content, current, text);
        }

        enableSearchForCurrent();
    }

    @RequiredUIAccess
    private void lightOptions(UnifiedOptionsPage page, Component content, Configurable configurable, String text) {
        try {
            SearchableConfigurable searchable = OptionsLiveIndex.searchableOf(configurable);
            ConfigurableSearchHits hits = ConfigurableSearchWalker.findHits(
                content,
                (option, force) -> OptionsSearchMatcher.matches(option, text, force, searchable)
            );

            for (Tab tab : hits.getTabsToSelect()) {
                tab.select();
            }

            page.getSpotlight().setTargets(hits.getTargets(), hits.getTabTargets());
            myLitPage = page;
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    @RequiredUIAccess
    private void clearLitPage() {
        UnifiedOptionsPage litPage = myLitPage;
        myLitPage = null;
        if (litPage != null) {
            litPage.clearSpotlight();
        }
    }

    @RequiredUIAccess
    private void onContentPressed(MousePressedEvent event) {
        mySearchFieldActive = false;

        queueModificationCheck();

        myCore.getContext().setHoldingFilter(false);
        queueSpotlightUpdate();
    }

    @RequiredUIAccess
    private void onSearchPressed(MousePressedEvent event) {
        boolean hasText = !getFilterText().isEmpty();
        if (!myCore.getContext().isHoldingFilter() && hasText) {
            myCore.getContext().setHoldingFilter(true);
            queueSpotlightUpdate();
        }

        if (!mySearchFieldActive && hasText) {
            String pressedText = getFilterText();
            UIAccess.current().execute(() -> {
                if (!myDisposed && pressedText.equals(getFilterText())) {
                    mySearch.selectAll();
                }
            });
        }

        mySearchFieldActive = true;
    }

    @RequiredUIAccess
    private void enableSearchForCurrent() {
        Configurable current = myCore.getContext().getCurrentConfigurable();
        if (myDisposed || current == null || !isPageEditable(current)) {
            return;
        }

        try {
            myCore.getFilter().enableSearch(current, getFilterText().trim());
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    @RequiredUIAccess
    private void showContent(Component content) {
        if (myShownContent == content) {
            myContentLoading.stopLoading(layout -> {
            });
            return;
        }

        myShownContent = content;
        myContentLoading.stopLoading(layout -> layout.center(content));
    }

    @RequiredUIAccess
    private CompletableFuture<Void> updateIfCurrent(@Nullable Configurable configurable) {
        if (configurable == null || myCore.getContext().getCurrentConfigurable() != configurable) {
            return CompletableFuture.failedFuture(new CancellationException());
        }

        UnifiedOptionsPage page = myPages.get(configurable);
        if (page != null) {
            ConfigurationException error = myCore.getContext().getErrors().get(configurable);
            page.update(myCore.getBannerText(configurable), myCore.getProjectIconTooltip(configurable), error);
        }
        return CompletableFuture.completedFuture(null);
    }

    @RequiredUIAccess
    private void updateErrorStrip() {
        if (myDisposed) {
            return;
        }

        if (!hasErrors()) {
            myErrorStrip.setVisible(false);
            return;
        }

        String message = myCore.getErrorMessage();
        myErrorMessage.updatePresentation(presentation -> presentation.append(LocalizeValue.ofNullable(message)));
        myErrorMessage.setVisible(!StringUtil.isEmpty(message));
        myErrorStrip.setVisible(true);
    }

    @RequiredUIAccess
    private void onSearchTextChanged(String text) {
        String oldText = myFilterText;

        OptionsEditorFilterChange change;
        if (text.length() > oldText.length() && text.startsWith(oldText)) {
            change = OptionsEditorFilterChange.INSERT;
        }
        else if (text.length() < oldText.length() && oldText.startsWith(text)) {
            change = OptionsEditorFilterChange.REMOVE;
        }
        else {
            change = OptionsEditorFilterChange.CHANGE;
        }

        updateFilter(text, change, true, false);
    }

    @RequiredUIAccess
    private CompletableFuture<?> updateFilter(String text, OptionsEditorFilterChange change, boolean adjustSelection, boolean now) {
        myFilterText = text;

        OptionsEditorFilter filter = myCore.getFilter();
        Configurable toSelect = filter.update(text, change, adjustSelection);

        mySearch.setBackgroundColor(filter.isNothingFound() ? ComponentColors.ERROR_BACKGROUND : null);

        queueSpotlightUpdate();

        Configurable target = toSelect != null ? toSelect : myCore.getContext().getCurrentConfigurable();

        ScheduledFuture<?> queued = myQueuedRefilter;
        if (queued != null) {
            queued.cancel(false);
            myQueuedRefilter = null;
        }

        UIAccess uiAccess = UIAccess.current();
        if (now) {
            return refilterAndSelect(adjustSelection, target, uiAccess);
        }

        CompletableFuture<Object> result = new CompletableFuture<>();
        myQueuedRefilter = uiAccess.getScheduler().schedule(() -> {
            myQueuedRefilter = null;

            if (myDisposed) {
                result.complete(null);
                return;
            }

            refilterAndSelect(adjustSelection, target, uiAccess).whenComplete((value, error) -> result.complete(null));
        }, FILTER_DELAY, TimeUnit.MILLISECONDS);
        return result;
    }

    @RequiredUIAccess
    private CompletableFuture<?> refilterAndSelect(boolean adjustSelection, @Nullable Configurable target, UIAccess uiAccess) {
        return myTree.refilter()
            .thenComposeAsync(ignored -> selectAfterFilter(adjustSelection, target), uiAccess);
    }

    @RequiredUIAccess
    private CompletableFuture<?> selectAfterFilter(boolean adjustSelection, @Nullable Configurable target) {
        if (myDisposed || !adjustSelection) {
            return CompletableFuture.completedFuture(null);
        }
        return myTree.selectOrFirst(target);
    }

    @RequiredUIAccess
    private void onSearchKeyPressed(KeyPressedEvent event) {
        KeyboardInputDetails details = event.getInputDetails();
        KeyCode keyCode = details.getKeyCode();
        if (keyCode == null || details.withCtrl() || details.withAlt() || details.withMeta() || details.withShift()) {
            return;
        }

        int key = keyCode.key();
        if (key == KeyCode.ESCAPE.key()) {
            if (!getFilterText().isEmpty()) {
                UIAccess.current().execute(() -> {
                    if (!myDisposed) {
                        clearFilter();
                    }
                });
            }
        }
        else if (key == KeyCode.UP.key()) {
            myTree.moveSelection(false);
        }
        else if (key == KeyCode.DOWN.key()) {
            myTree.moveSelection(true);
        }
    }

    @RequiredUIAccess
    private void onTreeKeyPressed(KeyPressedEvent event) {
        KeyboardInputDetails details = event.getInputDetails();
        KeyCode keyCode = details.getKeyCode();
        if (keyCode == null || details.withCtrl() || details.withAlt() || details.withMeta()) {
            return;
        }

        int key = keyCode.key();

        char typed;
        if (key >= 'A' && key <= 'Z') {
            typed = details.withShift() ? (char) key : Character.toLowerCase((char) key);
        }
        else if (key >= '0' && key <= '9' && !details.withShift()) {
            typed = (char) key;
        }
        else {
            return;
        }

        String text = getFilterText() + typed;
        mySearch.setValue(text);
        mySearch.focus();
        mySearch.moveCaretTo(text.length());
        mySearchFieldActive = true;
    }

    @RequiredUIAccess
    private void startModificationPolling(UIAccess uiAccess) {
        ScheduledFuture<?> future = uiAccess.getScheduler().scheduleWithFixedDelay(
            this::checkCurrentModified,
            MODIFICATION_CHECK_PERIOD,
            MODIFICATION_CHECK_PERIOD,
            TimeUnit.MILLISECONDS
        );
        Disposer.register(this, () -> future.cancel(false));
    }

    private void queueModificationCheck() {
        if (myDisposed) {
            return;
        }

        UIAccess uiAccess = myCenter.getUIAccess();
        if (uiAccess == null || !uiAccess.isValid()) {
            return;
        }

        ScheduledFuture<?> previous = myQueuedModificationCheck;
        if (previous != null) {
            previous.cancel(false);
        }

        myQueuedModificationCheck = uiAccess.getScheduler().schedule(
            this::checkCurrentModified,
            ACTION_MODIFICATION_CHECK_DELAY,
            TimeUnit.MILLISECONDS
        );
    }

    @RequiredUIAccess
    private void checkCurrentModified() {
        if (myDisposed) {
            return;
        }

        try {
            myCore.checkModified(myCore.getContext().getCurrentConfigurable());
        }
        catch (Throwable e) {
            if (!myModificationCheckFailed) {
                myModificationCheckFailed = true;
                LOG.error(e);
            }
        }
    }

    private void registerKids() {
        Configurable[] configurables = myCore.getConfigurables();
        if (myRegisteredConfigurables == configurables) {
            return;
        }

        myRegisteredConfigurables = configurables;

        Set<Configurable> visited = new HashSet<>();
        for (Configurable configurable : configurables) {
            registerKids(configurable, visited);
        }
    }

    private void registerKids(Configurable parent, Set<Configurable> visited) {
        if (!visited.add(parent)) {
            return;
        }

        for (Configurable kid : UnifiedOptionsTreeModel.getKids(parent)) {
            myCore.getContext().registerKid(parent, kid);
            registerKids(kid, visited);
        }
    }

    private static void collectConfigurables(Configurable[] configurables, List<Configurable> result, Set<Configurable> visited) {
        for (Configurable configurable : configurables) {
            if (!visited.add(configurable)) {
                continue;
            }

            result.add(configurable);
            collectConfigurables(UnifiedOptionsTreeModel.getKids(configurable), result, visited);
        }
    }

    private void uiDataSnapshot(DataSink sink) {
        Settings settings = myCore.getSettings();
        sink.set(Settings.KEY, settings);
        sink.lazy(ProjectStructureSelector.KEY, () -> new ProjectStructureSelectorOverSettings(settings));
        sink.set(Project.KEY, myProject);
        sink.set(PlatformDataKeys.UI_DISPOSABLE, this);
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        ScheduledFuture<?> queued = myQueuedModificationCheck;
        if (queued != null) {
            queued.cancel(false);
        }

        ScheduledFuture<?> queuedRefilter = myQueuedRefilter;
        if (queuedRefilter != null) {
            queuedRefilter.cancel(false);
        }

        ScheduledFuture<?> queuedSpotlight = myQueuedSpotlight;
        if (queuedSpotlight != null) {
            queuedSpotlight.cancel(false);
        }

        myCore.dispose(configurable -> {
            configurable.disposeUIResources();

            UnifiedOptionsPage page = myPages.get(configurable);
            if (page != null) {
                Disposer.dispose(page);
            }
        });

        for (CompletableFuture<UnifiedOptionsPage> load : myPageLoads.values()) {
            load.completeExceptionally(new CancellationException());
        }

        myPages.clear();
        myPageLoads.clear();
    }
}
