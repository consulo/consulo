// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.command;

import com.intellij.openapi.project.Project;

import javax.annotation.Nullable;

/**
 * Represents a command currently being executed by a command processor.
 *
 * @author yole
 * @see CommandProcessorEx#startCommand(Project, String, Object, UndoConfirmationPolicy)
 */
public interface CommandToken {
  @Nullable
  Project getProject();
}
