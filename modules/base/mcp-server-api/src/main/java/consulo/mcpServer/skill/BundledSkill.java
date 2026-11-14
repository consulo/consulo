// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.mcpServer.skill;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * One skill that a {@link BundledSkillsProvider} bundles in its plugin resources.
 */
public final class BundledSkill {
    /**
     * The resource root of the bundled skills in a provider module.
     */
    public static final String DEFAULT_RESOURCE_ROOT = "bundledSkills";

    private final String mySkillId;
    private final Set<String> myTools;
    private final Set<AgentSkillTarget> myTargets;
    private final String myResourcePath;
    private final @Nullable String myEnabledRegistryKey;
    private final Set<String> myRequiredTools;

    /**
     * @param skillId            canonical skill id (folder id under bundled skills root)
     * @param tools              IDE MCP tool names that this skill uses. CLI tools and external MCP tools do not belong here.
     * @param targets            agents this skill is addressed to
     * @param resourcePath       the skill directory on the class path of the provider. The default is the module's own
     *                           {@code bundledSkills/<skillId>}. A provider that packages a skill elsewhere, such as at the root
     *                           of a resource-only module, passes that path instead. Its last segment is always {@code skillId}:
     *                           the installed directory takes its name from the path, and the gates and the tool allowlist look
     *                           the skill up by its id.
     * @param enabledRegistryKey the registry key that turns the skill on, or {@code null} when the skill has no gate of its own.
     *                           The default {@link BundledSkillsProvider#isSkillAvailable} reads it. The module that registers the
     *                           skill declares the key.
     * @param requiredTools      the subset of {@code tools} that an agent must be able to call. Without them, the agent does not
     *                           get the skill.
     */
    public BundledSkill(String skillId,
                        Set<String> tools,
                        Set<AgentSkillTarget> targets,
                        String resourcePath,
                        @Nullable String enabledRegistryKey,
                        Set<String> requiredTools) {
        if (skillId.isBlank()) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        if (!resourcePath.substring(resourcePath.lastIndexOf('/') + 1).equals(skillId)) {
            throw new IllegalArgumentException("resourcePath '" + resourcePath + "' must end with the skill id '" + skillId + "'");
        }
        if (!tools.containsAll(requiredTools)) {
            Set<String> missing = new HashSet<>(requiredTools);
            missing.removeAll(tools);
            throw new IllegalArgumentException("Skill '" + skillId + "' requires tools that it does not declare: " + missing);
        }
        mySkillId = skillId;
        myTools = Set.copyOf(tools);
        myTargets = Set.copyOf(targets);
        myResourcePath = resourcePath;
        myEnabledRegistryKey = enabledRegistryKey;
        myRequiredTools = Set.copyOf(requiredTools);
    }

    public BundledSkill(String skillId, Set<String> tools, Set<String> requiredTools) {
        this(skillId, tools, AgentSkillTarget.ALL, DEFAULT_RESOURCE_ROOT + "/" + skillId, null, requiredTools);
    }

    public BundledSkill(String skillId) {
        this(skillId, Set.of(), Set.of());
    }

    public String getSkillId() {
        return mySkillId;
    }

    public Set<String> getTools() {
        return myTools;
    }

    public Set<AgentSkillTarget> getTargets() {
        return myTargets;
    }

    public String getResourcePath() {
        return myResourcePath;
    }

    public @Nullable String getEnabledRegistryKey() {
        return myEnabledRegistryKey;
    }

    public Set<String> getRequiredTools() {
        return myRequiredTools;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BundledSkill skill
            && mySkillId.equals(skill.mySkillId)
            && myTools.equals(skill.myTools)
            && myTargets.equals(skill.myTargets)
            && myResourcePath.equals(skill.myResourcePath)
            && Objects.equals(myEnabledRegistryKey, skill.myEnabledRegistryKey)
            && myRequiredTools.equals(skill.myRequiredTools);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mySkillId, myTools, myTargets, myResourcePath, myEnabledRegistryKey, myRequiredTools);
    }

    @Override
    public String toString() {
        return "BundledSkill(skillId=" + mySkillId + ", tools=" + myTools + ", targets=" + myTargets + ", resourcePath=" + myResourcePath
            + ", enabledRegistryKey=" + myEnabledRegistryKey + ", requiredTools=" + myRequiredTools + ")";
    }
}
