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

import consulo.localize.LocalizeValue;

import java.nio.file.Path;

/**
 * An instruction an agent reads - an instruction file, a rule or a skill.
 *
 * @param file        the file to edit - {@code SKILL.md} for a skill
 * @param removed     what goes when it is removed - the directory of a skill
 * @param description of a skill, from its front matter
 * @param global      it lives in the home directory of the user, not in the project
 * @author VISTALL
 * @since 2026-11-14
 */
public record AIAgentInstruction(
    AIAgentInstructionKind kind,
    String name,
    String description,
    LocalizeValue agent,
    Path file,
    Path removed,
    boolean global
) {
}
