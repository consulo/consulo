package com.intellij.compiler.artifacts;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import consulo.roots.types.BinariesOrderRootType;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author nik
 */
public abstract class PackagingElementsTestCase extends ArtifactsTestCase {
  protected Artifact addArtifact(TestPackagingElementBuilder builder) {
    return addArtifact("a", builder);
  }

  protected Artifact addArtifact(final String name, TestPackagingElementBuilder builder) {
    return addArtifact(name, builder.build());
  }

  protected static void assertLayout(Artifact artifact, String expected) {
    assertLayout(artifact.getRootElement(), expected);
  }

  protected static void assertLayout(PackagingElement element, String expected) {
    ArtifactsTestUtil.assertLayout(element, expected);
  }

  protected String getProjectBasePath() {
    return getBaseDir().getPath();
  }

  protected VirtualFile getBaseDir() {
    final VirtualFile baseDir = myProject.getBaseDir();
    assertNotNull(baseDir);
    return baseDir;
  }

  protected TestPackagingElementBuilder root() {
    return TestPackagingElementBuilder.root(myProject);
  }

  protected TestPackagingElementBuilder archive(String name) {
    return TestPackagingElementBuilder.archive(myProject, name);
  }

  protected VirtualFile createFile(final String path) {
    return createFile(path, "");
  }

  protected VirtualFile createFile(final String path, final String text) {
    return VfsTestUtil.createFile(getBaseDir(), path, text);
  }

  protected VirtualFile createDir(final String path) {
    return VfsTestUtil.createDir(getBaseDir(), path);
  }

  protected static VirtualFile getJDomJar() {
    return getJarFromLibDirectory("jdom.jar");
  }

  protected static String getLocalJarPath(VirtualFile jarEntry) {
    return PathUtil.getLocalFile(jarEntry).getPath();
  }

  protected static String getJUnitJarPath() {
    return getLocalJarPath(getJarFromLibDirectory("junit.jar"));
  }

  private static VirtualFile getJarFromLibDirectory(final String relativePath) {
    final File file = PathManager.findFileInLibDirectory(relativePath);
    final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
    assertNotNull(file.getAbsolutePath() + " not found", virtualFile);
    final VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(virtualFile);
    assertNotNull(jarRoot);
    return jarRoot;
  }

  protected Library addProjectLibrary(final @Nullable Module module, final String name, final VirtualFile... jars) {
    return addProjectLibrary(module, name, DependencyScope.COMPILE, jars);
  }

  protected Library addProjectLibrary(final @Nullable Module module, final String name, final DependencyScope scope,
                                      final VirtualFile... jars) {
    return addProjectLibrary(myProject, module, name, scope, jars);
  }

  static Library addProjectLibrary(final Project project, final @Nullable Module module, final String name, final DependencyScope scope,
                                   final VirtualFile[] jars) {
    return new WriteAction<Library>() {
      @Override
      protected void run(final Result<Library> result) {
        final Library library = LibraryTablesRegistrar.getInstance().getLibraryTable(project).createLibrary(name);
        final Library.ModifiableModel libraryModel = library.getModifiableModel();
        for (VirtualFile jar : jars) {
          libraryModel.addRoot(jar, BinariesOrderRootType.getInstance());
        }
        libraryModel.commit();
        if (module != null) {
          ModuleRootModificationUtil.addDependency(module, library, scope, false);
        }
        result.setResult(library);
      }
    }.execute().getResultObject();
  }

  protected static void addModuleLibrary(final Module module, final VirtualFile jar) {
    ModuleRootModificationUtil.addModuleLibrary(module, jar.getUrl());
  }

  protected static void addModuleDependency(final Module module, final Module dependency) {
    ModuleRootModificationUtil.addDependency(module, dependency);
  }
}
