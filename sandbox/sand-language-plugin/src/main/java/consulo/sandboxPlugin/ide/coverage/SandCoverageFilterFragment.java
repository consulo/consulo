package consulo.sandboxPlugin.ide.coverage;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentGroup;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.run.SandConfiguration;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

/**
 * Coverage include pattern of Sand configurations, a fragment of the platform "Code Coverage" group.
 *
 * @author VISTALL
 * @since 2026-10-23
 */
@ExtensionImpl
public class SandCoverageFilterFragment implements RunConfigurationFragment<RunConfiguration> {
    @Override
    public String getId() {
        return "coverage.sand.include";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Specify Sand entry points");
    }

    @Override
    public @Nullable String getParentId() {
        return StandardRunConfigurationFragmentGroup.COVERAGE;
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return configuration instanceof SandConfiguration;
    }

    @Override
    public boolean isVisibleByDefault(RunConfiguration target) {
        return getState(target).getIncludePattern() != null;
    }

    @Override
    public Class<SandCoverageFilterState> getStateClass() {
        return SandCoverageFilterState.class;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<RunConfiguration> createEditor(Project project, Disposable parent) {
        TextBox pattern = TextBox.create();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(LocalizeValue.localizeTODO("Include entry points:"), pattern);
            }

            @RequiredUIAccess
            @Override
            public void reset(RunConfiguration target) {
                pattern.setValue(StringUtil.notNullize(getState(target).getIncludePattern()));
            }

            @RequiredUIAccess
            @Override
            public void apply(RunConfiguration target) {
                getState(target).setIncludePattern(pattern.getValue());
            }
        };
    }

    @Override
    public void applyHidden(RunConfiguration target) {
        getState(target).setIncludePattern(null);
    }

    @Override
    public void convertLegacyState(RunConfiguration configuration, LegacyRunConfigurationConverter converter) {
        Element extension = converter.findExtension("sandCoverage");
        if (extension != null) {
            SandCoverageFilterState state = new SandCoverageFilterState();
            state.setIncludePattern(extension.getAttributeValue("include"));
            converter.addFragment(state);
        }
    }

    private static SandCoverageFilterState getState(RunConfiguration configuration) {
        return ((SandConfiguration) configuration).getFragmentState(SandCoverageFilterState.class);
    }
}
