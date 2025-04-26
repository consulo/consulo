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
package consulo.ide.impl.psi.search.scope.packageSet;

import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.content.scope.PatternBasedPackageSet;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class PatternPackageSet extends PatternBasedPackageSet {
    public static final String SCOPE_TEST = "test";
    public static final String SCOPE_SOURCE = "src";
    public static final String SCOPE_LIBRARY = "lib";
    public static final String SCOPE_PROBLEM = "problem";
    public static final String SCOPE_ANY = "";

    private final Pattern myPattern;
    private final Pattern myModulePattern;
    private final Pattern myModuleGroupPattern;
    private final String myAspectJSyntaxPattern;
    private final String myScope;
    private final String myModulePatternText;

    public PatternPackageSet(@NonNls @Nullable String aspectPattern,
                             @Nonnull String scope,
                             @NonNls String modulePattern) {
        myAspectJSyntaxPattern = aspectPattern;
        myScope = scope;
        myModulePatternText = modulePattern;
        Pattern mmgp = null;
        Pattern mmp = null;
        if (modulePattern == null || modulePattern.length() == 0) {
            mmp = null;
        }
        else {
            if (modulePattern.startsWith("group:")) {
                int idx = modulePattern.indexOf(':', 6);
                if (idx == -1) idx = modulePattern.length();
                mmgp = Pattern.compile(StringUtil.replace(modulePattern.substring(6, idx), "*", ".*"));
                if (idx < modulePattern.length() - 1) {
                    mmp = Pattern.compile(StringUtil.replace(modulePattern.substring(idx + 1), "*", ".*"));
                }
            }
            else {
                mmp = Pattern.compile(StringUtil.replace(modulePattern, "*", ".*"));
            }
        }
        myModulePattern = mmp;
        myModuleGroupPattern = mmgp;
        myPattern = aspectPattern != null ? Pattern.compile(FilePatternPackageSet.convertToRegexp(aspectPattern, '.')) : null;
    }

    @Override
    public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        return matchesScope(file, project, fileIndex) && (myPattern == null || myPattern.matcher(getPackageName(file, fileIndex)).matches());
    }

    private boolean matchesScope(VirtualFile file, Project project, ProjectFileIndex fileIndex) {
        if (file == null) return false;
        boolean isSource = fileIndex.isInSourceContent(file);
        if (myScope == SCOPE_ANY) {
            return fileIndex.isInContent(file) && FilePatternPackageSet.matchesModule(myModuleGroupPattern, myModulePattern, file, fileIndex);
        }
        if (myScope == SCOPE_SOURCE) {
            return isSource && !fileIndex.isInTestSourceContent(file) && FilePatternPackageSet.matchesModule(myModuleGroupPattern, myModulePattern,
                file, fileIndex);
        }
        if (myScope == SCOPE_LIBRARY) {
            return (fileIndex.isInLibraryClasses(file) || fileIndex.isInLibrarySource(file)) && matchesLibrary(myModulePattern, file, fileIndex);
        }
        if (myScope == SCOPE_TEST) {
            return isSource && fileIndex.isInTestSourceContent(file) && FilePatternPackageSet.matchesModule(myModuleGroupPattern, myModulePattern,
                file, fileIndex);
        }
        if (myScope == SCOPE_PROBLEM) {
            return isSource && WolfTheProblemSolver.getInstance(project).isProblemFile(file) &&
                FilePatternPackageSet.matchesModule(myModuleGroupPattern, myModulePattern, file, fileIndex);
        }
        throw new RuntimeException("Unknown scope: " + myScope);
    }

    private static String getPackageName(VirtualFile file, ProjectFileIndex fileIndex) {
        return StringUtil.getQualifiedName(fileIndex.getPackageNameByDirectory(file.isDirectory() ? file : file.getParent()), file.getNameWithoutExtension());
    }

    @Nonnull
    @Override
    public PackageSet createCopy() {
        return new PatternPackageSet(myAspectJSyntaxPattern, myScope, myModulePatternText);
    }

    @Override
    public int getNodePriority() {
        return 0;
    }

    @Nonnull
    @Override
    public String getText() {
        StringBuilder buf = new StringBuilder();
        if (myScope != SCOPE_ANY) {
            buf.append(myScope);
        }

        if (myModulePattern != null || myModuleGroupPattern != null) {
            buf.append("[").append(myModulePatternText).append("]");
        }

        if (buf.length() > 0) {
            buf.append(':');
        }

        buf.append(myAspectJSyntaxPattern);
        return buf.toString();
    }

    @Override
    public String getModulePattern() {
        return myModulePatternText;
    }

    @Override
    public boolean isOn(String oldQName) {
        return Comparing.strEqual(oldQName, myAspectJSyntaxPattern) || //class qname
            Comparing.strEqual(oldQName + "..*", myAspectJSyntaxPattern) || //package req
            Comparing.strEqual(oldQName + ".*", myAspectJSyntaxPattern); //package
    }

    @Override
    public String getPattern() {
        return myAspectJSyntaxPattern;
    }

    public static boolean matchesLibrary(final Pattern libPattern,
                                         final VirtualFile file,
                                         final ProjectFileIndex fileIndex) {
        if (libPattern != null) {
            final List<OrderEntry> entries = fileIndex.getOrderEntriesForFile(file);
            for (OrderEntry orderEntry : entries) {
                if (orderEntry instanceof LibraryOrderEntry) {
                    final String libraryName = ((LibraryOrderEntry) orderEntry).getLibraryName();
                    if (libraryName != null) {
                        if (libPattern.matcher(libraryName).matches()) return true;
                    }
                    else {
                        final String presentableName = orderEntry.getPresentableName();
                        final String fileName = new File(presentableName).getName();
                        if (libPattern.matcher(fileName).matches()) return true;
                    }
                }
                else if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry) {
                    final String jdkName = ((ModuleExtensionWithSdkOrderEntry) orderEntry).getSdkName();
                    if (jdkName != null && libPattern.matcher(jdkName).matches()) return true;
                }
            }
            return false;
        }
        return true;
    }
}