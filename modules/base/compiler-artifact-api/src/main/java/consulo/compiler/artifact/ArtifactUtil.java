/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.artifact;

import consulo.compiler.CompilerConfiguration;
import consulo.compiler.artifact.element.*;
import consulo.compiler.resourceCompiler.ResourceCompilerConfiguration;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FList;
import consulo.util.collection.SmartList;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author nik
 */
public class ArtifactUtil {
  private ArtifactUtil() {
  }

  public static CompositePackagingElement<?> copyFromRoot(@Nonnull CompositePackagingElement<?> oldRoot, @Nonnull Project project) {
    final CompositePackagingElement<?> newRoot = (CompositePackagingElement<?>)copyElement(oldRoot, project);
    copyChildren(oldRoot, newRoot, project);
    return newRoot;
  }


  public static void copyChildren(CompositePackagingElement<?> oldParent,
                                  CompositePackagingElement<?> newParent,
                                  @Nonnull Project project) {
    for (PackagingElement<?> child : oldParent.getChildren()) {
      newParent.addOrFindChild(copyWithChildren(child, project));
    }
  }

  @Nonnull
  public static <S> PackagingElement<S> copyWithChildren(@Nonnull PackagingElement<S> element, @Nonnull Project project) {
    final PackagingElement<S> copy = copyElement(element, project);
    if (element instanceof CompositePackagingElement<?>) {
      copyChildren((CompositePackagingElement<?>)element, (CompositePackagingElement<?>)copy, project);
    }
    return copy;
  }

  @Nonnull
  private static <S> PackagingElement<S> copyElement(@Nonnull PackagingElement<S> element, @Nonnull Project project) {
    //noinspection unchecked
    final PackagingElement<S> copy = (PackagingElement<S>)element.getType().createEmpty(project);
    copy.loadState(ArtifactManager.getInstance(project), element.getState());
    return copy;
  }

  public static <E extends PackagingElement<?>> boolean processPackagingElements(@Nonnull Artifact artifact,
                                                                                 @Nullable PackagingElementType<E> type,
                                                                                 @Nonnull final Predicate<? super E> processor,
                                                                                 final @Nonnull PackagingElementResolvingContext resolvingContext,
                                                                                 final boolean processSubstitutions) {
    return processPackagingElements(artifact, type, new PackagingElementProcessor<>() {
      @Override
      public boolean process(@Nonnull E e, @Nonnull PackagingElementPath path) {
        return processor.test(e);
      }
    }, resolvingContext, processSubstitutions);
  }

  public static <E extends PackagingElement<?>> boolean processPackagingElements(@Nonnull Artifact artifact,
                                                                                 @Nullable PackagingElementType<E> type,
                                                                                 @Nonnull PackagingElementProcessor<? super E> processor,
                                                                                 final @Nonnull PackagingElementResolvingContext resolvingContext,
                                                                                 final boolean processSubstitutions) {
    return processPackagingElements(artifact.getRootElement(),
                                    type,
                                    processor,
                                    resolvingContext,
                                    processSubstitutions,
                                    artifact.getArtifactType());
  }

  public static <E extends PackagingElement<?>> boolean processPackagingElements(final PackagingElement<?> rootElement,
                                                                                 @Nullable PackagingElementType<E> type,
                                                                                 @Nonnull PackagingElementProcessor<? super E> processor,
                                                                                 final @Nonnull PackagingElementResolvingContext resolvingContext,
                                                                                 final boolean processSubstitutions,
                                                                                 final ArtifactType artifactType) {
    return processElementRecursively(rootElement,
                                     type,
                                     processor,
                                     resolvingContext,
                                     processSubstitutions,
                                     artifactType,
                                     PackagingElementPath.EMPTY,
                                     new HashSet<>());
  }

  private static <E extends PackagingElement<?>> boolean processElementsRecursively(final List<? extends PackagingElement<?>> elements,
                                                                                    @Nullable PackagingElementType<E> type,
                                                                                    @Nonnull PackagingElementProcessor<? super E> processor,
                                                                                    final @Nonnull PackagingElementResolvingContext resolvingContext,
                                                                                    final boolean processSubstitutions,
                                                                                    ArtifactType artifactType,
                                                                                    @Nonnull PackagingElementPath path,
                                                                                    Set<PackagingElement<?>> processed) {
    for (PackagingElement<?> element : elements) {
      if (!processElementRecursively(element, type, processor, resolvingContext, processSubstitutions, artifactType, path, processed)) {
        return false;
      }
    }
    return true;
  }

