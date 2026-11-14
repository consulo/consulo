// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.mcpServer.skill;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.util.registry.Registry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Bundles skill directories in the plugin resources of the provider.
 * <p>
 * The consumer resolves {@link BundledSkill#getResourcePath()} on the class loader of the provider class.
 *
 * <h3>Why this API is in the MCP Server module</h3>
 * A bundled skill calls MCP tools, and delivery checks the tools that the skill requires.
 * So the skill and its provider ship with the plugin that owns those tools.
 *
 * <h3>Threading</h3>
 * The consumer calls each hook off the UI thread, so blocking work is allowed.
 * A hook must not start an agent or an MCP server.
 * {@link #registeredSkills()} declares the skills and decides nothing.
 *
 * <h3>Availability</h3>
 * A skill reaches a working directory only when {@link #isProviderAvailable} and {@link #isSkillAvailable} both return {@code true}.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BundledSkillsProvider {
    /**
     * Declares the complete catalog of this provider. No I/O, no workspace state.
     */
    List<BundledSkill> registeredSkills();

    /**
     * Provider-level availability hook for one working directory.
     */
    default boolean isProviderAvailable(SkillWorkspaceContext context) {
        return true;
    }

    /**
     * Skill-level availability hook.
     * <p>
     * The default reads {@link BundledSkill#getEnabledRegistryKey()}. A skill without a key is available.
     */
    default boolean isSkillAvailable(SkillWorkspaceContext context, BundledSkill skill) {
        String key = skill.getEnabledRegistryKey();
        return key == null || Registry.is(key);
    }

    /**
     * Called after the consumer extracts the skill directory to {@code directory} and before delivery. It can add files to the
     * directory.
     */
    default void prepareSkillDirectory(BundledSkill skill, Path directory) throws IOException {
    }
}
