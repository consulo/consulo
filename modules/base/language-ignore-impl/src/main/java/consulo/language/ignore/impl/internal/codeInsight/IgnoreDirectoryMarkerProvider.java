/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package consulo.language.ignore.impl.internal.codeInsight;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.Language;
import consulo.language.editor.Pass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.cache.PatternCache;
import consulo.language.ignore.localize.LanguageIgnoreLocalize;
import consulo.language.ignore.psi.IgnoreEntryDirectory;
import consulo.language.ignore.psi.IgnoreEntryFile;
import consulo.language.ignore.util.RegexUtil;
import consulo.language.impl.ast.LeafElement;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * {@link LineMarkerProviderDescriptor} that marks entry lines with directory icon if they point to the directory in virtual system.
 */
@ExtensionImpl
public final class IgnoreDirectoryMarkerProvider extends LineMarkerProviderDescriptor implements DumbAware {
    @Override
    public Language getLanguage() {
        return IgnoreLanguage.INSTANCE;
    }

    @Override
    @RequiredReadAction
    public @Nullable LineMarkerInfo getLineMarkerInfo(PsiElement element) {
        return null;
    }

    private record IgnoreFileRecord(IgnoreEntryFile entry, Pattern pattern) {
    }

    @Override
    @RequiredReadAction
    @SuppressWarnings("deprecation")
    public void collectSlowLineMarkers(List<PsiElement> elements, Collection<LineMarkerInfo> result) {
        if (elements.isEmpty()) {
            return;
        }

        Collection<IgnoreFileRecord> ignoreEntryFiles = new HashSet<>(elements.size());
        List<IgnoreEntryFile> ignoreEntryDirectories = new ArrayList<>(elements.size());

        Project project = elements.get(0).getProject();
        PatternCache patternCache = PatternCache.getInstance(project);
        PsiFile psiFile = elements.get(0).getContainingFile();
        VirtualFile containingFile = psiFile != null ? psiFile.getVirtualFile() : null;
        VirtualFile parent = containingFile != null ? containingFile.getParent() : null;
        VirtualFile projectDir = project.getBaseDir();
        boolean isRootFile = parent == null || projectDir == null;

        for (PsiElement element : elements) {
            ProgressManager.checkCanceled();
            if (element instanceof IgnoreEntryFile entry) {
                if (entry instanceof IgnoreEntryDirectory dir) {
                    ignoreEntryDirectories.add(dir);
                }
                else {
                    Pattern pattern = patternCache.createPattern(entry);
                    if (pattern != null && !isRootFile) {
                        ignoreEntryFiles.add(new IgnoreFileRecord(entry, pattern));
                    }
                }
            }
        }

        if (parent != null && !ignoreEntryFiles.isEmpty()) {
            computerDirectoriesExist(parent, ignoreEntryFiles, ignoreEntryDirectories);
        }

        for (IgnoreEntryFile directory : ignoreEntryDirectories) {
            ProgressManager.checkCanceled();
            PsiElement leafElement = firstLeafOrNull(directory);
            if (leafElement != null) {
                result.add(new LineMarkerInfo<>(
                    leafElement,
                    directory.getTextRange(),
                    PlatformIconGroup.nodesFolder(),
                    Pass.LINE_MARKERS,
                    null,
                    null,
                    GutterIconRenderer.Alignment.CENTER
                ));
            }
        }
    }

    @Override
    public LocalizeValue getName() {
        return LanguageIgnoreLocalize.gutterNameVersionControlIgnoredDirectories();
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesFolder();
    }

    private static @Nullable PsiElement firstLeafOrNull(PsiElement element) {
        LeafElement firstLeaf = TreeUtil.findFirstLeaf(element.getNode());
        return firstLeaf != null ? firstLeaf.getPsi() : null;
    }

    private static void computerDirectoriesExist(
        VirtualFile root,
        Collection<IgnoreFileRecord> fileRecords,
        List<? super IgnoreEntryFile> ignoreEntryDirectories
    ) {
        VirtualFileUtil.iterateChildrenRecursively(root, VirtualFile::isDirectory, dir -> {
            ProgressManager.checkCanceled();
            String path = VirtualFileUtil.getRelativePath(dir, root);
            if (path != null) {
                for (IgnoreFileRecord fileRecord : fileRecords) {
                    if (RegexUtil.match(fileRecord.pattern(), path)) {
                        // directory matched, add it to the directory list
                        fileRecords.remove(fileRecord);
                        ignoreEntryDirectories.add(fileRecord.entry());
                        if (fileRecords.isEmpty()) {
                            return false;
                        }
                        break;
                    }
                }
            }
            return true;
        });
    }
}