  public static void processRecursivelySkippingIncludedArtifacts(Artifact artifact,
                                                                 final Predicate<PackagingElement<?>> processor,
                                                                 PackagingElementResolvingContext context) {
    processPackagingElements(artifact.getRootElement(), null, new PackagingElementProcessor<>() {
      @Override
      public boolean process(@Nonnull PackagingElement<?> element, @Nonnull PackagingElementPath path) {
        return processor.test(element);
      }

      @Override
      public boolean shouldProcessSubstitution(ComplexPackagingElement<?> element) {
        return !(element instanceof ArtifactPackagingElement);
      }
    }, context, true, artifact.getArtifactType());
  }

  private static <E extends PackagingElement<?>> boolean processElementRecursively(@Nonnull PackagingElement<?> element,
                                                                                   @Nullable PackagingElementType<E> type,
                                                                                   @Nonnull PackagingElementProcessor<? super E> processor,
                                                                                   @Nonnull PackagingElementResolvingContext resolvingContext,
                                                                                   final boolean processSubstitutions,
                                                                                   ArtifactType artifactType,
                                                                                   @Nonnull PackagingElementPath path,
                                                                                   Set<PackagingElement<?>> processed) {
    if (!processor.shouldProcess(element) || !processed.add(element)) {
      return true;
    }
    if (type == null || element.getType() == type) {
      if (!processor.process((E)element, path)) {
        return false;
      }
    }
    if (element instanceof CompositePackagingElement<?>) {
      final CompositePackagingElement<?> composite = (CompositePackagingElement<?>)element;
      return processElementsRecursively(composite.getChildren(),
                                        type,
                                        processor,
                                        resolvingContext,
                                        processSubstitutions,
                                        artifactType,
                                        path.appendComposite(composite),
                                        processed);
    }
    else if (element instanceof ComplexPackagingElement<?> && processSubstitutions) {
      final ComplexPackagingElement<?> complexElement = (ComplexPackagingElement<?>)element;
      if (processor.shouldProcessSubstitution(complexElement)) {
        final List<? extends PackagingElement<?>> substitution = complexElement.getSubstitution(resolvingContext, artifactType);
        if (substitution != null) {
          return processElementsRecursively(substitution,
                                            type,
                                            processor,
                                            resolvingContext,
                                            processSubstitutions,
                                            artifactType,
                                            path.appendComplex(complexElement),
                                            processed);
        }
      }
    }
    return true;
  }

  public static void removeDuplicates(@Nonnull CompositePackagingElement<?> parent) {
    List<PackagingElement<?>> prevChildren = new ArrayList<>();

    List<PackagingElement<?>> toRemove = new ArrayList<>();
    for (PackagingElement<?> child : parent.getChildren()) {
      if (child instanceof CompositePackagingElement<?>) {
        removeDuplicates((CompositePackagingElement<?>)child);
      }
      boolean merged = false;
      for (PackagingElement<?> prevChild : prevChildren) {
        if (child.isEqualTo(prevChild)) {
          if (child instanceof CompositePackagingElement<?>) {
            for (PackagingElement<?> childElement : ((CompositePackagingElement<?>)child).getChildren()) {
              ((CompositePackagingElement<?>)prevChild).addOrFindChild(childElement);
            }
          }
          merged = true;
          break;
        }
      }
      if (merged) {
        toRemove.add(child);
      }
      else {
        prevChildren.add(child);
      }
    }

    for (PackagingElement<?> child : toRemove) {
      parent.removeChild(child);
    }
  }

  public static <S> void copyProperties(ArtifactProperties<?> from, ArtifactProperties<S> to) {
    //noinspection unchecked
    to.loadState((S)from.getState());
  }

