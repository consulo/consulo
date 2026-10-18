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
package consulo.versionControlSystem.impl.internal.commit;

import consulo.codeEditor.EditorEx;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.language.editor.ui.AdditionalPageAtBottomEditorCustomization;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.RightMarginEditorCustomization;
import consulo.language.editor.ui.SoftWrapsEditorCustomization;
import consulo.language.editor.ui.WrapWhenTypingReachesRightMarginCustomization;
import consulo.language.spellchecker.editor.SpellcheckingEditorCustomizationProvider;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.CommitMessageComponent;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CommitMessageImpl implements CommitMessageComponent {
    public static final Key<DataContext> DATA_CONTEXT_KEY = Key.create("commit message data context");

    private final EditorBox myEditorBox;
    private final Component myComponent;
    private boolean myCheckSpelling;

    @RequiredUIAccess
    public CommitMessageImpl(Project project) {
        this(project, true);
    }

    @RequiredUIAccess
    public CommitMessageImpl(Project project, boolean withSeparator) {
        myEditorBox = createCommitTextEditor(project, false);

        DockLayout content = DockLayout.create();
        content.center(myEditorBox);

        ActionGroup toolbarActions = getToolbarActions();
        if (toolbarActions != null) {
            ActionToolbar toolbar = ActionToolbarFactory.getInstance()
                .createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, ActionToolbar.Style.VERTICAL);
            toolbar.setTargetUIComponent(content);
            content.right(toolbar.getUIComponent());
        }

        myComponent = withSeparator ? LabeledLayout.create(VcsLocalize.labelCommitComment(), content) : content;
        myComponent.putUserData(UiDataProvider.KEY, sink -> sink.set(VcsDataKeys.COMMIT_MESSAGE_CONTROL, this));
        myEditorBox.getDocument().putUserData(DATA_CONTEXT_KEY, DataManager.getInstance().getDataContext(myComponent));
    }

    @Override
    public Component getComponent() {
        return myComponent;
    }

    @Override
    public String getCommitMessage() {
        String text = myEditorBox.getDocument().getCharsSequence().toString();
        int end = text.length();
        while (end > 0 && Character.isSpaceChar(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    @Override
    @RequiredUIAccess
    public void setCommitMessage(@Nullable String currentDescription) {
        myEditorBox.setValue(currentDescription == null ? "" : currentDescription);
    }

    /**
     * Creates a text editor appropriate for creating commit messages.
     *
     * @param project           project this commit message editor is intended for
     * @param forceSpellCheckOn if false, {@link VcsConfiguration#CHECK_COMMIT_MESSAGE_SPELLING} will control
     *                          whether or not the editor has spell check enabled
     * @return a commit message editor
     */
    @RequiredUIAccess
    public static EditorBox createCommitTextEditor(Project project, boolean forceSpellCheckOn) {
        List<Consumer<EditorEx>> features = new ArrayList<>();

        VcsConfiguration configuration = VcsConfiguration.getInstance(project);
        boolean enableSpellChecking = forceSpellCheckOn || configuration.CHECK_COMMIT_MESSAGE_SPELLING;

        SpellcheckingEditorCustomizationProvider.getInstance().getCustomizationOpt(enableSpellChecking).ifPresent(features::add);
        features.add(new RightMarginEditorCustomization(configuration.USE_COMMIT_MESSAGE_MARGIN, configuration.COMMIT_MESSAGE_MARGIN_SIZE));
        features.add(WrapWhenTypingReachesRightMarginCustomization.getInstance(configuration.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN));
        features.add(SoftWrapsEditorCustomization.ENABLED);
        features.add(AdditionalPageAtBottomEditorCustomization.DISABLED);

        return project.getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(project)
            .multiline()
            .customize(editor -> features.forEach(feature -> feature.accept(editor)))
            .build();
    }

    public static @Nullable ActionGroup getToolbarActions() {
        return ActionManager.getInstance().getAction("Vcs.MessageActionGroup") instanceof ActionGroup group ? group : null;
    }

    public EditorBox getEditorBox() {
        return myEditorBox;
    }

    @Override
    public boolean isCheckSpelling() {
        return myCheckSpelling;
    }

    @Override
    public void setCheckSpelling(boolean check) {
        myCheckSpelling = check;
        if (!(myEditorBox.getEditor() instanceof EditorEx editorEx)) {
            return;
        }

        SpellcheckingEditorCustomizationProvider.getInstance()
            .getCustomizationOpt(check)
            .ifPresent(customizer -> customizer.accept(editorEx));
    }
}
