package consulo.endpoint.impl.internal.view.example;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.DisposableCoroutineScope;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.codeEditor.EditorFactory;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.endpoint.EndpointElementItem;
import consulo.endpoint.EndpointListItem;
import consulo.endpoint.EndpointSidePanel;
import consulo.endpoint.EndpointUrlTargetProvider;
import consulo.endpoint.client.generator.AvailableClientSettings;
import consulo.endpoint.client.generator.ClientExample;
import consulo.endpoint.client.generator.ClientGenerator;
import consulo.endpoint.client.generator.ClientGeneratorOpenInScratchService;
import consulo.endpoint.client.generator.ClientGeneratorSetting;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.oas.OasExportUtil;
import consulo.endpoint.oas.OpenApiSpecification;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.LabeledBuilder;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

final class EndpointExampleSidePanel implements EndpointSidePanel, Disposable {
    private final Project myProject;
    private final EditorFactory myEditorFactory;
    private final List<ClientGenerator> myGenerators;

    private final DockLayout myRoot;
    private final DockLayout myContent;
    private final ComboBox<ClientGenerator> myGeneratorComboBox;
    private final MutableFlatDataModel<String> myLanguageModel;
    private final ComboBox<String> myLanguageComboBox;
    private final Component myLanguageRow;
    private final MutableFlatDataModel<String> myVersionModel;
    private final ComboBox<String> myVersionComboBox;
    private final Component myVersionRow;
    private final CheckBox myBoilerplateCheckBox;
    private final ActionToolbar myToolbar;
    private final EditorBox myEditorBox;
    private final Label myUnavailableLabel;

    private volatile @Nullable OpenApiSpecification myOpenApiSpecification;
    private volatile @Nullable ClientGenerator mySelectedGenerator;
    private @Nullable Disposable myGenerateDisposable;
    private boolean myUpdatingControls;

    @RequiredUIAccess
    EndpointExampleSidePanel(
        Project project,
        EditorFactory editorFactory,
        EditorBoxBuilderFactory editorBoxBuilderFactory,
        List<ClientGenerator> generators
    ) {
        myProject = project;
        myEditorFactory = editorFactory;
        myGenerators = List.copyOf(generators);

        myGeneratorComboBox = ComboBox.create(myGenerators);
        myGeneratorComboBox.setTextRenderer(generator -> generator == null ? LocalizeValue.empty() : generator.getTitle());

        myLanguageModel = FlatDataModel.of(List.of());
        myLanguageComboBox = ComboBox.create(myLanguageModel);
        myLanguageComboBox.setTextRenderer(EndpointExampleSidePanel::presentSetting);
        myLanguageRow = LabeledBuilder.simple(EndpointLocalize.clientGeneratorLanguageLabel(), myLanguageComboBox);

        myVersionModel = FlatDataModel.of(List.of());
        myVersionComboBox = ComboBox.create(myVersionModel);
        myVersionComboBox.setTextRenderer(EndpointExampleSidePanel::presentSetting);
        myVersionRow = LabeledBuilder.simple(EndpointLocalize.clientGeneratorVersionLabel(), myVersionComboBox);

        myBoilerplateCheckBox = CheckBox.create(EndpointLocalize.clientGeneratorBoilerplateCheckbox());

        HorizontalLayout settings = HorizontalLayout.create();
        settings.add(LabeledBuilder.simple(EndpointLocalize.clientGeneratorComboboxLabel(), myGeneratorComboBox));
        settings.add(myLanguageRow);
        settings.add(myVersionRow);
        settings.add(myBoilerplateCheckBox);

        myEditorBox = editorBoxBuilderFactory.create(project)
            .viewer()
            .multiline()
            .editorFont()
            .build();
        myUnavailableLabel = Label.create(EndpointLocalize.clientGeneratorUnavailable());

        myRoot = DockLayout.create(Space.NONE);

        myToolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            "EndpointExamples",
            ActionGroup.newImmutableBuilder().add(new SaveToScratchAction()).build(),
            ActionToolbar.Style.INPLACE
        );
        myToolbar.setTargetUIComponent(myRoot);

        DockLayout header = DockLayout.create(Space.NONE);
        header.center(settings);
        header.right(myToolbar.getUIComponent());

        myContent = DockLayout.create(Space.NONE);
        myContent.center(myUnavailableLabel);

        myRoot.top(header);
        myRoot.center(myContent);

        if (!myGenerators.isEmpty()) {
            myGeneratorComboBox.setValue(myGenerators.get(0), false);
        }
        mySelectedGenerator = myGeneratorComboBox.getValue();
        refreshSettingControls();

