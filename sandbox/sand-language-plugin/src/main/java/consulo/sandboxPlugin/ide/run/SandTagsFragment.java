package consulo.sandboxPlugin.ide.run;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletion;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

import java.util.List;

class SandTagsFragment implements RunConfigurationFragment<SandConfiguration> {
    @Override
    public String getId() {
        return "sandTags";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Tags");
    }

    @Override
    public LocalizeValue getGroup() {
        return LocalizeValue.localizeTODO("Sand");
    }

    @Override
    public boolean isVisibleByDefault(SandConfiguration target) {
        return target.getTags() != null;
    }

    @Override
    public void applyHidden(SandConfiguration target) {
        target.setTags(null);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<SandConfiguration> createEditor(Project project, Disposable parent) {
        EditorBox tagsBox = project.getApplication().getInstance(EditorBoxBuilderFactory.class).create(project)
            .completion(new TextFieldWithAutoCompletion.StringsCompletionProvider(List.of("fast", "slow", "flaky"), null))
            .build();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(LocalizeValue.localizeTODO("Tags:"), tagsBox);
            }

            @RequiredUIAccess
            @Override
            public void reset(SandConfiguration target) {
                tagsBox.setValue(StringUtil.notNullize(target.getTags()));
            }

            @RequiredUIAccess
            @Override
            public void apply(SandConfiguration target) {
                target.setTags(tagsBox.getValue());
            }
        };
    }
}
