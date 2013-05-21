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
package com.intellij.cyclicDependencies;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.packageDependencies.DependenciesBuilder;
import com.intellij.packageDependencies.ForwardDependenciesBuilder;
import com.intellij.psi.*;
import com.intellij.util.graph.CachingSemiGraph;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphAlgorithms;
import com.intellij.util.graph.GraphGenerator;

import java.util.*;

/**
 * User: anna
 * Date: Jan 30, 2005
 */
public class CyclicDependenciesBuilder{
  private final Project myProject;
  private final AnalysisScope myScope;
  private final Map<String, PsiJavaPackage> myPackages = new HashMap<String, PsiJavaPackage>();
  private Graph<PsiJavaPackage> myGraph;
  private final Map<PsiJavaPackage, Map<PsiJavaPackage, Set<PsiFile>>> myFilesInDependentPackages = new HashMap<PsiJavaPackage, Map<PsiJavaPackage, Set<PsiFile>>>();
  private final Map<PsiJavaPackage, Map<PsiJavaPackage, Set<PsiFile>>> myBackwardFilesInDependentPackages = new HashMap<PsiJavaPackage, Map<PsiJavaPackage, Set<PsiFile>>>();
  private final Map<PsiJavaPackage, Set<PsiJavaPackage>> myPackageDependencies = new HashMap<PsiJavaPackage, Set<PsiJavaPackage>>();
  private HashMap<PsiJavaPackage, Set<List<PsiJavaPackage>>> myCyclicDependencies = new HashMap<PsiJavaPackage, Set<List<PsiJavaPackage>>>();
  private int myFileCount = 0;
  private final ForwardDependenciesBuilder myForwardBuilder;

  private String myRootNodeNameInUsageView;

  public CyclicDependenciesBuilder(final Project project, final AnalysisScope scope) {
    myProject = project;
    myScope = scope;
    myForwardBuilder = new ForwardDependenciesBuilder(myProject, myScope){
      public String getRootNodeNameInUsageView() {
        return CyclicDependenciesBuilder.this.getRootNodeNameInUsageView();
      }

      public String getInitialUsagesPosition() {
        return AnalysisScopeBundle.message("cyclic.dependencies.usage.view.initial.text");
      }
    };
  }

  public String getRootNodeNameInUsageView() {
    return myRootNodeNameInUsageView;
  }

  public void setRootNodeNameInUsageView(final String rootNodeNameInUsageView) {
    myRootNodeNameInUsageView = rootNodeNameInUsageView;
  }

  public Project getProject() {
    return myProject;
  }

  public AnalysisScope getScope() {
    return myScope;
  }

  public DependenciesBuilder getForwardBuilder() {
    return myForwardBuilder;
  }

  public void analyze() {
    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
    getScope().accept(new PsiRecursiveElementVisitor() {
      @Override public void visitFile(PsiFile file) {
        if (file instanceof PsiJavaFile) {
          PsiJavaFile psiJavaFile = (PsiJavaFile)file;
          if (getScope().contains(psiJavaFile)) {
            final PsiJavaPackage aPackage = findPackage(psiJavaFile.getPackageName());
            if (aPackage != null) {
              myPackages.put(psiJavaFile.getPackageName(), aPackage);
            }
          }
          final Set<PsiJavaPackage> packs = getPackageHierarhy(psiJavaFile.getPackageName());
          final ForwardDependenciesBuilder builder = new ForwardDependenciesBuilder(getProject(), new AnalysisScope(psiJavaFile));
          builder.setTotalFileCount(getScope().getFileCount());
          builder.setInitialFileCount(++myFileCount);
          builder.analyze();
          final Set<PsiFile> psiFiles = builder.getDependencies().get(psiJavaFile);
          if (psiFiles == null) return;
          for (PsiJavaPackage pack : packs) {
            Set<PsiJavaPackage> pack2Packages = myPackageDependencies.get(pack);
            if (pack2Packages == null) {
              pack2Packages = new HashSet<PsiJavaPackage>();
              myPackageDependencies.put(pack, pack2Packages);
            }
            for (PsiFile psiFile : psiFiles) {
              if (!(psiFile instanceof PsiJavaFile) ||
                  !projectFileIndex.isInSourceContent(psiFile.getVirtualFile()) ||
                  !getScope().contains(psiFile)) {
                continue;
              }

              // construct dependent packages
              final String packageName = ((PsiJavaFile)psiFile).getPackageName();
              //do not depend on parent packages
              if (packageName.startsWith(pack.getQualifiedName())) {
                continue;
              }
              final PsiJavaPackage depPackage = findPackage(packageName);
              if (depPackage == null) { //not from analyze scope
                continue;
              }
              pack2Packages.add(depPackage);

              constractFilesInDependenciesPackagesMap(pack, depPackage, psiFile, myFilesInDependentPackages);
              constractFilesInDependenciesPackagesMap(depPackage, pack, psiJavaFile, myBackwardFilesInDependentPackages);
              constractWholeDependenciesMap(psiJavaFile, psiFile);
            }
          }
        }
      }
    });
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      if (indicator.isCanceled()) {
        throw new ProcessCanceledException();
      }
      indicator.setText(AnalysisScopeBundle.message("cyclic.dependencies.progress.text"));
      indicator.setText2("");
      indicator.setIndeterminate(true);
    }
    myCyclicDependencies = getCycles(myPackages.values());
  }

  private void constractFilesInDependenciesPackagesMap(final PsiJavaPackage pack,
                                                       final PsiJavaPackage depPackage,
                                                       final PsiFile file,
                                                       final Map<PsiJavaPackage, Map<PsiJavaPackage, Set<PsiFile>>> filesInDependentPackages) {
    Map<PsiJavaPackage, Set<PsiFile>> dependentPackages2Files = filesInDependentPackages.get(pack);
    if (dependentPackages2Files == null) {
      dependentPackages2Files = new HashMap<PsiJavaPackage, Set<PsiFile>>();
      filesInDependentPackages.put(pack, dependentPackages2Files);
    }
    Set<PsiFile> depFiles = dependentPackages2Files.get(depPackage);
    if (depFiles == null) {
      depFiles = new HashSet<PsiFile>();
      dependentPackages2Files.put(depPackage, depFiles);
    }
    depFiles.add(file);
  }

