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
package consulo.ai.impl.internal.instruction;

import consulo.ai.AIAgentInstructionProvider;
import consulo.ai.AIAgentRuleDirectory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Finds the instructions the agents read, and creates and removes them. Reads the disk - never on the ui thread.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public final class AIAgentInstructionScanner {
    private static final Logger LOG = Logger.getInstance(AIAgentInstructionScanner.class);

    public static final String SKILL_FILE = "SKILL.md";
    private static final int SKILL_DEPTH = 3;

    private AIAgentInstructionScanner() {
    }

    public static List<AIAgentInstruction> scan(Path projectDirectory, @Nullable Path homeDirectory, List<AIAgentInstructionProvider> providers) {
        List<AIAgentInstruction> result = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        for (AIAgentInstructionProvider provider : providers) {
            for (String name : provider.getInstructionFiles()) {
                Path file = projectDirectory.resolve(name);
                if (Files.isRegularFile(file) && seen.add(real(file))) {
                    result.add(new AIAgentInstruction(AIAgentInstructionKind.FILE, name, "", provider.getName(), file, file, false));
                }
            }
            for (AIAgentRuleDirectory directory : provider.getRuleDirectories()) {
                for (Path file : list(projectDirectory.resolve(directory.path()))) {
                    String name = file.getFileName().toString();
                    if (Files.isRegularFile(file) && hasExtension(name, directory.extensions()) && seen.add(real(file))) {
                        result.add(new AIAgentInstruction(AIAgentInstructionKind.RULE, name, "", provider.getName(), file, file, false));
                    }
                }
            }
            for (String directory : provider.getSkillDirectories()) {
                skills(projectDirectory.resolve(directory), provider.getName(), false, seen, result);
                if (homeDirectory != null) {
                    skills(homeDirectory.resolve(directory), provider.getName(), true, seen, result);
                }
            }
        }
        result.sort(Comparator.comparing(AIAgentInstruction::global).thenComparing(instruction -> instruction.name().toLowerCase(Locale.ROOT)));
        return result;
    }

    /**
     * The instruction files of the agents which the project does not have yet.
     */
    public static List<String> missingInstructionFiles(Path projectDirectory, List<AIAgentInstructionProvider> providers) {
        List<String> result = new ArrayList<>();
        for (AIAgentInstructionProvider provider : providers) {
            for (String name : provider.getInstructionFiles()) {
                if (!result.contains(name) && !Files.exists(projectDirectory.resolve(name))) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    public static Path createInstructionFile(Path projectDirectory, String name) throws IOException {
        return write(projectDirectory.resolve(name), "# " + name + "\n\nInstructions for the agents go here.\n");
    }

    public static Path createRule(Path projectDirectory, AIAgentRuleDirectory directory, String name) throws IOException {
        String base = sanitize(name, false);
        String fileName = hasExtension(base, directory.extensions()) ? base : base + directory.newFileExtension();
        String title = hasExtension(base, directory.extensions()) ? base.substring(0, base.lastIndexOf('.')) : base;
        return write(projectDirectory.resolve(directory.path()).resolve(fileName), directory.template().replace("{name}", title));
    }

    public static Path createSkill(Path skillsDirectory, String name) throws IOException {
        String folder = sanitize(name, true);
        String text = "---\n" +
            "name: " + folder + "\n" +
            "description: \"When to use this skill (be specific).\"\n" +
            "disable-model-invocation: false\n" +
            "---\n\n" +
            "# " + folder + "\n\n" +
            "Instructions for the agent go here.\n";
        return write(skillsDirectory.resolve(folder).resolve(SKILL_FILE), text);
    }

    public static void remove(AIAgentInstruction instruction) throws IOException {
        Path removed = instruction.removed();
        if (!Files.isDirectory(removed)) {
            Files.deleteIfExists(removed);
            return;
        }
        try (Stream<Path> walk = Files.walk(removed)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void skills(Path root, LocalizeValue agent, boolean global, Set<Path> seen, List<AIAgentInstruction> result) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root, SKILL_DEPTH)) {
            for (Path file : walk.filter(path -> path.getFileName().toString().equalsIgnoreCase(SKILL_FILE)).toList()) {
                Path folder = file.getParent();
                if (folder == null || folder.equals(root) || !seen.add(real(file))) {
                    continue;
                }
                String description = description(file);
                result.add(new AIAgentInstruction(AIAgentInstructionKind.SKILL, folder.getFileName().toString(), description, agent, file, folder, global));
            }
        }
        catch (IOException e) {
            LOG.warn("Cannot read skills of " + root, e);
        }
    }

    /**
     * The {@code description} of the front matter of a skill.
     */
    static String description(Path skillFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(skillFile, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            return "";
        }
        if (lines.isEmpty() || !lines.getFirst().strip().equals("---")) {
            return "";
        }
        for (int i = 1; i < lines.size(); i++) {
            String text = lines.get(i).strip();
            if (text.equals("---")) {
                break;
            }
            if (!text.toLowerCase(Locale.ROOT).startsWith("description:")) {
                continue;
            }
            String value = text.substring("description:".length()).strip();
            if (!value.startsWith(">") && !value.startsWith("|")) {
                return unquote(value);
            }
            // a block scalar - the indented lines after the key
            StringBuilder block = new StringBuilder();
            for (int j = i + 1; j < lines.size(); j++) {
                String next = lines.get(j);
                if (!next.isBlank() && !Character.isWhitespace(next.charAt(0))) {
                    break;
                }
                if (!next.isBlank()) {
                    if (!block.isEmpty()) {
                        block.append(' ');
                    }
                    block.append(next.strip());
                }
            }
            return block.toString();
        }
        return "";
    }

    private static String unquote(String text) {
        if (text.length() >= 2 && (text.startsWith("\"") && text.endsWith("\"") || text.startsWith("'") && text.endsWith("'"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static String sanitize(String name, boolean skill) {
        String text = name.strip();
        if (skill) {
            text = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
            if (text.length() > 64) {
                text = text.substring(0, 64);
            }
            return text.isEmpty() ? "skill" : text;
        }
        text = text.replaceAll("[\\\\/:*?\"<>|]", "_");
        return text.isEmpty() ? "rule" : text;
    }

    private static boolean hasExtension(String name, List<String> extensions) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lower.endsWith(extension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static Path write(Path file, String text) throws IOException {
        if (Files.exists(file)) {
            throw new FileAlreadyExistsException(file.toString());
        }
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }

    private static List<Path> list(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(directory)) {
            return children.toList();
        }
        catch (IOException e) {
            LOG.warn("Cannot list " + directory, e);
            return List.of();
        }
    }

    private static Path real(Path file) {
        try {
            return file.toRealPath();
        }
        catch (IOException e) {
            return file.toAbsolutePath().normalize();
        }
    }
}