  @Nullable
  public static String getDefaultArtifactOutputPath(@Nonnull String artifactName, final @Nonnull Project project) {
    final CompilerConfiguration extension = CompilerConfiguration.getInstance(project);
    String outputUrl = extension.getCompilerOutputUrl();
    if (outputUrl == null || outputUrl.length() == 0) {
      final VirtualFile baseDir = project.getBaseDir();
      if (baseDir == null) return null;
      outputUrl = baseDir.getUrl() + "/out";
    }
    return VirtualFileUtil.urlToPath(outputUrl) + "/artifacts/" + FileUtil.sanitizeFileName(artifactName);
  }

  public static <E extends PackagingElement<?>> boolean processElementsWithSubstitutions(@Nonnull List<? extends PackagingElement<?>> elements,
                                                                                         @Nonnull PackagingElementResolvingContext context,
                                                                                         @Nonnull ArtifactType artifactType,
                                                                                         @Nonnull PackagingElementPath parentPath,
                                                                                         @Nonnull PackagingElementProcessor<E> processor) {
    return processElementsWithSubstitutions(elements, context, artifactType, parentPath, processor, new HashSet<>());
  }

  private static <E extends PackagingElement<?>> boolean processElementsWithSubstitutions(@Nonnull List<? extends PackagingElement<?>> elements,
                                                                                          @Nonnull PackagingElementResolvingContext context,
                                                                                          @Nonnull ArtifactType artifactType,
                                                                                          @Nonnull PackagingElementPath parentPath,
                                                                                          @Nonnull PackagingElementProcessor<E> processor,
                                                                                          final Set<PackagingElement<?>> processed) {
    for (PackagingElement<?> element : elements) {
      if (!processed.add(element)) {
        continue;
      }

      if (element instanceof ComplexPackagingElement<?> && processor.shouldProcessSubstitution((ComplexPackagingElement)element)) {
        final ComplexPackagingElement<?> complexElement = (ComplexPackagingElement<?>)element;
        final List<? extends PackagingElement<?>> substitution = complexElement.getSubstitution(context, artifactType);
        if (substitution != null && !processElementsWithSubstitutions(substitution,
                                                                      context,
                                                                      artifactType,
                                                                      parentPath.appendComplex(complexElement),
                                                                      processor,
                                                                      processed)) {
          return false;
        }
      }
      else if (!processor.process((E)element, parentPath)) {
        return false;
      }
    }
    return true;
  }

  public static List<PackagingElement<?>> findByRelativePath(@Nonnull CompositePackagingElement<?> parent,
                                                             @Nonnull String relativePath,
                                                             @Nonnull PackagingElementResolvingContext context,
                                                             @Nonnull ArtifactType artifactType) {
    final List<PackagingElement<?>> result = new ArrayList<>();
    processElementsByRelativePath(parent,
                                  relativePath,
                                  context,
                                  artifactType,
                                  PackagingElementPath.EMPTY,
                                  new PackagingElementProcessor<>() {
                                    @Override
                                    public boolean process(@Nonnull PackagingElement<?> packagingElement,
                                                           @Nonnull PackagingElementPath path) {
                                      result.add(packagingElement);
                                      return true;
                                    }
                                  });
    return result;
  }

  public static boolean processElementsByRelativePath(@Nonnull final CompositePackagingElement<?> parent,
                                                      @Nonnull String relativePath,
                                                      @Nonnull final PackagingElementResolvingContext context,
                                                      @Nonnull final ArtifactType artifactType,
                                                      @Nonnull PackagingElementPath parentPath,
                                                      @Nonnull final PackagingElementProcessor<PackagingElement<?>> processor) {
    relativePath = StringUtil.trimStart(relativePath, "/");
    if (relativePath.length() == 0) {
      return true;
    }

    int i = relativePath.indexOf('/');
    final String firstName = i != -1 ? relativePath.substring(0, i) : relativePath;
    final String tail = i != -1 ? relativePath.substring(i + 1) : "";

    return processElementsWithSubstitutions(parent.getChildren(),
                                            context,
                                            artifactType,
                                            parentPath.appendComposite(parent), new PackagingElementProcessor<>() {
        @Override
        public boolean process(@Nonnull PackagingElement<?> element,
                               @Nonnull PackagingElementPath path) {
          boolean process = false;
          if (element instanceof CompositePackagingElement && firstName.equals(((CompositePackagingElement<?>)element).getName())) {
            process = true;
          }
          else if (element instanceof FileCopyPackagingElement) {
            final FileCopyPackagingElement fileCopy = (FileCopyPackagingElement)element;
            if (firstName.equals(fileCopy.getOutputFileName())) {
              process = true;
            }
          }

          if (process) {
            if (tail.length() == 0) {
              if (!processor.process(element, path)) return false;
            }
            else if (element instanceof CompositePackagingElement<?>) {
              return processElementsByRelativePath((CompositePackagingElement)element,
                                                   tail,
                                                   context,
                                                   artifactType,
                                                   path,
                                                   processor);
            }
          }
          return true;
        }
      });
  }

