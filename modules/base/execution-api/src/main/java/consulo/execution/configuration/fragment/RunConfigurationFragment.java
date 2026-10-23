package consulo.execution.configuration.fragment;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.disposer.Disposable;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.RunConfiguration;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * One part of a run configuration editor. Shared and contributed fragments are extensions and decide through
 * {@link #isApplicable(RunConfiguration)} where they appear; a configuration type returns its own ones from
 * {@link RunConfiguration#createFragments()}.
 *
 * @param <T> what the fragment edits: the configuration, or {@link consulo.execution.RunnerAndConfigurationSettings} for a
 *            {@link RunnerAndConfigurationSettingsFragment}
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface RunConfigurationFragment<T> {
    String getId();

    /**
     * The entry of the fragment in "Modify options".
     */
    LocalizeValue getName();

    /**
     * The heading the fragment is listed under in "Modify options", empty for none.
     */
    default LocalizeValue getGroup() {
        return LocalizeValue.empty();
    }

    /**
     * Position of a shared or contributed fragment on the page, lower first; see {@link StandardRunConfigurationFragmentOrder}.
     * A configuration type's own fragments keep the order of {@link RunConfiguration#createFragments()} and come after the
     * shared fragments with a negative order ({@link StandardRunConfigurationFragmentOrder#BEFORE_TYPE}).
     */
    default int getOrder() {
        return StandardRunConfigurationFragmentOrder.LAST;
    }

    default RunConfigurationFragmentType getType() {
        return RunConfigurationFragmentType.EDITOR;
    }

    /**
     * The id of the {@link RunConfigurationFragmentType#GROUP} this fragment belongs to, {@code null} for the page itself. A
     * fragment whose group is not applicable is left out.
     */
    default @Nullable String getParentId() {
        return null;
    }

    /**
     * Whether the user can hide the fragment through "Modify options".
     */
    default boolean isRemovable() {
        return true;
    }

    default boolean isApplicable(RunConfiguration configuration) {
        return true;
    }

    default boolean isVisibleByDefault(T target) {
        return true;
    }

    /**
     * The state POJO this fragment keeps in the configuration, {@code null} when it edits values owned by someone else.
     */
    default @Nullable Class<?> getStateClass() {
        return null;
    }

    /**
     * The editor shown while the fragment is visible, {@code null} for a {@link RunConfigurationFragmentType#TAG}.
     */
    @RequiredUIAccess
    default @Nullable RunConfigurationFragmentEditor<T> createEditor(Project project, Disposable parent) {
        return null;
    }

    /**
     * Called on apply instead of the editor while the fragment is hidden.
     */
    default void applyHidden(T target) {
    }

    /**
     * Converts this fragment's part of an old configuration element into its state, added by
     * {@link LegacyRunConfigurationConverter#addFragment}. Called for applicable fragments after the configuration's own
     * {@code convertLegacyState}.
     */
    default void convertLegacyState(RunConfiguration configuration, LegacyRunConfigurationConverter converter) {
    }
}