        myGeneratorComboBox.addValueListener(event -> {
            mySelectedGenerator = myGeneratorComboBox.getValue();
            myToolbar.updateActionsAsync();
            refreshSettingControls();
            regenerate();
        });
        myLanguageComboBox.addValueListener(event -> onSettingChanged());
        myVersionComboBox.addValueListener(event -> onSettingChanged());
        myBoilerplateCheckBox.addValueListener(event -> onSettingChanged());

        myToolbar.updateActionsAsync();
    }

    @Override
    public LocalizeValue getTitle() {
        return EndpointLocalize.endpointsDetailsExamplesTitle();
    }

    @RequiredUIAccess
    @Override
    public Component getComponent() {
        return myRoot;
    }

    @Override
    public Coroutine<?, Boolean> isAvailable(List<? extends EndpointListItem> selectedItems) {
        return Coroutine.first(ReadLock.<Object, Boolean>apply(ignored -> !myGenerators.isEmpty() && hasOpenApi(selectedItems)));
    }

    @Override
    public Coroutine<?, ?> update(List<? extends EndpointListItem> selectedItems) {
        AtomicReference<@Nullable Disposable> generation = new AtomicReference<>();
        return Coroutine
            .first(ReadLock.<Object, Optional<OpenApiSpecification>>apply(ignored -> computeOpenApiSpecification(selectedItems)))
            .then(UIAction.<Optional<OpenApiSpecification>, Optional<EndpointExampleRequest>>apply(specification -> {
                cancelGeneration();
                Disposable generateDisposable = Disposable.newDisposable("EndpointExampleGeneration");
                Disposer.register(this, generateDisposable);
                myGenerateDisposable = generateDisposable;
                generation.set(generateDisposable);
                myOpenApiSpecification = specification.orElse(null);
                myToolbar.updateActionsAsync();
                return createRequest();
            }))
            .then(ReadLock.<Optional<EndpointExampleRequest>, Optional<ClientExample>>apply(EndpointExampleSidePanel::generate))
            .then(UIAction.<Optional<ClientExample>, Optional<ClientExample>>apply(example -> {
                Disposable generateDisposable = generation.get();
                if (generateDisposable != null && myGenerateDisposable == generateDisposable) {
                    myGenerateDisposable = null;
                    Disposer.dispose(generateDisposable);
                    showExample(example);
                }
                return example;
            }));
    }

    @Override
    public void dispose() {
    }

    @RequiredReadAction
    private boolean hasOpenApi(List<? extends EndpointListItem> selectedItems) {
        for (EndpointListItem item : selectedItems) {
            if (item instanceof EndpointElementItem<?, ?> elementItem
                && elementItem.getProvider() instanceof EndpointUrlTargetProvider<?, ?> urlTargetProvider
                && urlTargetProvider.shouldShowOpenApiPanel()
                && elementItem.isValid()) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    private Optional<OpenApiSpecification> computeOpenApiSpecification(List<? extends EndpointListItem> selectedItems) {
        if (myGenerators.isEmpty()) {
            return Optional.empty();
        }
        List<EndpointListItem> validItems = new ArrayList<>();
        for (EndpointListItem item : selectedItems) {
            if (!(item instanceof EndpointElementItem<?, ?> elementItem) || elementItem.isValid()) {
                validItems.add(item);
            }
        }
        return Optional.ofNullable(OasExportUtil.getOpenApiSpecification(validItems));
    }

    @RequiredReadAction
    private static Optional<ClientExample> generate(Optional<EndpointExampleRequest> request) {
        if (request.isEmpty()) {
            return Optional.empty();
        }
        EndpointExampleRequest exampleRequest = request.get();
        ClientGenerator generator = exampleRequest.generator();
        ClientGeneratorSetting actualSettings = generator.getAvailableClientSettings().getActualClientSettings();
        synchronized (actualSettings) {
            exampleRequest.settings().applyTo(actualSettings);
            return Optional.ofNullable(generator.generate(exampleRequest.openApiSpecification()));
        }
    }

    @RequiredUIAccess
    private Optional<EndpointExampleRequest> createRequest() {
        ClientGenerator generator = myGeneratorComboBox.getValue();
        OpenApiSpecification specification = myOpenApiSpecification;
        if (generator == null || specification == null) {
            return Optional.empty();
        }
        EndpointExampleSettings settings = new EndpointExampleSettings(
            myBoilerplateCheckBox.isVisible() && Boolean.TRUE.equals(myBoilerplateCheckBox.getValue()),
            myLanguageRow.isVisible() ? myLanguageComboBox.getValue() : null,
            myVersionRow.isVisible() ? myVersionComboBox.getValue() : null
        );
        return Optional.of(new EndpointExampleRequest(generator, specification, settings));
    }

    @RequiredUIAccess
    private void onSettingChanged() {
        if (!myUpdatingControls) {
            regenerate();
        }
    }

    @RequiredUIAccess
    private void regenerate() {
        cancelGeneration();
        myToolbar.updateActionsAsync();

        Optional<EndpointExampleRequest> request = createRequest();
        if (request.isEmpty()) {
            showExample(Optional.empty());
            return;
        }

        Disposable generateDisposable = Disposable.newDisposable("EndpointExampleGeneration");
        Disposer.register(this, generateDisposable);
        myGenerateDisposable = generateDisposable;

        DisposableCoroutineScope.launchAsync(
            myProject.coroutineContext(),
            generateDisposable,
            () -> Coroutine
                .first(ReadLock.<Object, Optional<ClientExample>>apply(ignored -> generate(request)))
                .then(UIAction.<Optional<ClientExample>, Optional<ClientExample>>apply(example -> {
                    if (myGenerateDisposable == generateDisposable) {
                        showExample(example);
                    }
                    return example;
                }))
        );
    }

    @RequiredUIAccess
    private void cancelGeneration() {
        Disposable generateDisposable = myGenerateDisposable;
        if (generateDisposable != null) {
            myGenerateDisposable = null;
            Disposer.dispose(generateDisposable);
        }
    }

    @RequiredUIAccess
    private void showExample(Optional<ClientExample> example) {
        if (example.isEmpty()) {
            myContent.center(myUnavailableLabel);
            return;
        }
        ClientExample clientExample = example.get();
        String text = StringUtil.convertLineSeparators(clientExample.getText());
        myEditorBox.setDocument(myEditorFactory.createDocument(text), clientExample.getFileType());
        myContent.center(myEditorBox);
    }

    @RequiredUIAccess
    private void refreshSettingControls() {
        myUpdatingControls = true;
        try {
            ClientGenerator generator = myGeneratorComboBox.getValue();
            if (generator == null) {
                myLanguageRow.setVisible(false);
                myVersionRow.setVisible(false);
                myBoilerplateCheckBox.setVisible(false);
                return;
            }

            AvailableClientSettings availableSettings = generator.getAvailableClientSettings();
            ClientGeneratorSetting actualSettings = availableSettings.getActualClientSettings();

            refreshSettingComboBox(
                myLanguageModel,
                myLanguageComboBox,
                myLanguageRow,
                availableSettings.getFrameworkLanguages(),
                actualSettings.getFrameworkLanguage()
            );
            refreshSettingComboBox(
                myVersionModel,
                myVersionComboBox,
                myVersionRow,
                availableSettings.getFrameworkVersions(),
                actualSettings.getFrameworkVersion()
            );

            myBoilerplateCheckBox.setValue(actualSettings.getBoilerplate(), false);
            myBoilerplateCheckBox.setVisible(availableSettings.getBoilerplateAvailable());
        }
        finally {
            myUpdatingControls = false;
        }
    }

    @RequiredUIAccess
    private static void refreshSettingComboBox(
        MutableFlatDataModel<String> model,
        ComboBox<String> comboBox,
        Component row,
        Set<String> values,
        @Nullable String actualValue
    ) {
        List<String> items = List.copyOf(values);
        model.replaceAll(items);
        if (items.isEmpty()) {
            comboBox.setValue(null, false);
            row.setVisible(false);
            return;
        }
        comboBox.setValue(actualValue != null && items.contains(actualValue) ? actualValue : items.get(0), false);
        row.setVisible(true);
    }

    private static LocalizeValue presentSetting(@Nullable String value) {
        return value == null ? LocalizeValue.empty() : LocalizeValue.of(value);
    }

    private final class SaveToScratchAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private SaveToScratchAction() {
            super(EndpointLocalize.commandExportClientExample(), LocalizeValue.empty(), PlatformIconGroup.actionsScratch());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            ClientGenerator generator = myGeneratorComboBox.getValue();
            OpenApiSpecification specification = myOpenApiSpecification;
            if (generator == null || specification == null) {
                return;
            }
            ClientGeneratorOpenInScratchService.getInstance(myProject)
                .createScratchFileWithoutEndpointsChangeTracking(generator, specification);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(mySelectedGenerator != null && myOpenApiSpecification != null);
        }
    }
}