  public static boolean processDirectoryChildren(@Nonnull CompositePackagingElement<?> parent,
                                                 @Nonnull PackagingElementPath pathToParent,
                                                 @Nonnull String relativePath,
                                                 @Nonnull final PackagingElementResolvingContext context,
                                                 @Nonnull final ArtifactType artifactType,
                                                 @Nonnull final PackagingElementProcessor<PackagingElement<?>> processor) {
    return processElementsByRelativePath(parent,
                                         relativePath,
                                         context,
                                         artifactType,
                                         pathToParent, new PackagingElementProcessor<>() {
        @Override
        public boolean process(@Nonnull PackagingElement<?> element,
                               @Nonnull PackagingElementPath path) {
          if (element instanceof DirectoryPackagingElement) {
            final List<PackagingElement<?>> children =
              ((DirectoryPackagingElement)element).getChildren();
            if (!processElementsWithSubstitutions(children,
                                                  context,
                                                  artifactType,
                                                  path.appendComposite((DirectoryPackagingElement)element),
                                                  processor)) {
              return false;
            }
          }
          return true;
        }
      });
  }

  public static void processFileOrDirectoryCopyElements(Artifact artifact,
                                                        PackagingElementProcessor<FileOrDirectoryCopyPackagingElement<?>> processor,
                                                        PackagingElementResolvingContext context,
                                                        boolean processSubstitutions) {
    processPackagingElements(artifact, FileCopyElementType.getInstance(), processor, context, processSubstitutions);
    processPackagingElements(artifact, DirectoryCopyElementType.getInstance(), processor, context, processSubstitutions);
    processPackagingElements(artifact, ExtractedDirectoryElementType.getInstance(), processor, context, processSubstitutions);
  }

  public static Collection<Trinity<Artifact, PackagingElementPath, String>> findContainingArtifactsWithOutputPaths(@Nonnull final VirtualFile file,
                                                                                                                   @Nonnull Project project,
                                                                                                                   final Artifact[] artifacts) {
    final boolean isResourceFile = ResourceCompilerConfiguration.getInstance(project).isResourceFile(file);
    final List<Trinity<Artifact, PackagingElementPath, String>> result = new ArrayList<>();
    final PackagingElementResolvingContext context = ArtifactManager.getInstance(project).getResolvingContext();
    for (final Artifact artifact : artifacts) {
      processPackagingElements(artifact, null, new PackagingElementProcessor<>() {
        @Override
        public boolean process(@Nonnull PackagingElement<?> element, @Nonnull PackagingElementPath path) {
          if (element instanceof FileOrDirectoryCopyPackagingElement<?>) {
            final VirtualFile root = ((FileOrDirectoryCopyPackagingElement)element).findFile();
            if (root != null && VirtualFileUtil.isAncestor(root, file, false)) {
              final String relativePath;
              if (root.equals(file) && element instanceof FileCopyPackagingElement) {
                relativePath = ((FileCopyPackagingElement)element).getOutputFileName();
              }
              else {
                relativePath = VirtualFileUtil.getRelativePath(file, root, '/');
              }
              result.add(Trinity.create(artifact, path, relativePath));
              return false;
            }
          }
          else if (isResourceFile && element instanceof ModuleOutputPackagingElement) {
            final String relativePath = getRelativePathInSources(file, (ModuleOutputPackagingElement)element, context);
            if (relativePath != null) {
              result.add(Trinity.create(artifact, path, relativePath));
              return false;
            }
          }
          return true;
        }
      }, context, true);
    }
    return result;
  }

