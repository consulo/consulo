/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.RefactoringUIUtil;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.ValueComponent;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.usage.UsageInfo;
import consulo.usage.UsagePreviewPanel;
import consulo.usage.UsagePreviewPanelFactory;
import consulo.usage.UsageViewPresentation;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author dsl
 */
public class AutomaticRenamingDialog extends DialogDescriptor {
    private static final int CHECK_COLUMN_WIDTH = 30;

    private final Project myProject;
    private final AutomaticRenamer myRenamer;
    private final List<RenameRow> myRows = new ArrayList<>();

    private @Nullable MutableFlatDataModel<RenameRow> myModel;
    private @Nullable Table<RenameRow> myTable;
    private @Nullable Toast myErrorToast;
    private @Nullable Label myUsageFileLabel;
    private @Nullable UsagePreviewPanel myUsagePreviewPanel;
    private boolean myValid = true;
    private boolean myErrorShown;

    public AutomaticRenamingDialog(Project project, AutomaticRenamer renamer) {
        super(LocalizeValue.of(renamer.getDialogTitle()));
        myProject = project;
        myRenamer = renamer;
        populateData();
    }

    private void populateData() {
        Map<PsiNamedElement, String> renames = myRenamer.getRenames();
        boolean selected = myRenamer.isSelectedByDefault();
        for (Map.Entry<PsiNamedElement, String> entry : renames.entrySet()) {
            if (entry.getValue() != null) {
                myRows.add(new RenameRow(entry.getKey(), entry.getValue(), selected));
            }
        }
        myRows.sort((r1, r2) -> Comparing.compare(r1.myElement.getName(), r2.myElement.getName()));
    }

    /**
     * @return whether the user chose to rename, the renamer holding the chosen renames
     */
    @RequiredUIAccess
    public CompletableFuture<Boolean> showAsync() {
        if (myProject.getApplication().isUnitTestMode()) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        return myProject.getApplication()
            .getInstance(DialogService.class)
            .build(myProject, this)
            .showAsync()
            .handle((value, error) -> error == null && value != null);
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofPixel(700, 640);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "#consulo.ide.impl.idea.refactoring.rename.AutomaticRenamingDialog";
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        MutableFlatDataModel<RenameRow> model = FlatDataModel.of(myRows);
        myModel = model;

        Table<RenameRow> table = Table.create(model);
        table.setAllowMultipleSelect(true);
        table.setSpeedSearchConverter(row -> row.myNewName);
        myTable = table;

        table.addColumn(LocalizeValue.empty(), row -> row.myShouldRename)
            .setWidth(CHECK_COLUMN_WIDTH)
            .setResizable(false)
            .setRenderer(ComponentItemRenderer.reusable(
                () -> CheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(Boolean.TRUE.equals(item.getValue()), false)
            ))
            .setEditor(new TableItemEditor<>() {
                @Override
                @RequiredUIAccess
                public ValueComponent<Boolean> createComponent(RenameRow row) {
                    return CheckBox.create(LocalizeValue.empty(), row.myShouldRename);
                }

                @Override
                @RequiredUIAccess
                public void commit(RenameRow row, @Nullable Boolean value) {
                    row.myShouldRename = Boolean.TRUE.equals(value);
                    model.update(row);
                    handleChanges(row);
                }
            });

        table.addColumn(RefactoringLocalize.automaticRenamerEnityNameColumn(myRenamer.entityName()), row -> row)
            .setRenderer((presentation, item) -> {
                RenameRow row = item.getValue();
                if (row == null) {
                    return;
                }
                for (int i = 0; i < row.myDescription.size(); i++) {
                    String run = row.myDescription.get(i);
                    if (!run.isEmpty()) {
                        presentation.append(run, i % 2 == 0 ? TextAttribute.REGULAR : TextAttribute.REGULAR_BOLD);
                    }
                }
            });

        table.addColumn(RefactoringLocalize.automaticRenamerRenameToColumn(), row -> row.myNewName)
            .setRenderer((presentation, item) -> {
                String newName = item.getValue();
                if (newName != null) {
                    presentation.append(newName);
                }
            })
            .setEditor(new TableItemEditor<>() {
                @Override
                @RequiredUIAccess
                public ValueComponent<String> createComponent(RenameRow row) {
                    return TextBox.create(row.myNewName);
                }

                @Override
                @RequiredUIAccess
                public void commit(RenameRow row, @Nullable String value) {
                    row.myNewName = value == null ? "" : value;
                    model.update(row);
                    handleChanges(row);
                }
            });

        table.addSelectListener(event -> selectionChanged(table.getSelectedItem()));

        Button selectAll = Button.create(RefactoringLocalize.selectAllButton(), event -> setAllSelected(true));
        Button unselectAll = Button.create(RefactoringLocalize.unselectAllButton(), event -> setAllSelected(false));
        HorizontalLayout buttons = HorizontalLayout.create();
        buttons.add(selectAll);
        buttons.add(unselectAll);

        DockLayout tablePanel = DockLayout.create(Space.SMALL);
        tablePanel.center(ScrollableLayout.create(table));
        tablePanel.bottom(DockLayout.create().right(buttons));

        Label usageFileLabel = Label.create(LocalizeValue.empty());
        myUsageFileLabel = usageFileLabel;

        Component content;
        if (myProject.getApplication().isUnifiedApplication()) {
            DockLayout layout = DockLayout.create(Space.SMALL);
            layout.center(tablePanel);
            layout.bottom(usageFileLabel);
            content = layout;
        }
        else {
            UsagePreviewPanel previewPanel = UsagePreviewPanelFactory.getInstance().createPreviewPanel(myProject, new UsageViewPresentation());
            Disposer.register(uiDisposable, previewPanel);
            previewPanel.updateLayout(null);
            myUsagePreviewPanel = previewPanel;

            DockLayout previewLayout = DockLayout.create();
            previewLayout.top(usageFileLabel);
            previewLayout.center(TargetAWT.wrap(previewPanel.createComponent()));

            TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
            splitLayout.setFirstComponent(tablePanel);
            splitLayout.setSecondComponent(previewLayout);
            splitLayout.setProportion(50);
            content = splitLayout;
        }

        Toast errorToast = Toast.create();
        errorToast.addStyle(ToastStyle.ERROR);
        errorToast.setVisible(false);
        myErrorToast = errorToast;

        DockLayout root = DockLayout.create(Space.SMALL);
        root.top(Label.create(LocalizeValue.of(myRenamer.getDialogDescription())));
        root.center(content);
        root.bottom(errorToast);

        if (!myRows.isEmpty()) {
            table.select(myRows.get(0));
        }
        return root;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedComponent() {
        return myTable;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myValid;
    }

    @Override
    @RequiredUIAccess
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (action instanceof DialogOkAction) {
            updateRenamer();
        }
        return true;
    }

