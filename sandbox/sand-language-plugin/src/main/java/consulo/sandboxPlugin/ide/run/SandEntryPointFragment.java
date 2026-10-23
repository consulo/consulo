package consulo.sandboxPlugin.ide.run;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

class SandEntryPointFragment implements RunConfigurationFragment<SandConfiguration> {
    @Override
    public String getId() {
        return "sandEntryPoint";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Entry point");
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<SandConfiguration> createEditor(Project project, Disposable parent) {
        EditorBoxBuilderFactory factory = project.getApplication().getInstance(EditorBoxBuilderFactory.class);
        EditorBox entryPointBox = factory.create(project)
            .completion(new SandEntryPointCompletionProvider())
            .placeholder(LocalizeValue.localizeTODO("main"))
            .build();
        EditorBox expressionBox = factory.create(project)
            .language(SandLanguage.INSTANCE)
            .placeholder(LocalizeValue.localizeTODO("Sand expression"))
            .build();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(LocalizeValue.localizeTODO("Entry point:"), entryPointBox);
                layout.addLabeled(LocalizeValue.localizeTODO("Expression:"), expressionBox);
            }

            @RequiredUIAccess
            @Override
            public void reset(SandConfiguration target) {
                entryPointBox.setValue(StringUtil.notNullize(target.getEntryPoint()));
                expressionBox.setValue(StringUtil.notNullize(target.getExpression()));
            }

            @RequiredUIAccess
            @Override
            public void apply(SandConfiguration target) {
                target.setEntryPoint(entryPointBox.getValue());
                target.setExpression(expressionBox.getValue());
            }
        };
    }
}
