// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.controlFlow;

/**
 * Marker interface for instructions that can be replaced by direct connection between predecessors and successors.
 */
public interface TransparentInstruction extends Instruction {
}
