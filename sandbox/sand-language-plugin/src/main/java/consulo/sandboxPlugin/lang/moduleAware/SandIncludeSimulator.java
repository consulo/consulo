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
package consulo.sandboxPlugin.lang.moduleAware;

import consulo.logging.Logger;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Walks one file's {@code #flag} / {@code #undef} / {@code #include} / conditional
 * directives under a given entry environment, expanding includes in place (include-once
 * per walk, C semantics: an included file's definitions flow back into the includer).
 * Produces the end-of-file environment and the environment observed at every include
 * site. Purely on-demand — nothing project-wide is computed or retained.
 */
public final class SandIncludeSimulator {
    private static final Logger LOG = Logger.getInstance(SandIncludeSimulator.class);

    public record Walk(Set<String> endEnv, Map<VirtualFile, Set<Set<String>>> includeSiteEnvs) {
    }

    private SandIncludeSimulator() {
    }

    public static Walk walk(VirtualFile file, Set<String> entryEnv) {
        Map<VirtualFile, Set<Set<String>>> siteEnvs = new HashMap<>();
        Set<String> flags = new HashSet<>(entryEnv);
        expand(file, flags, siteEnvs, new HashSet<>());
        return new Walk(Set.copyOf(flags), siteEnvs);
    }

    private static void expand(VirtualFile file,
                               Set<String> flags,
                               Map<VirtualFile, Set<Set<String>>> siteEnvs,
                               Set<VirtualFile> visited) {
        if (!visited.add(file)) {
            return;
        }

        String text;
        try {
            text = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            LOG.warn("Cannot read " + file.getPath(), e);
            return;
        }

        Deque<boolean[]> levels = new ArrayDeque<>();
        boolean active = true;

        for (String rawLine : text.split("\n", -1)) {
            String line = rawLine.strip();
            if (!line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            String keyword = parts[0];
            String argument = parts.length > 1 ? parts[1] : null;

            switch (keyword) {
                case "#if", "#ifndef" -> {
                    boolean condition = argument != null && flags.contains(argument);
                    if (keyword.equals("#ifndef")) {
                        condition = argument != null && !flags.contains(argument);
                    }
                    boolean taken = active && condition;
                    levels.push(new boolean[]{active, taken});
                    active = taken;
                }
                case "#elif" -> {
                    boolean[] level = levels.peek();
                    if (level == null || !level[0] || level[1]) {
                        active = false;
                    }
                    else {
                        active = argument != null && flags.contains(argument);
                        if (active) {
                            level[1] = true;
                        }
                    }
                }
                case "#else" -> {
                    boolean[] level = levels.peek();
                    active = level != null && level[0] && !level[1];
                    if (active && level != null) {
                        level[1] = true;
                    }
                }
                case "#end" -> {
                    boolean[] level = levels.poll();
                    active = level == null || level[0];
                }
                case "#flag" -> {
                    if (active && argument != null) {
                        flags.add(argument);
                    }
                }
                case "#undef" -> {
                    if (active && argument != null) {
                        flags.remove(argument);
                    }
                }
                case "#include" -> {
                    if (active && argument != null) {
                        String name = argument.replace("\"", "");
                        VirtualFile parent = file.getParent();
                        VirtualFile target = parent == null ? null : parent.findChild(name);
                        if (target != null && target.getFileType() == SandFileType.INSTANCE) {
                            siteEnvs.computeIfAbsent(target, key -> new HashSet<>()).add(Set.copyOf(flags));
                            expand(target, flags, siteEnvs, visited);
                        }
                    }
                }
                default -> {
                }
            }
        }
    }
}
