// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.util.ModificationTracker;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;

/**
 * An interface used to support tracking of common PSI modifications. It has three main usage patterns:
 * <ol>
 * <li/> Get a stamp of current PSI state. This stamp is increased when PSI is modified, allowing other subsystems
 * to check if PSI has changed since they accessed it last time. This can be used to flush and rebuild various internal caches.
 * See {@link #getModificationCount()}, {@link #getJavaStructureModificationCount()}, {@link #getOutOfCodeBlockModificationCount()}
 * <p>
 * <li/> Make a {@link CachedValue} instance dependent on a specific PSI modification tracker.
 * To achieve that, one should can one of the constants in this interface as {@link CachedValueProvider.Result}
 * dependencies.
 * <p>
 * <li/> Subscribe to any PSI change (for example, to drop caches in the listener manually).
 * See {@link PsiModificationTrackerListener}
 *
 * </ol>
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface PsiModificationTracker extends ModificationTracker {
  /**
   * Provides a way to get the instance of {@link PsiModificationTracker} corresponding to a given project.
   *
   * @see #getInstance(Project)
   */
  @Deprecated
  @DeprecationInfo("#getInstance(Project)")
  class SERVICE {
    private SERVICE() {
    }

    /**
     * @return The instance of {@link PsiModificationTracker} corresponding to the given project.
     */
    public static PsiModificationTracker getInstance(Project project) {
      return PsiModificationTracker.getInstance(project);
    }
  }

  @Nonnull
  static PsiModificationTracker getInstance(Project project) {
    return project.getInstance(PsiModificationTracker.class);
  }

  /**
   * This key can be passed as a dependency in a {@link CachedValueProvider}.
   * The corresponding {@link CachedValue} will then be flushed on every physical PSI change.
   *
   * @see #getModificationCount()
   */
  Key MODIFICATION_COUNT = Key.create("MODIFICATION_COUNT");

  /**
   * This key can be passed as a dependency in a {@link CachedValueProvider}.
   * The corresponding {@link CachedValue} will then be flushed on every physical PSI change that doesn't happen inside a Java code block.
   * This can include changes on Java class or file level, or changes in non-Java files, e.g. XML. Rarely needed.
   *
   * @see #getOutOfCodeBlockModificationCount()
   * @deprecated rarely supported by language plugins; also a wrong way for optimisations
   */
  @Deprecated
  Key OUT_OF_CODE_BLOCK_MODIFICATION_COUNT = Key.create("OUT_OF_CODE_BLOCK_MODIFICATION_COUNT");

  /**
   * This key can be passed as a dependency in a {@link CachedValueProvider}.
   * The corresponding {@link CachedValue} will then be flushed on every physical PSI change that can affect Java structure and resolve.
   *
   * @see #getJavaStructureModificationCount()
   * @deprecated rarely supported by JVM language plugins; also a wrong way for optimisations
   */
  @Deprecated
  Key JAVA_STRUCTURE_MODIFICATION_COUNT = Key.create("JAVA_STRUCTURE_MODIFICATION_COUNT");

  /**
   * Tracks any PSI modification.
   *
   * @return current counter value. Increased whenever any physical PSI is changed.
   */
  @Override
  long getModificationCount();

  /**
   * @return Same as {@link #getJavaStructureModificationCount()}, but also includes changes in non-Java files, e.g. XML. Rarely needed.
   * @deprecated rarely supported by language plugins; also a wrong way for optimisations
   */
  @Deprecated
  long getOutOfCodeBlockModificationCount();

  /**
   * @return an object returning {@link #getOutOfCodeBlockModificationCount()}
   * @deprecated rarely supported by language plugins; also a wrong way for optimisations
   */
  @Deprecated
  @Nonnull
  ModificationTracker getOutOfCodeBlockModificationTracker();

  /**
   * Tracks structural Java modifications, i.e. the ones on class/method/field/file level. Modifications inside method bodies are not tracked.
   * Useful to work with resolve caches that only depend on Java structure, and not the method code.
   *
   * @return current counter value. Increased whenever any physical PSI in Java structure is changed.
   * @deprecated rarely supported by JVM language plugins; also a wrong way for optimisations
   */
  @Deprecated
  long getJavaStructureModificationCount();

  /**
   * @return an object returning {@link #getJavaStructureModificationCount()}
   * @deprecated rarely supported by JVM language plugins; also a wrong way for optimisations
   */
  @Deprecated
  @Nonnull
  ModificationTracker getJavaStructureModificationTracker();

  /**
   * Increase count. Required write action. Please we careful calling this method
   */
  @RequiredWriteAction
  void incCounter();
}