    @RequiredUIAccess
    private void setAllSelected(boolean selected) {
        MutableFlatDataModel<RenameRow> model = myModel;
        for (RenameRow row : myRows) {
            row.myShouldRename = selected;
            if (model != null) {
                model.update(row);
            }
        }
    }

    @RequiredUIAccess
    private void handleChanges(RenameRow row) {
        boolean validName = RenameUtil.isValidName(myProject, row.myElement, row.myNewName);
        myValid = validName;
        setErrorText(validName ? LocalizeValue.empty() : LocalizeValue.localizeTODO("Identifier '" + row.myNewName + "' is invalid"));
        updateOkButtonState();
    }

    @RequiredUIAccess
    private void setErrorText(LocalizeValue errorText) {
        Toast errorToast = myErrorToast;
        if (errorToast == null) {
            return;
        }
        boolean shown = errorText.isNotEmpty();
        errorToast.setText(errorText);
        errorToast.setVisible(shown);
        if (shown != myErrorShown) {
            myErrorShown = shown;
            updateSize();
        }
    }

    @RequiredUIAccess
    private void selectionChanged(@Nullable RenameRow row) {
        Label usageFileLabel = myUsageFileLabel;
        UsagePreviewPanel previewPanel = myUsagePreviewPanel;
        if (row == null) {
            if (usageFileLabel != null) {
                usageFileLabel.setText(LocalizeValue.empty());
            }
            if (previewPanel != null) {
                previewPanel.updateLayout(null);
            }
            return;
        }

        if (usageFileLabel != null) {
            usageFileLabel.setText(LocalizeValue.of(row.myFileName));
        }
        if (previewPanel != null) {
            previewPanel.updateLayout(List.of(new UsageInfo(row.myElement)));
        }
    }

    private void updateRenamer() {
        for (RenameRow row : myRows) {
            if (row.myShouldRename) {
                myRenamer.setRename(row.myElement, row.myNewName);
            }
            else {
                myRenamer.doNotRename(row.myElement);
            }
        }
    }

    private static final class RenameRow {
        private final PsiNamedElement myElement;
        private final List<String> myDescription;
        private final String myFileName;
        private String myNewName;
        private boolean myShouldRename;

        private RenameRow(PsiNamedElement element, String newName, boolean shouldRename) {
            myElement = element;
            myDescription = splitBold(RefactoringUIUtil.getDescription(element, true).get());
            myFileName = fileNameOf(element);
            myNewName = newName;
            myShouldRename = shouldRename;
        }

        private static List<String> splitBold(String html) {
            List<String> runs = new ArrayList<>();
            String rest = html;
            boolean bold = false;
            while (true) {
                int tag = rest.indexOf(bold ? "</b>" : "<b>");
                if (tag < 0) {
                    runs.add(StringUtil.stripHtml(rest, false));
                    return runs;
                }
                runs.add(StringUtil.stripHtml(rest.substring(0, tag), false));
                rest = rest.substring(tag + (bold ? 4 : 3));
                bold = !bold;
            }
        }

        private static String fileNameOf(PsiNamedElement element) {
            PsiFile containingFile = element.getContainingFile();
            VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();
            return virtualFile == null ? "" : virtualFile.getName();
        }
    }
}
