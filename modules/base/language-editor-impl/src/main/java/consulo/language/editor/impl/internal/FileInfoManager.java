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
package consulo.language.editor.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.language.editor.FileLookupInfoProvider;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * @author spleaner
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class FileInfoManager implements Disposable {
    private final Map<FileType, FileLookupInfoProvider> myFileType2InfoProvider = new HashMap<>();

    @Inject
    public FileInfoManager(@Nonnull Application application) {
        for (FileLookupInfoProvider provider : FileLookupInfoProvider.EP_NAME.getExtensionList(application)) {
            FileType[] types = provider.getFileTypes();
            for (FileType type : types) {
                myFileType2InfoProvider.put(type, provider);
            }
        }
    }

    public static FileInfoManager getFileInfoManager() {
        return Application.get().getService(FileInfoManager.class);
    }

    @RequiredReadAction
    public static Object getFileLookupItem(PsiElement psiElement) {
        if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
            return psiElement;
        }

        PsiFile file = (PsiFile)psiElement;
        return getFileInfoManager()._getLookupItem(file, file.getName(), IconDescriptorUpdaters.getIcon(file, 0));
    }

    @Nullable
    public static String getFileAdditionalInfo(PsiElement psiElement) {
        return getFileInfoManager()._getInfo(psiElement);
    }

    @Nullable
    private String _getInfo(PsiElement psiElement) {
        if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
            return null;
        }

        PsiFile psiFile = (PsiFile)psiElement;
        FileLookupInfoProvider provider = myFileType2InfoProvider.get(psiFile.getFileType());
        if (provider != null) {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile != null) {
                Pair<String, String> info = provider.getLookupInfo(virtualFile, psiElement.getProject());
                return info == null ? null : info.second;
            }
        }

        return null;
    }

    public static LookupElementBuilder getFileLookupItem(PsiElement psiElement, String encoded, Image icon) {
        if (!(psiElement instanceof PsiFile) || !(psiElement.isPhysical())) {
            return LookupElementBuilder.create(psiElement, encoded).withIcon(icon);
        }

        return getFileInfoManager()._getLookupItem((PsiFile)psiElement, encoded, icon);
    }

    public LookupElementBuilder _getLookupItem(@Nonnull PsiFile file, String name, Image icon) {
        LookupElementBuilder builder = LookupElementBuilder.create(file, name).withIcon(icon);

        String info = _getInfo(file);
        if (info != null) {
            return builder.withTailText(String.format(" (%s)", info), true);
        }

        return builder;
    }

    @Override
    public void dispose() {
        myFileType2InfoProvider.clear();
    }
}
