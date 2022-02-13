package com.intellij.psi.targets;

import consulo.component.extension.ExtensionPointName;
import consulo.language.pom.PomTarget;
import javax.annotation.Nonnull;

import java.util.Set;

public interface AliasingPsiTargetMapper {
  ExtensionPointName<AliasingPsiTargetMapper> EP_NAME = ExtensionPointName.create("consulo.aliasingPsiTargetMapper");

  @Nonnull
  Set<AliasingPsiTarget> getTargets(@Nonnull PomTarget target);
}
