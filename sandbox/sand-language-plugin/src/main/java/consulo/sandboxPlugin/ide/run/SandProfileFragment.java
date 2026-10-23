package consulo.sandboxPlugin.ide.run;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.EditorComboBox;
import consulo.language.editor.ui.textCompletion.DefaultTextCompletionValueDescriptor;
import consulo.language.editor.ui.textCompletion.ValuesCompletionProvider;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ImageBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

import java.util.List;

class SandProfileFragment implements RunConfigurationFragment<SandConfiguration> {
    private static final List<String> PROFILES = List.of("debug", "release", "profile");

    @Override
    public String getId() {
        return "sandProfile";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Profile");
    }

    @Override
    public LocalizeValue getGroup() {
        return LocalizeValue.localizeTODO("Sand");
    }

    @Override
    public boolean isVisibleByDefault(SandConfiguration target) {
        return target.getProfile() != null;
    }

    @Override
    public void applyHidden(SandConfiguration target) {
        target.setProfile(null);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<SandConfiguration> createEditor(Project project, Disposable parent) {
        EditorComboBox profileBox = project.getApplication().getInstance(EditorBoxBuilderFactory.class).create(project)
            .completion(new ValuesCompletionProvider<>(new DefaultTextCompletionValueDescriptor.StringValueDescriptor(), PROFILES))
            .buildComboBox();
        profileBox.setItems(PROFILES);
        profileBox.setSuffixComponent(ImageBox.create(PlatformIconGroup.generalInformation()));

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(LocalizeValue.localizeTODO("Profile:"), profileBox);
            }

            @RequiredUIAccess
            @Override
            public void reset(SandConfiguration target) {
                profileBox.setValue(StringUtil.notNullize(target.getProfile()));
            }

            @RequiredUIAccess
            @Override
            public void apply(SandConfiguration target) {
                target.setProfile(profileBox.getValue());
            }
        };
    }
}
