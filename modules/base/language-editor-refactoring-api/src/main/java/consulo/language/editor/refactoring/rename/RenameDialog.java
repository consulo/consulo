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

package consulo.language.editor.refactoring.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.configurable.ConfigurationException;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.document.util.TextRange;
import consulo.language.editor.action.SelectWordUtil;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.inplace.VariableInplaceRenameHandler;
import consulo.language.editor.refactoring.ui.RefactoringDialogDescriptor;
import consulo.language.editor.refactoring.util.TextOccurrencesUtil;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilder;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.EditorComboBox;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.localize.LanguageLocalize;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.usage.UsageViewUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RenameDialog extends RefactoringDialogDescriptor {
    private static final Logger LOG = Logger.getInstance(RenameDialog.class);
    private static final LocalizeValue REFACTORING_NAME = RefactoringLocalize.renameTitle();

    private @Nullable SuggestedNameInfo mySuggestedNameInfo;

    private final PsiElement myPsiElement;
    private final @Nullable PsiElement myNameSuggestionContext;
    private final @Nullable Editor myEditor;
    private final @Nullable String myHelpID;
    private final String myOldName;
    private final String[] mySuggestedNames;
    private final boolean myTextOccurrencesEnabled;
    private final Map<AutomaticRenamerFactory, Boolean> myAutomaticRenamers = new LinkedHashMap<>();

    private String myNewName;
    private boolean mySearchInComments;
    private boolean mySearchTextOccurrences;
    private @Nullable TextRange myInitialSelection;
    private boolean myInitialSelectionApplied;
    private @Nullable EditorBox myNameBox;
    private @Nullable CheckBox myCbSearchInComments;

    @RequiredUIAccess
    public RenameDialog(
        Project project,
        PsiElement psiElement,
        @Nullable PsiElement nameSuggestionContext,
        @Nullable Editor editor
    ) {
        super(project, REFACTORING_NAME);

        assert psiElement.isValid();

        myPsiElement = psiElement;
        myNameSuggestionContext = nameSuggestionContext;
        myEditor = editor;
        myOldName = UsageViewUtil.getShortName(psiElement);
        mySuggestedNames = getSuggestedNames();
        myNewName = mySuggestedNames.length > 0 ? mySuggestedNames[0] : "";
        myInitialSelection = computeInitialSelection(myNewName);

        mySearchInComments = isToSearchInCommentsForRename();
        myTextOccurrencesEnabled = TextOccurrencesUtil.isSearchTextOccurencesEnabled(psiElement);
        mySearchTextOccurrences = myTextOccurrencesEnabled && isToSearchForTextOccurencesForRename();

        AutomaticRenamerFactory.EP_NAME.forEachExtensionSafe(factory -> {
            if (factory.isApplicable(myPsiElement) && factory.getOptionName().isNotEmpty()) {
                myAutomaticRenamers.put(factory, factory.isEnabled());
            }
        });

        myHelpID = RenamePsiElementProcessor.forElement(psiElement).getHelpID(psiElement);
    }

    @RequiredUIAccess
    public static void showRenameDialog(DataContext dataContext, RenameDialog dialog) {
        if (Application.get().isUnitTestMode()) {
            String name = dataContext.getData(PsiElementRenameHandler.DEFAULT_NAME);
            //noinspection TestOnlyProblems
            dialog.performRename(name);
        }
        else {
            dialog.showAsync();
        }
    }

    @RequiredReadAction
    protected String getLabelText() {
        return RefactoringLocalize.rename0AndItsUsagesTo(getFullName()).get();
    }

    public PsiElement getPsiElement() {
        return myPsiElement;
    }

    @Override
    public @Nullable String getHelpId() {
        return myHelpID;
    }

    @Override
    protected boolean hasPreviewButton() {
        return RenamePsiElementProcessor.forElement(myPsiElement).showRenamePreviewButton(myPsiElement);
    }

    protected boolean isToSearchForTextOccurencesForRename() {
        return RenamePsiElementProcessor.forElement(myPsiElement).isToSearchForTextOccurrences(myPsiElement);
    }

    protected boolean isToSearchInCommentsForRename() {
        return RenamePsiElementProcessor.forElement(myPsiElement).isToSearchInComments(myPsiElement);
    }

    @RequiredReadAction
    protected String getFullName() {
        String name = DescriptiveNameUtil.getDescriptiveName(myPsiElement);
        return (UsageViewUtil.getType(myPsiElement) + " " + name).trim();
    }

    protected boolean shouldSelectAll() {
        return myEditor == null || myEditor.getSettings().isPreselectRename();
    }

    protected void preselectExtension(int start, int end) {
        myInitialSelection = new TextRange(start, end);
    }

    @RequiredUIAccess
    protected void processNewNameChanged() {
        validateButtons();
    }

    public String[] getSuggestedNames() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String initialName = VariableInplaceRenameHandler.getInitialName();
        if (initialName != null) {
            result.add(initialName);
        }
        result.add(UsageViewUtil.getShortName(myPsiElement));
        for (NameSuggestionProvider provider : NameSuggestionProvider.EP_NAME.getExtensionList()) {
            SuggestedNameInfo info = provider.getSuggestedNames(myPsiElement, myNameSuggestionContext, result);
            if (info != null) {
                mySuggestedNameInfo = info;
                if (provider instanceof PreferrableNameSuggestionProvider pnsp && !pnsp.shouldCheckOthers()) {
                    break;
                }
            }
        }
        return ArrayUtil.toStringArray(result);
    }

    public String getNewName() {
        return myNewName.trim();
    }

    public boolean isSearchInComments() {
        return mySearchInComments;
    }

    public boolean isSearchInNonJavaFiles() {
        return mySearchTextOccurrences;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedComponent() {
        EditorBox nameBox = myNameBox;
        if (nameBox != null && !myInitialSelectionApplied) {
            myInitialSelectionApplied = true;
            applyInitialSelection(nameBox);
        }
        return nameBox;
    }

    @Override
    @RequiredUIAccess
    protected Component createContentComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();
        layout.add(Label.create(LocalizeValue.of(getLabelText())));

        EditorBox nameBox = createNameBox();
        myNameBox = nameBox;
        TableLayout nameRow = TableLayout.create(StaticPosition.TOP);
        nameRow.add(nameBox, TableLayout.cell(0, 0).fill());
        layout.add(nameRow);

        List<Component> options = new ArrayList<>();
        createCheckboxes(options);
        if (!options.isEmpty()) {
            TableLayout optionsLayout = TableLayout.create(StaticPosition.TOP);
            for (int i = 0; i < options.size(); i++) {
                optionsLayout.add(options.get(i), TableLayout.cell(i / 2, i % 2).fill());
            }
            layout.add(optionsLayout);
        }
        return layout;
    }

    @RequiredUIAccess
    protected void createCheckboxes(List<Component> options) {
        CheckBox searchInComments = CheckBox.create(RefactoringLocalize.searchInCommentsAndStrings(), mySearchInComments);
        searchInComments.setId("searchInComments");
        searchInComments.addValueListener(event -> mySearchInComments = Boolean.TRUE.equals(event.getValue()));
        myCbSearchInComments = searchInComments;
        options.add(searchInComments);

        if (myTextOccurrencesEnabled) {
            CheckBox searchTextOccurrences = CheckBox.create(RefactoringLocalize.searchForTextOccurrences(), mySearchTextOccurrences);
            searchTextOccurrences.setId("searchTextOccurrences");
            searchTextOccurrences.addValueListener(event -> mySearchTextOccurrences = Boolean.TRUE.equals(event.getValue()));
            options.add(searchTextOccurrences);
        }

        for (Map.Entry<AutomaticRenamerFactory, Boolean> entry : myAutomaticRenamers.entrySet()) {
            CheckBox checkBox = CheckBox.create(entry.getKey().getOptionName(), entry.getValue());
            checkBox.addValueListener(event -> entry.setValue(Boolean.TRUE.equals(event.getValue())));
            options.add(checkBox);
        }
    }

    @RequiredUIAccess
    private EditorBox createNameBox() {
        EditorBoxBuilder builder = myProject.getApplication()
            .getInstance(EditorBoxBuilderFactory.class)
            .create(myProject)
            .text(myNewName)
            .fileType(PlainTextFileType.INSTANCE);

        EditorBox nameBox;
        if (mySuggestedNames.length > 1) {
            EditorComboBox comboBox = builder.buildComboBox();
            comboBox.setItems(List.of(mySuggestedNames));
            nameBox = comboBox;
        }
        else {
            nameBox = builder.build();
        }

        nameBox.setId("newName");
        nameBox.addValueListener(event -> {
            String value = event.getValue();
            myNewName = value == null ? "" : value;
            processNewNameChanged();
        });
        return nameBox;
    }

    private @Nullable TextRange computeInitialSelection(String name) {
        if (myPsiElement instanceof PsiFile && myEditor == null) {
            int pos = name.lastIndexOf('.');
            return pos > 0 ? new TextRange(0, pos) : null;
        }

        Editor editor = myEditor;
        if (editor == null) {
            return new TextRange(0, name.length());
        }

        int offset = editor.getCaretModel().getOffset();
        List<TextRange> ranges = new ArrayList<>();
        SelectWordUtil.addWordSelection(editor.getSettings().isCamelWords(), editor.getDocument().getCharsSequence(), offset, ranges);
        for (TextRange wordRange : ranges) {
            if (!editor.getDocument().getText(wordRange).equals(name)) {
                continue;
            }

            int wordStart = wordRange.getStartOffset();
            SelectionModel selectionModel = editor.getSelectionModel();
            TextRange selected = new TextRange(
                Math.max(0, selectionModel.getSelectionStart() - wordStart),
                Math.max(0, selectionModel.getSelectionEnd() - wordStart)
            ).intersection(new TextRange(0, name.length()));
            if (selectionModel.hasSelection() && selected != null && !selected.isEmpty()) {
                return selected;
            }
            if (shouldSelectAll()) {
                return new TextRange(0, name.length());
            }
            return TextRange.from(Math.min(offset - wordStart, name.length()), 0);
        }
        return null;
    }

    @RequiredUIAccess
    private void applyInitialSelection(EditorBox nameBox) {
        TextRange selection = myInitialSelection;
        if (selection == null) {
            return;
        }

        Editor editor = nameBox.getEditor();
        if (editor == null) {
            if (selection.getStartOffset() == 0 && selection.getEndOffset() == myNewName.length()) {
                nameBox.selectAll();
            }
            return;
        }

        TextRange range = selection.intersection(new TextRange(0, editor.getDocument().getTextLength()));
        if (range == null) {
            return;
        }
        editor.getSelectionModel().removeSelection();
        editor.getCaretModel().moveToOffset(range.getEndOffset());
        if (!range.isEmpty()) {
            editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
        }
    }

    @Override
    @RequiredUIAccess
    protected void doAction() {
        LOG.assertTrue(myPsiElement.isValid());

        performRename(getNewName());
    }

    @RequiredUIAccess
    public void performRename(String newName) {
        RenamePsiElementProcessor elementProcessor = RenamePsiElementProcessor.forElement(myPsiElement);
        elementProcessor.setToSearchInComments(myPsiElement, isSearchInComments());
        if (myTextOccurrencesEnabled) {
            elementProcessor.setToSearchForTextOccurrences(myPsiElement, isSearchInNonJavaFiles());
        }
        if (mySuggestedNameInfo != null) {
            mySuggestedNameInfo.nameChosen(newName);
        }

        RenameProcessor processor = createRenameProcessor(newName);

        for (Map.Entry<AutomaticRenamerFactory, Boolean> entry : myAutomaticRenamers.entrySet()) {
            boolean enabled = entry.getValue();
            entry.getKey().setEnabled(enabled);
            if (enabled) {
                processor.addRenamerFactory(entry.getKey());
            }
        }

        invokeRefactoring(processor);
    }

    @RequiredReadAction
    protected RenameProcessor createRenameProcessor(String newName) {
        return new RenameProcessor(
            getProject(),
            myPsiElement,
            newName,
            isSearchInComments(),
            isSearchInNonJavaFiles()
        );
    }

    @Override
    protected void canRun() throws ConfigurationException {
        if (Comparing.strEqual(getNewName(), myOldName)) {
            throw new ConfigurationException(LocalizeValue.empty());
        }
        if (!areButtonsValid()) {
            throw new ConfigurationException(LanguageLocalize.dialogMessageValidIdentifier(getNewName()));
        }
        Function<String, String> inputValidator = RenameInputValidatorRegistry.getInputErrorValidator(myPsiElement);
        if (inputValidator != null) {
            String errorText = inputValidator.apply(getNewName());
            if (errorText != null) {
                setErrorText(LocalizeValue.of(errorText));
            }
        }
    }

    @Override
    @RequiredReadAction
    protected boolean areButtonsValid() {
        return RenameUtil.isValidName(myProject, myPsiElement, getNewName());
    }

    protected @Nullable EditorBox getNameBox() {
        return myNameBox;
    }

    public @Nullable CheckBox getCbSearchInComments() {
        return myCbSearchInComments;
    }
}
