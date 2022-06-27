package consulo.ide.impl.psi.targets;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.pom.PomTarget;
import javax.annotation.Nonnull;

import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface AliasingPsiTargetMapper {
  ExtensionPointName<AliasingPsiTargetMapper> EP_NAME = ExtensionPointName.create(AliasingPsiTargetMapper.class);

  @Nonnull
  Set<AliasingPsiTarget> getTargets(@Nonnull PomTarget target);
}
