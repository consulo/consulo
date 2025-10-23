// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.component.util.ModificationTracker;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * Allows to modify current project settings for a specific PSI file. There can be several modifiers applying changes to the same instance
 * of {@code TransientCodeStyleSettings} object. The modifier can be registered via "com.intellij.codeStyleSettingsModifier" extension
 * point.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CodeStyleSettingsModifier {
    ExtensionPointName<CodeStyleSettingsModifier> EP_NAME = ExtensionPointName.create(CodeStyleSettingsModifier.class);

    /**
     * Modifies given settings. The modifier may add dependencies to the transient code style settings to update them if the dependencies
     * change, see {@link TransientCodeStyleSettings#addDependency(ModificationTracker)} method.
     *
     * @param settings The settings to modify, may contain changes made by other code style settings modifiers.
     * @param file     The PSI file for which a modification is to be made.
     * @return True if the modifier has made any changes, false otherwise.
     */
    boolean modifySettings(@Nonnull TransientCodeStyleSettings settings, @Nonnull PsiFile file);

    /**
     * Checks if the modifier may potentially override project code style settings. This may include enabled/disabled flag in settings,
     * a presence of certain files and etc. The method is called on a pooled thread and thus may not return immediately.
     *
     * @param project The project to check the overriding status for.
     * @return True if the modifier may override project setting, false otherwise (default).
     */
    default boolean mayOverrideSettingsOf(@Nonnull Project project) {
        return false;
    }

    /**
     * @return The name of the modifier to be shown in UI.
     */
    String getName();

    /**
     * A factory method which returns status bar UI contributor for the given settings given that the settings have been modified by this
     * specific modifier.
     *
     * @param transientSettings The settings to construct UI contributor for.
     * @return The UI contributor or null if code style changes should not be indicated in the status bar.
     * @see CodeStyleStatusBarUIContributor
     */
    @Nullable
    CodeStyleStatusBarUIContributor getStatusBarUiContributor(@Nonnull TransientCodeStyleSettings transientSettings);

    /**
     * @return The function which disables the modifier in the given code style settings or by other means. It is purely programmatic and
     * doesn't perform any UI operations by itself. {@code null} means that programmatic disabling is not available.
     */
    @Nullable
    default Consumer<CodeStyleSettings> getDisablingFunction() {
        return null;
    }
}