// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingContext;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItem;
import consulo.dataContext.DataContext;
import consulo.component.extension.ExtensionPointName;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import consulo.application.util.matcher.Matcher;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Introduction
 * <p>
 * This class provides ability to run an arbitrary activity for and may provide help section to the matched input text.
 * If you want to provide your own provider usually it's better to extend {@link RunAnythingProviderBase} - default implementation base class.
 * <p>
 * <p>
 * Matching and execution
 * <p>
 * {@link RunAnythingProvider} operates with {@code V} that represents a value to be executed.
 * E.g. {@code V} can be a run configuration, an action or a string command to be executed in console.
 * <p>
 * See {@link RunAnythingRunConfigurationProvider}, {@link RunAnythingCommandProvider} and others inheritors.
 * <p>
 * <p>
 * Help
 * <p>
 * "Run Anything" popup provides ability to show commands help. If "?" is inserted a list of commands that can be matched will be shown.
 * <p>
 * This help list is divided into several help groups, that usually are associated with a language or plugin, e.g. 'ruby'/'java'.
 * E.g. 'ruby' help group consists of "ruby \<script.rb\>", "rvm use \<sdk_version\>" etc. command placeholders.
 * <p>
 * Each help group {@link consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingHelpGroup} joins providers related to this group.
 * <p>
 * To add a provider command help placeholder in a group do the following:
 * <ul>
 * <li>register your own help group {@link consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingHelpGroup}</li>
 * <li>implement {@link #getHelpItem(DataContext)}. See also getHelp*() methods of {@link RunAnythingProviderBase}</li>
 * <li>add provider to {@link consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingHelpGroup#getProviders()}</li>
 * </ul>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface RunAnythingProvider<V> {
    ExtensionPointName<RunAnythingProvider> EP_NAME = ExtensionPointName.create(RunAnythingProvider.class);
    /**
     * Use it to retrieve command executing context, e.g. project base directory, module or custom working directory
     * that'd been chosen by the "Choose context" dropdown
     */
    Key<RunAnythingContext> EXECUTING_CONTEXT = Key.create("EXECUTING_CONTEXT");

    /**
     * Finds matching value by input {@code pattern}.
     * <p>
     * E.g. if input "open #projectName" an action {@link AnAction} "Open recent project #projectName" is returned,
     * for "ruby test.rb" an existing run configuration with the name "ruby test.rb" is find.
     *
     * @param dataContext use it to fetch project, module, working directory
     * @param pattern     input string
     */
    @Nullable
    V findMatchingValue(@Nonnull DataContext dataContext, @Nonnull String pattern);

    /**
     * Gets completions variants for input command prefix. E.g. "rvm use" provider should return list of sdk versions.
     *
     * @param dataContext use it to fetch project, module, working directory
     * @param pattern     input string, use it to provide specific variants for the input command if needed, e.g. for command arguments completion
     */
    @Nonnull
    Collection<V> getValues(@Nonnull DataContext dataContext, @Nonnull String pattern);

    /**
     * Execute actual matched {@link #findMatchingValue(DataContext, String)} value.
     *
     * @param dataContext use it to fetch project, module, working directory
     * @param value       matched value
     */
    void execute(@Nonnull DataContext dataContext, @Nonnull V value);

    /**
     * A value specific icon is painted it in the search field and used by value presentation wrapper.
     * E.g. for a configuration value it gets configuration type icon.
     *
     * @param value matching value
     */
    @Nullable
    Image getIcon(@Nonnull V value);

    /**
     * If select a value in the list this command will be inserted into the search field.
     *
     * @param value matching value
     */
    @Nonnull
    String getCommand(@Nonnull V value);

    /**
     * Returns text that is painted on the popup bottom and changed according to the list selection.
     */
    @Nullable
    String getAdText();

    /**
     * Returns value's presentation wrapper that is actually added into the main list.
     * See also {@link #getHelpItem(DataContext)}
     *
     * @param dataContext use it to fetch project, module, working directory
     * @param value       matching value
     */
    @Nonnull
    RunAnythingItem getMainListItem(@Nonnull DataContext dataContext, @Nonnull V value);

    /**
     * Returns help group title this provider belongs to
     */
    @Nullable
    default String getHelpGroupTitle() {
        return null;
    }

    /**
     * Returns value's presentation wrapper that is actually added into the help list.
     * See also {@link #getMainListItem(DataContext, Object)}
     *
     * @param dataContext use it to fetch project, module, working directory
     */

    @Nullable
    RunAnythingItem getHelpItem(@Nonnull DataContext dataContext);

    /**
     * Returns completion group title. {@code null} means that current provider doesn't provide completion.
     */
    @Nonnull
    LocalizeValue getCompletionGroupTitle();

    /**
     * Returns group matcher for filtering group elements. Remain {@code null} to use default matcher
     *
     * @param dataContext use it to fetch project, module, working directory
     * @param pattern     to build matcher
     */
    @Nullable
    Matcher getMatcher(@Nonnull DataContext dataContext, @Nonnull String pattern);

    /**
     * Provides context types that can be chosen as execution contexts:
     * - project, {@link RunAnythingContext.ProjectContext}
     * - module, {@link RunAnythingContext.ModuleContext}
     * - working directory, {@link RunAnythingContext.RecentDirectoryContext}
     * <p>
     * The first context will be chosen as default context.
     */
    @Nonnull
    List<RunAnythingContext> getExecutionContexts(@Nonnull DataContext dataContext);

    /**
     * Finds provider that matches {@code pattern}
     *
     * @param dataContext use it to fetch project, module, working directory
     * @param pattern     input string
     */
    @Nullable
    static RunAnythingProvider findMatchedProvider(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        return Arrays.stream(EP_NAME.getExtensions())
            .filter(provider -> provider.findMatchingValue(dataContext, pattern) != null)
            .findFirst()
            .orElse(null);
    }
}