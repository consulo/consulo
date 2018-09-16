package com.intellij.psi.targets;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.pom.PomTarget;
import javax.annotation.Nonnull;

import java.util.Set;

public interface AliasingPsiTargetMapper {
  ExtensionPointName<AliasingPsiTargetMapper> EP_NAME = ExtensionPointName.create("com.intellij.aliasingPsiTargetMapper");

  @Nonnull
  Set<AliasingPsiTarget> getTargets(@Nonnull PomTarget target);
}
