package consulo.compiler.impl;

import com.intellij.compiler.impl.TranslatingCompilerFilesMonitorImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 13:36/06.11.13
 */
public abstract class TranslatingCompilerFilesMonitor {
  @NotNull
  public static TranslatingCompilerFilesMonitor getInstance() {
    return ApplicationManager.getApplication().getComponent(TranslatingCompilerFilesMonitor.class);
  }

  public abstract void suspendProject(Project project);

  public abstract void watchProject(Project project);

  public abstract boolean isSuspended(Project project);

  public abstract boolean isSuspended(int projectId);

  public abstract void collectFiles(CompileContext context,
                    TranslatingCompiler compiler,
                    Iterator<VirtualFile> scopeSrcIterator,
                    boolean forceCompile,
                    boolean isRebuild,
                    Collection<VirtualFile> toCompile,
                    Collection<Trinity<File, String, Boolean>> toDelete);

  public abstract void update(CompileContext context,
              @Nullable String outputRoot,
              Collection<TranslatingCompiler.OutputItem> successfullyCompiled,
              VirtualFile[] filesToRecompile) throws IOException;

  public abstract void updateOutputRootsLayout(Project project);

  public abstract List<String> getCompiledClassNames(VirtualFile srcFile, Project project);

  public abstract void scanSourceContent(TranslatingCompilerFilesMonitorImpl.ProjectRef projRef,
                         Collection<VirtualFile> roots,
                         int totalRootCount,
                         boolean isNewRoots);

  public abstract void ensureInitializationCompleted(Project project, ProgressIndicator indicator);

  public abstract void scanSourcesForCompilableFiles(Project project);

  public abstract boolean isMarkedForCompilation(Project project, VirtualFile file);
}
