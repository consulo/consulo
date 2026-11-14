/*
 * Copyright 2013-2026 consulo.io
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
package consulo.mcpServer.impl.internal.ai;

import consulo.ai.AIMcpServerProvider;
import consulo.application.Application;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.mcpServer.impl.internal.McpToolRegistry;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import consulo.mcpServer.skill.AgentSkillTarget;
import consulo.mcpServer.skill.BundledSkill;
import consulo.mcpServer.skill.BundledSkillsProvider;
import consulo.mcpServer.skill.SkillWorkspaceContext;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Puts the bundled skills an agent gets for a working directory into a plugin directory of the system directory -
 * {@code skills/<id>} beside the manifests of Claude and Cursor; Codex takes the {@code skills} directory as a root.
 * The project is never written to.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class McpBundledSkillDelivery {
    private static final Logger LOG = Logger.getInstance(McpBundledSkillDelivery.class);

    static final String SKILLS_DIRECTORY = "skills";

    private static final String MANIFEST = """
        {
          "name": "consulo",
          "description": "Skills of the Consulo IDE for its MCP tools",
          "version": "1.0.0"
        }
        """;

    private McpBundledSkillDelivery() {
    }

    /**
     * @param serverRunning whether the agent gets the MCP server of the IDE - a skill needing its tools comes only then
     */
    static @Nullable Path deliver(Application application,
                                  McpServerSettings settings,
                                  McpToolRegistry tools,
                                  boolean serverRunning,
                                  Path workingDirectory,
                                  String agent) {
        SkillWorkspaceContext context = SkillWorkspaceContext.create(workingDirectory, findProject(workingDirectory));
        AgentSkillTarget target = switch (agent) {
            case AIMcpServerProvider.AGENT_CLAUDE -> AgentSkillTarget.CLAUDE;
            case AIMcpServerProvider.AGENT_CODEX -> AgentSkillTarget.CODEX;
            default -> AgentSkillTarget.OTHER;
        };
        Path plugin = ContainerPathManager.get().getSystemDir()
            .resolve("ai-skills")
            .resolve(agent)
            .resolve(Integer.toHexString(context.getWorkingDirectory().toString().hashCode()));
        try {
            FileUtil.delete(plugin);
            boolean[] delivered = {false};
            application.getExtensionPoint(BundledSkillsProvider.class).forEach(provider -> {
                if (!provider.isProviderAvailable(context)) {
                    return;
                }
                for (BundledSkill skill : provider.registeredSkills()) {
                    if (!skill.getTargets().contains(target) || !provider.isSkillAvailable(context, skill)) {
                        continue;
                    }
                    boolean callable = skill.getRequiredTools().isEmpty() || serverRunning && skill.getRequiredTools().stream()
                        .allMatch(tool -> tools.findTool(tool) != null && settings.isToolEnabled(tool));
                    if (!callable) {
                        continue;
                    }
                    Path directory = plugin.resolve(SKILLS_DIRECTORY).resolve(skill.getSkillId());
                    try {
                        if (extract(provider.getClass().getClassLoader(), skill.getResourcePath(), directory)) {
                            provider.prepareSkillDirectory(skill, directory);
                            delivered[0] = true;
                        }
                        else {
                            LOG.error("The skill " + skill.getSkillId() + " has no SKILL.md at " + skill.getResourcePath());
                        }
                    }
                    catch (IOException | URISyntaxException e) {
                        LOG.error("Cannot deliver the skill " + skill.getSkillId(), e);
                    }
                }
            });
            if (!delivered[0]) {
                return null;
            }
            for (String manifest : new String[]{".claude-plugin", ".cursor-plugin"}) {
                Path file = plugin.resolve(manifest).resolve("plugin.json");
                Files.createDirectories(file.getParent());
                Files.writeString(file, MANIFEST, StandardCharsets.UTF_8);
            }
            return plugin;
        }
        catch (IOException | UncheckedIOException e) {
            LOG.error("Cannot deliver the bundled skills to " + plugin, e);
            return null;
        }
    }

    private static @Nullable Project findProject(Path workingDirectory) {
        Path normalized = workingDirectory.toAbsolutePath().normalize();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            String basePath = project.getBasePath();
            if (basePath != null && Path.of(basePath).toAbsolutePath().normalize().equals(normalized)) {
                return project;
            }
        }
        return null;
    }

    /**
     * Copies the skill directory from the class path - a jar of the plugin or a directory of classes.
     */
    private static boolean extract(ClassLoader loader, String resourcePath, Path target) throws IOException, URISyntaxException {
        URL marker = loader.getResource(resourcePath + "/SKILL.md");
        if (marker == null) {
            return false;
        }
        URI uri = marker.toURI();
        FileSystem opened = null;
        try {
            Path skillFile;
            try {
                skillFile = Path.of(uri);
            }
            catch (FileSystemNotFoundException e) {
                opened = FileSystems.newFileSystem(uri, Map.of());
                skillFile = opened.provider().getPath(uri);
            }
            Path source = skillFile.getParent();
            if (source == null) {
                return false;
            }
            try (Stream<Path> files = Files.walk(source)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    Path copy = target.resolve(source.relativize(file).toString());
                    if (Files.isDirectory(file)) {
                        Files.createDirectories(copy);
                    }
                    else {
                        // a walk visits a directory before its files - the parent is there
                        Files.copy(file, copy);
                    }
                }
            }
            return true;
        }
        finally {
            if (opened != null) {
                opened.close();
            }
        }
    }
}