  @Nullable
  private static String getRelativePathInSources(@Nonnull VirtualFile file,
                                                 final @Nonnull ModuleOutputPackagingElement moduleElement,
                                                 @Nonnull PackagingElementResolvingContext context) {
    for (VirtualFile sourceRoot : moduleElement.getSourceRoots(context)) {
      if (VirtualFileUtil.isAncestor(sourceRoot, file, true)) {
        return VirtualFileUtil.getRelativePath(file, sourceRoot, '/');
      }
    }
    return null;
  }

  @Nullable
  public static VirtualFile findSourceFileByOutputPath(Artifact artifact, String outputPath, PackagingElementResolvingContext context) {
    final List<VirtualFile> files = findSourceFilesByOutputPath(artifact.getRootElement(), outputPath, context, artifact.getArtifactType());
    return files.isEmpty() ? null : files.get(0);
  }

  @Nullable
  public static VirtualFile findSourceFileByOutputPath(CompositePackagingElement<?> parent,
                                                       String outputPath,
                                                       PackagingElementResolvingContext context,
                                                       ArtifactType artifactType) {
    final List<VirtualFile> files = findSourceFilesByOutputPath(parent, outputPath, context, artifactType);
    return files.isEmpty() ? null : files.get(0);
  }

  public static List<VirtualFile> findSourceFilesByOutputPath(CompositePackagingElement<?> parent,
                                                              final String outputPath,
                                                              final PackagingElementResolvingContext context,
                                                              final ArtifactType artifactType) {
    final String path = StringUtil.trimStart(outputPath, "/");
    if (path.length() == 0) {
      return Collections.emptyList();
    }

    int i = path.indexOf('/');
    final String firstName = i != -1 ? path.substring(0, i) : path;
    final String tail = i != -1 ? path.substring(i + 1) : "";

    final List<VirtualFile> result = new SmartList<>();
    processElementsWithSubstitutions(parent.getChildren(),
                                     context,
                                     artifactType,
                                     PackagingElementPath.EMPTY, new PackagingElementProcessor<>() {
        @Override
        public boolean process(@Nonnull PackagingElement<?> element,
                               @Nonnull PackagingElementPath elementPath) {
          //todo[nik] replace by method findSourceFile() in PackagingElement
          if (element instanceof CompositePackagingElement) {
            final CompositePackagingElement<?> compositeElement = (CompositePackagingElement<?>)element;
            if (firstName.equals(compositeElement.getName())) {
              result.addAll(findSourceFilesByOutputPath(compositeElement, tail, context, artifactType));
            }
          }
          else if (element instanceof FileCopyPackagingElement) {
            final FileCopyPackagingElement fileCopyElement = (FileCopyPackagingElement)element;
            if (firstName.equals(fileCopyElement.getOutputFileName()) && tail.length() == 0) {
              ContainerUtil.addIfNotNull(result, fileCopyElement.findFile());
            }
          }
          else if (element instanceof DirectoryCopyPackagingElement || element instanceof ExtractedDirectoryPackagingElement) {
            final VirtualFile sourceRoot = ((FileOrDirectoryCopyPackagingElement<?>)element).findFile();
            if (sourceRoot != null) {
              ContainerUtil.addIfNotNull(result, sourceRoot.findFileByRelativePath(path));
            }
          }
          else if (element instanceof ModuleOutputPackagingElement) {
            for (VirtualFile sourceRoot : ((ModuleOutputPackagingElement)element).getSourceRoots(context)) {
              final VirtualFile sourceFile = sourceRoot.findFileByRelativePath(path);
              if (sourceFile != null && ResourceCompilerConfiguration.getInstance(context.getProject())
                                                                     .isResourceFile(sourceFile)) {
                result.add(sourceFile);
              }
            }
          }
          return true;
        }
      });

    return result;
  }

  public static boolean processParents(@Nonnull Artifact artifact,
                                       @Nonnull PackagingElementResolvingContext context,
                                       @Nonnull ParentElementProcessor processor,
                                       int maxLevel) {
    return processParents(artifact,
                          context,
                          processor,
                          FList.<Pair<Artifact, CompositePackagingElement<?>>>emptyList(),
                          maxLevel,
                          new HashSet<>());
  }