//construct all dependencies for usage view
  private void constractWholeDependenciesMap(final PsiJavaFile psiJavaFile, final PsiFile psiFile) {
    Set<PsiFile> wholeDependencies = myForwardBuilder.getDependencies().get(psiJavaFile);
    if (wholeDependencies == null) {
      wholeDependencies = new HashSet<PsiFile>();
      myForwardBuilder.getDependencies().put(psiJavaFile, wholeDependencies);
    }
    wholeDependencies.add(psiFile);
  }

  public Set<PsiFile> getDependentFilesInPackage(PsiJavaPackage pack, PsiJavaPackage depPack) {
    Set<PsiFile> psiFiles = new HashSet<PsiFile>();
    final Map<PsiJavaPackage, Set<PsiFile>> map = myFilesInDependentPackages.get(pack);
    if (map != null){
      psiFiles = map.get(depPack);
    }
    if (psiFiles == null) {
      psiFiles = new HashSet<PsiFile>();
    }
    return psiFiles;
  }

  public Set<PsiFile> getDependentFilesInPackage(PsiJavaPackage firstPack, PsiJavaPackage middlePack, PsiJavaPackage lastPack) {
    Set<PsiFile> result = new HashSet<PsiFile>();
    final Map<PsiJavaPackage, Set<PsiFile>> forwardMap = myFilesInDependentPackages.get(firstPack);
    if (forwardMap != null && forwardMap.get(middlePack) != null){
      result.addAll(forwardMap.get(middlePack));
    }
    final Map<PsiJavaPackage, Set<PsiFile>> backwardMap = myBackwardFilesInDependentPackages.get(lastPack);
    if (backwardMap != null && backwardMap.get(middlePack) != null){
      result.addAll(backwardMap.get(middlePack));
    }
    return result;
  }


  public HashMap<PsiJavaPackage, Set<List<PsiJavaPackage>>> getCyclicDependencies() {
    return myCyclicDependencies;
  }

  public HashMap<PsiJavaPackage, Set<List<PsiJavaPackage>>> getCycles(Collection<PsiJavaPackage> packages) {
    if (myGraph == null){
      myGraph = buildGraph();
    }
    final HashMap<PsiJavaPackage, Set<List<PsiJavaPackage>>> result = new HashMap<PsiJavaPackage, Set<List<PsiJavaPackage>>>();
    for (Iterator<PsiJavaPackage> iterator = packages.iterator(); iterator.hasNext();) {
      PsiJavaPackage psiPackage = iterator.next();
        Set<List<PsiJavaPackage>> paths2Pack = result.get(psiPackage);
        if (paths2Pack == null) {
          paths2Pack = new HashSet<List<PsiJavaPackage>>();
          result.put(psiPackage, paths2Pack);
        }
        paths2Pack.addAll(GraphAlgorithms.getInstance().findCycles(myGraph, psiPackage));
    }
    return result;
  }

  public Map<String, PsiJavaPackage> getAllScopePackages() {
    if (myPackages.isEmpty()) {
      final PsiManager psiManager = PsiManager.getInstance(getProject());
      getScope().accept(new PsiRecursiveElementVisitor() {
        @Override public void visitFile(PsiFile file) {
          if (file instanceof PsiJavaFile) {
            PsiJavaFile psiJavaFile = (PsiJavaFile)file;
            final PsiJavaPackage aPackage = JavaPsiFacade.getInstance(psiManager.getProject()).findPackage(psiJavaFile.getPackageName());
            if (aPackage != null) {
              myPackages.put(aPackage.getQualifiedName(), aPackage);
            }
          }
        }
      });
    }
    return myPackages;
  }


  private Graph<PsiJavaPackage> buildGraph() {
    final Graph<PsiJavaPackage> graph = GraphGenerator.create(CachingSemiGraph.create(new GraphGenerator.SemiGraph<PsiJavaPackage>() {
      public Collection<PsiJavaPackage> getNodes() {
        return getAllScopePackages().values();
      }

      public Iterator<PsiJavaPackage> getIn(PsiJavaPackage psiPack) {
        final Set<PsiJavaPackage> psiPackages = myPackageDependencies.get(psiPack);
        if (psiPackages == null) {     //for packs without java classes
          return new HashSet<PsiJavaPackage>().iterator();
        }
        return psiPackages.iterator();
      }
    }));
    return graph;
  }

  public Set<PsiJavaPackage> getPackageHierarhy(String packageName) {
    final Set<PsiJavaPackage> result = new HashSet<PsiJavaPackage>();
    PsiJavaPackage psiPackage = findPackage(packageName);
    if (psiPackage != null) {
      result.add(psiPackage);
    }
    else {
      return result;
    }
    while (psiPackage.getParentPackage() != null && psiPackage.getParentPackage().getQualifiedName().length() != 0) {
      final PsiJavaPackage aPackage = findPackage(psiPackage.getParentPackage().getQualifiedName());
      if (aPackage == null) {
        break;
      }
      result.add(aPackage);
      psiPackage = psiPackage.getParentPackage();
    }
    return result;
  }

  private PsiJavaPackage findPackage(String packName) {
    final PsiJavaPackage psiPackage = getAllScopePackages().get(packName);
    return psiPackage;
  }

}
