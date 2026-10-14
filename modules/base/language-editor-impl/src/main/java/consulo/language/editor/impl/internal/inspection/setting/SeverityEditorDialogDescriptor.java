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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.ui.ColorAndFontDescriptionPanel;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.Settings;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl.SeverityBasedTextAttributes;
import consulo.language.editor.impl.internal.rawHighlight.SeverityUtil;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.rawHighlight.HighlightInfoTypeImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.ListBox;
import consulo.ui.Space;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.StandardColors;
import consulo.ui.ex.dialog.DialogDescriptor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class SeverityEditorDialogDescriptor extends DialogDescriptor {
    private static final String GENERAL_COLORS_PAGE_ID = "reference.settingsdialog.IDE.editor.colors.General";

    private final SeverityRegistrarImpl myRegistrar;
    private final MutableFlatDataModel<SeverityBasedTextAttributes> myModel;
    private final @Nullable HighlightSeverity myPreselected;

    private @Nullable ListBox<SeverityBasedTextAttributes> myList;
    private @Nullable ColorAndFontDescriptionPanel myAttributesPanel;
    private @Nullable DockLayout myDetails;
    private @Nullable Component myDefaultMessage;
    private @Nullable SeverityBasedTextAttributes myCurrent;

    public SeverityEditorDialogDescriptor(SeverityRegistrarImpl registrar, @Nullable HighlightSeverity preselected) {
        super(InspectionLocalize.severitiesEditorDialogTitle());
        myRegistrar = registrar;
        myPreselected = preselected;

        List<SeverityBasedTextAttributes> types = new ArrayList<>(SeverityUtil.getRegisteredHighlightingInfoTypes(registrar));
        types.sort((first, second) -> -registrar.compare(first.getSeverity(), second.getSeverity()));
        myModel = FlatDataModel.of(types);
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(50, 22);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        ListBox<SeverityBasedTextAttributes> list = ListBox.create(myModel);
        list.setRenderer((presentation, item) -> {
            SeverityBasedTextAttributes value = item.getValue();
            if (value != null) {
                presentation.append(LocalizeValue.of(value.getSeverity().getName()));
            }
        });
        list.addValueListener(event -> onSelected(event.getValue()));
        myList = list;

        Component decorated = ToolbarDecoratorBuilderFactory.getInstance()
            .create(list)
            .addOrReplaceAction(new AddSeverityAction())
            .addOrReplaceAction(new RemoveSeverityAction())
            .addOrReplaceAction(new UpSeverityAction())
            .addOrReplaceAction(new DownSeverityAction())
            .disableAction(EditAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP)
            .build();

        myAttributesPanel = new ColorAndFontDescriptionPanel();

        Button editColors = Button.create(InspectionLocalize.severitiesDefaultSettingsMessage(), event -> editColorsAndFonts());
        myDefaultMessage = DockLayout.create(Space.NONE).left(editColors);

        DockLayout details = DockLayout.create(Space.NONE);
        details.paddingBuilder().leftSet(Space.MEDIUM).apply();
        myDetails = details;

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(decorated);
        split.setSecondComponent(details);
        split.setProportion(45);

        SeverityBasedTextAttributes preselection = null;
        for (int i = 0; i < myModel.getSize(); i++) {
            SeverityBasedTextAttributes type = myModel.get(i);
            if (type.getSeverity().equals(myPreselected)) {
                preselection = type;
            }
        }
        if (preselection == null && myModel.getSize() > 0) {
            preselection = myModel.get(0);
        }
        if (preselection != null) {
            list.setValue(preselection);
        }
        return split;
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myList;
    }

    @Override
    public DialogValue getOkValue() {
        SeverityBasedTextAttributes current = myList == null ? null : myList.getValue();
        return new SeverityEditorResult(current == null ? null : current.getSeverity());
    }

    @Override
    public void onHandleValue(AnAction action, @Nullable DialogValue value) {
        if (action instanceof DialogOkAction) {
            commit();
        }
    }

    @RequiredUIAccess
    private void onSelected(@Nullable SeverityBasedTextAttributes selected) {
        SeverityBasedTextAttributes previous = myCurrent;
        if (previous != null) {
            applyAttributes(previous);
        }
        myCurrent = selected;

        DockLayout details = myDetails;
        ColorAndFontDescriptionPanel attributesPanel = myAttributesPanel;
        Component defaultMessage = myDefaultMessage;
        if (details == null || attributesPanel == null || defaultMessage == null) {
            return;
        }

        details.removeAll();
        if (selected == null) {
            return;
        }

        if (myRegistrar.isDefaultSeverity(selected.getSeverity())) {
            details.top(defaultMessage);
        }
        else {
            attributesPanel.reset(describe(selected, selected.getAttributes().clone()));
            details.top(attributesPanel.getUIComponent());
        }
    }

    private static SeverityTextAttributesDescription describe(SeverityBasedTextAttributes info, TextAttributes attributes) {
        return new SeverityTextAttributesDescription(
            LocalizeValue.of(info.getType().toString()),
            attributes,
            info.getType().getAttributesKey()
        );
    }

    @RequiredUIAccess
    private void applyAttributes(SeverityBasedTextAttributes info) {
        ColorAndFontDescriptionPanel attributesPanel = myAttributesPanel;
        if (attributesPanel == null || myRegistrar.isDefaultSeverity(info.getSeverity())) {
            return;
        }

        SeverityTextAttributesDescription description = describe(info, new TextAttributes());
        attributesPanel.apply(description, null);
        info.getAttributes().copyFrom(description.getTextAttributes());
    }

    private void commit() {
        SeverityBasedTextAttributes current = myCurrent;
        if (current != null) {
            applyAttributes(current);
        }

        Collection<SeverityBasedTextAttributes> removed = new HashSet<>(SeverityUtil.getRegisteredHighlightingInfoTypes(myRegistrar));
        List<HighlightSeverity> order = new ArrayList<>();
        for (int i = myModel.getSize() - 1; i >= 0; i--) {
            SeverityBasedTextAttributes info = myModel.get(i);
            order.add(info.getSeverity());
            if (!myRegistrar.isDefaultSeverity(info.getSeverity())) {
                removed.remove(info);
                ColorValue stripeColor = info.getAttributes().getErrorStripeColor();
                myRegistrar.registerSeverity(info, stripeColor != null ? stripeColor : StandardColors.YELLOW);
            }
        }
        for (SeverityBasedTextAttributes info : removed) {
            myRegistrar.unregisterSeverity(info.getSeverity());
        }
        myRegistrar.setOrder(order);
    }

    @RequiredUIAccess
    private void editColorsAndFonts() {
        ListBox<SeverityBasedTextAttributes> list = myList;
        SeverityBasedTextAttributes current = list == null ? null : list.getValue();
        Settings settings = list == null ? null : DataManager.getInstance().getDataContext(list).getData(Settings.KEY);
        if (settings == null || current == null) {
            return;
        }

        String severityName = current.getSeverity().getName();
        settings.select(GENERAL_COLORS_PAGE_ID).whenComplete((configurable, error) -> {
            if (configurable instanceof SearchableConfigurable searchable) {
                Runnable search = searchable.enableSearch(severityName);
                if (search != null) {
                    search.run();
                }
            }
        });
    }

    private boolean isMovable(@Nullable SeverityBasedTextAttributes value, int offset) {
        if (value == null) {
            return false;
        }

        int index = myModel.indexOf(value);
        int target = index + offset;
        if (target < 0 || target >= myModel.getSize()) {
            return false;
        }
        return !(myRegistrar.isDefaultSeverity(value.getSeverity()) && myRegistrar.isDefaultSeverity(myModel.get(target).getSeverity()));
    }

    @RequiredUIAccess
    private void move(SeverityBasedTextAttributes value, int offset) {
        if (!isMovable(value, offset)) {
            return;
        }

        SeverityBasedTextAttributes current = myCurrent;
        if (current != null) {
            applyAttributes(current);
        }

        int target = myModel.indexOf(value) + offset;
        myModel.remove(value);
        myModel.add(value, target);

        ListBox<SeverityBasedTextAttributes> list = myList;
        if (list != null) {
            list.setValue(value);
        }
    }

    private class AddSeverityAction extends AddAction<SeverityBasedTextAttributes> {
        @RequiredUIAccess
        @Override
        protected void doAdd(AnActionEvent e) {
            InputBoxBuilder.text()
                .title(InspectionLocalize.highlightSeverityCreateDialogTitle())
                .text(InspectionLocalize.highlightSeverityCreateDialogNameLabel())
                .asQuestion()
                .validator(name -> isUniqueName(name) ? null : InputProblem.error(InspectionLocalize.highlightSeverityCreateDialogNameLabel()))
                .showAsync(myList)
                .whenComplete((name, error) -> {
                    if (name != null && !name.isBlank() && isUniqueName(name)) {
                        addSeverity(name);
                    }
                });
        }
    }

    private boolean isUniqueName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        for (int i = 0; i < myModel.getSize(); i++) {
            if (Objects.equals(myModel.get(i).getSeverity().getName(), name)) {
                return false;
            }
        }
        return true;
    }

    @RequiredUIAccess
    private void addSeverity(String name) {
        TextAttributes attributes = Objects.requireNonNull(CodeInsightColors.WARNINGS_ATTRIBUTES.getDefaultAttributes());
        HighlightInfoTypeImpl info = new HighlightInfoTypeImpl(new HighlightSeverity(name, 50), TextAttributesKey.createTextAttributesKey(name));
        SeverityBasedTextAttributes created = new SeverityBasedTextAttributes(attributes.clone(), info);
        myModel.add(created);

        ListBox<SeverityBasedTextAttributes> list = myList;
        if (list != null) {
            list.setValue(created);
        }
    }

    private class RemoveSeverityAction extends RemoveAction<SeverityBasedTextAttributes> {
        @RequiredUIAccess
        @Override
        public void updateAtUI(AnActionEvent e) {
            super.updateAtUI(e);
            SeverityBasedTextAttributes value = getSelectedValue(e);
            if (value != null && myRegistrar.isDefaultSeverity(value.getSeverity())) {
                e.getPresentation().setEnabled(false);
            }
        }

        @RequiredUIAccess
        @Override
        protected void doRemove(SeverityBasedTextAttributes value, AnActionEvent e) {
            if (myRegistrar.isDefaultSeverity(value.getSeverity())) {
                return;
            }

            if (myCurrent == value) {
                myCurrent = null;
            }
            myModel.remove(value);
        }
    }

    private class UpSeverityAction extends UpMoveAction<SeverityBasedTextAttributes> {
        @RequiredUIAccess
        @Override
        public void updateAtUI(AnActionEvent e) {
            super.updateAtUI(e);
            if (!isMovable(getSelectedValue(e), -1)) {
                e.getPresentation().setEnabled(false);
            }
        }

        @RequiredUIAccess
        @Override
        protected void doUp(SeverityBasedTextAttributes value, AnActionEvent e) {
            move(value, -1);
        }
    }

    private class DownSeverityAction extends DownMoveAction<SeverityBasedTextAttributes> {
        @RequiredUIAccess
        @Override
        public void updateAtUI(AnActionEvent e) {
            super.updateAtUI(e);
            if (!isMovable(getSelectedValue(e), 1)) {
                e.getPresentation().setEnabled(false);
            }
        }

        @RequiredUIAccess
        @Override
        protected void doDown(SeverityBasedTextAttributes value, AnActionEvent e) {
            move(value, 1);
        }
    }
}