  private static boolean processParents(@Nonnull final Artifact artifact,
                                        @Nonnull final PackagingElementResolvingContext context,
                                        @Nonnull final ParentElementProcessor processor,
                                        FList<Pair<Artifact, CompositePackagingElement<?>>> pathToElement,
                                        final int maxLevel,
                                        final Set<Artifact> processed) {
    if (!processed.add(artifact)) return true;

    final FList<Pair<Artifact, CompositePackagingElement<?>>> pathFromRoot;
    final CompositePackagingElement<?> rootElement = artifact.getRootElement();
    if (rootElement instanceof ArtifactRootElement<?>) {
      pathFromRoot = pathToElement;
    }
    else {
      if (!processor.process(rootElement, pathToElement, artifact)) {
        return false;
      }
      pathFromRoot = pathToElement.prepend(new Pair<>(artifact, rootElement));
    }
    if (pathFromRoot.size() > maxLevel) return true;

    for (final Artifact anArtifact : context.getArtifactModel().getArtifacts()) {
      if (processed.contains(anArtifact)) continue;

      final PackagingElementProcessor<ArtifactPackagingElement> elementProcessor =
        new PackagingElementProcessor<>() {
          @Override
          public boolean shouldProcessSubstitution(ComplexPackagingElement<?> element) {
            return !(element instanceof ArtifactPackagingElement);
          }

          @Override
          public boolean process(@Nonnull ArtifactPackagingElement element, @Nonnull PackagingElementPath path) {
            if (artifact.getName().equals(element.getArtifactName())) {
              FList<Pair<Artifact, CompositePackagingElement<?>>> currentPath = pathFromRoot;
              final List<CompositePackagingElement<?>> parents = path.getParents();
              for (int i = 0, parentsSize = parents.size(); i < parentsSize - 1; i++) {
                CompositePackagingElement<?> parent = parents.get(i);
                if (!processor.process(parent, currentPath, anArtifact)) {
                  return false;
                }
                currentPath = currentPath.prepend(new Pair<>(anArtifact, parent));
                if (currentPath.size() > maxLevel) {
                  return true;
                }
              }

              if (!parents.isEmpty()) {
                CompositePackagingElement<?> lastParent = parents.get(parents.size() - 1);
                if (lastParent instanceof ArtifactRootElement<?> && !processor.process(lastParent, currentPath, anArtifact)) {
                  return false;
                }
              }
              return processParents(anArtifact, context, processor, currentPath, maxLevel, processed);
            }
            return true;
          }
        };
      if (!processPackagingElements(anArtifact, ArtifactElementType.getInstance(), elementProcessor, context, true)) {
        return false;
      }
    }
    return true;
  }

  public static void removeChildrenRecursively(@Nonnull CompositePackagingElement<?> element,
                                               @Nonnull Predicate<PackagingElement<?>> condition) {
    List<PackagingElement<?>> toRemove = new ArrayList<>();
    for (PackagingElement<?> child : element.getChildren()) {
      if (child instanceof CompositePackagingElement<?>) {
        final CompositePackagingElement<?> compositeChild = (CompositePackagingElement<?>)child;
        removeChildrenRecursively(compositeChild, condition);
        if (compositeChild.getChildren().isEmpty()) {
          toRemove.add(child);
        }
      }
      else if (condition.test(child)) {
        toRemove.add(child);
      }
    }

    element.removeChildren(toRemove);
  }

  public static boolean shouldClearArtifactOutputBeforeRebuild(Artifact artifact) {
    final String outputPath = artifact.getOutputPath();
    return !StringUtil.isEmpty(outputPath) && artifact.getRootElement() instanceof ArtifactRootElement<?>;
  }

  public static Set<Module> getModulesIncludedInArtifacts(final @Nonnull Collection<? extends Artifact> artifacts,
                                                          final @Nonnull Project project) {
    // not interest in includeTestScope
    return getModulesIncludedInArtifacts(artifacts, project, new boolean[1]);
  }

  public static Set<Module> getModulesIncludedInArtifacts(@Nonnull Collection<? extends Artifact> artifacts,
                                                          @Nonnull Project project,
                                                          boolean[] includeTestScope) {
    final Set<Module> modules = new HashSet<>();
    final PackagingElementResolvingContext resolvingContext = ArtifactManager.getInstance(project).getResolvingContext();
    for (Artifact artifact : artifacts) {
      processPackagingElements(artifact, null, element -> {
        if (element instanceof ModuleOutputPackagingElement moduleOutputPackagingElement) {
          Module module = moduleOutputPackagingElement.findModule(resolvingContext);
          if (module != null) {
            modules.add(module);

            if (LanguageContentFolderScopes.test().test(moduleOutputPackagingElement.getContentFolderType())) {
              includeTestScope[0] = true;
            }
          }
        }
        return true;
      }, resolvingContext, true);
    }
    return modules;
  }

  public static Collection<Artifact> getArtifactsContainingModuleOutput(@Nonnull final Module module) {
    ArtifactManager artifactManager = ArtifactManager.getInstance(module.getProject());
    final PackagingElementResolvingContext context = artifactManager.getResolvingContext();
    final Set<Artifact> result = new HashSet<>();
    Predicate<PackagingElement<?>> processor = element -> {
      if (element instanceof ModuleOutputPackagingElement &&
        module.equals(((ModuleOutputPackagingElement)element).findModule(context)) &&
        ((ModuleOutputPackagingElement)element).getContentFolderType() == ProductionContentFolderTypeProvider.getInstance()) {
        return false;
      }
      if (element instanceof ArtifactPackagingElement && result.contains(((ArtifactPackagingElement)element).findArtifact(context))) {
        return false;
      }
      return true;
    };
    for (Artifact artifact : artifactManager.getSortedArtifacts()) {
      boolean contains = !processPackagingElements(artifact, null, processor, context, true);
      if (contains) {
        result.add(artifact);
      }
    }
    return result;
  }

  public static List<Artifact> getArtifactWithOutputPaths(Project project) {
    final List<Artifact> result = new ArrayList<>();
    for (Artifact artifact : ArtifactManager.getInstance(project).getSortedArtifacts()) {
      if (!StringUtil.isEmpty(artifact.getOutputPath())) {
        result.add(artifact);
      }
    }
    return result;
  }

  public static String suggestArtifactFileName(String artifactName) {
    return PathUtil.suggestFileName(artifactName, true, true);
  }

  @Nullable
  private static PackagingElement<?> findArchiveOrDirectoryByName(@Nonnull CompositePackagingElement<?> parent, @Nonnull String name) {
    for (PackagingElement<?> element : parent.getChildren()) {
      if (element instanceof ArchivePackagingElement && ((ArchivePackagingElement)element).getArchiveFileName().equals(name) ||
        element instanceof DirectoryPackagingElement && ((DirectoryPackagingElement)element).getDirectoryName().equals(name)) {
        return element;
      }
    }
    return null;
  }

  @Nonnull
  public static String suggestFileName(@Nonnull CompositePackagingElement<?> parent,
                                       @Nonnull String prefix,
                                       @Nonnull String suffix) {
    String name = prefix + suffix;
    int i = 2;
    while (findArchiveOrDirectoryByName(parent, name) != null) {
      name = prefix + i++ + suffix;
    }
    return name;
  }

  public static String trimForwardSlashes(@Nonnull String path) {
    while (path.length() != 0 && (path.charAt(0) == '/' || path.charAt(0) == File.separatorChar)) {
      path = path.substring(1);
    }
    return path;
  }

  public static String concatPaths(String... paths) {
    final StringBuilder builder = new StringBuilder();
    for (String path : paths) {
      if (path.length() == 0) continue;

      final int len = builder.length();
      if (len > 0 && builder.charAt(len - 1) != '/' && builder.charAt(len - 1) != File.separatorChar) {
        builder.append('/');
      }
      builder.append(len != 0 ? trimForwardSlashes(path) : path);
    }
    return builder.toString();
  }

  public static String appendToPath(@Nonnull String basePath, @Nonnull String relativePath) {
    final boolean endsWithSlash = StringUtil.endsWithChar(basePath, '/') || StringUtil.endsWithChar(basePath, '\\');
    final boolean startsWithSlash = StringUtil.startsWithChar(relativePath, '/') || StringUtil.startsWithChar(relativePath, '\\');
    String tail;
    if (endsWithSlash && startsWithSlash) {
      tail = trimForwardSlashes(relativePath);
    }
    else if (!endsWithSlash && !startsWithSlash && basePath.length() > 0 && relativePath.length() > 0) {
      tail = "/" + relativePath;
    }
    else {
      tail = relativePath;
    }
    return basePath + tail;
  }
}

