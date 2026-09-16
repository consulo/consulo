// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.encoding;

import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.ProjectManager;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.encoding.EncodingReference;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Everything the application-level encoding manager can answer from its own persisted state or by forwarding to the
 * project that owns a file. Reacting to editor and document changes is not part of it, so this half lives below the
 * document and editor layers and is shared by the production manager and by headless ones.
 */
public abstract class BaseApplicationEncodingManager implements ApplicationEncodingManager {
    private EncodingManagerState myState = new EncodingManagerState();

    public static Set<Charset> widelyKnownCharsets() {
        Set<Charset> result = new HashSet<>();
        result.add(StandardCharsets.UTF_8);
        result.add(CharsetToolkit.getDefaultSystemCharset());
        result.add(CharsetToolkit.getPlatformCharset());
        result.add(StandardCharsets.UTF_16);
        result.add(StandardCharsets.ISO_8859_1);
        result.add(StandardCharsets.US_ASCII);
        result.add(EncodingManager.getInstance().getDefaultCharset());
        result.add(EncodingManager.getInstance().getDefaultCharsetForPropertiesFiles(null));
        result.remove(null);
        return result;
    }

    /**
     * The encoding configuration of {@code project}, or null when the project keeps none.
     */
    protected @Nullable EncodingProjectManager getProjectEncodingManager(Project project) {
        return EncodingProjectManager.getInstance(project);
    }

    private @Nullable EncodingProjectManager getProjectEncodingManager(@Nullable VirtualFile virtualFile) {
        Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        return project == null || project.isDisposed() ? null : getProjectEncodingManager(project);
    }

    public EncodingManagerState getState() {
        return myState;
    }

    public void loadState(EncodingManagerState state) {
        myState = state;
    }

    @Override
    public Collection<Charset> getFavorites() {
        Collection<Charset> result = new HashSet<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            EncodingProjectManager encodingManager = getProjectEncodingManager(project);
            if (encodingManager != null) {
                result.addAll(encodingManager.getFavorites());
            }
        }
        result.addAll(widelyKnownCharsets());
        return result;
    }

    @Override
    public @Nullable Charset getEncoding(@Nullable VirtualFile virtualFile, boolean useParentDefaults) {
        EncodingProjectManager encodingManager = getProjectEncodingManager(virtualFile);
        return encodingManager == null ? null : encodingManager.getEncoding(virtualFile, useParentDefaults);
    }

    @Override
    public void setEncoding(@Nullable VirtualFile virtualFileOrDir, @Nullable Charset charset) {
        EncodingProjectManager encodingManager = getProjectEncodingManager(virtualFileOrDir);
        if (encodingManager != null) {
            encodingManager.setEncoding(virtualFileOrDir, charset);
        }
    }

    @Override
    public boolean isNative2Ascii(VirtualFile virtualFile) {
        EncodingProjectManager encodingManager = getProjectEncodingManager(virtualFile);
        return encodingManager != null && encodingManager.isNative2Ascii(virtualFile);
    }

    @Override
    public boolean isNative2AsciiForPropertiesFiles() {
        EncodingProjectManager encodingManager = getProjectEncodingManager((VirtualFile)null);
        return encodingManager != null && encodingManager.isNative2AsciiForPropertiesFiles();
    }

    @Override
    public void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii) {
        EncodingProjectManager encodingManager = getProjectEncodingManager(virtualFile);
        if (encodingManager != null) {
            encodingManager.setNative2AsciiForPropertiesFiles(virtualFile, native2Ascii);
        }
    }

    @Override
    public @Nullable Charset getDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile) {
        EncodingProjectManager encodingManager = getProjectEncodingManager(virtualFile);
        return encodingManager == null ? null : encodingManager.getDefaultCharsetForPropertiesFiles(virtualFile);
    }

    @Override
    public void setDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile, @Nullable Charset charset) {
        EncodingProjectManager encodingManager = getProjectEncodingManager(virtualFile);
        if (encodingManager != null) {
            encodingManager.setDefaultCharsetForPropertiesFiles(virtualFile, charset);
        }
    }

    @Override
    public Charset getDefaultCharset() {
        return myState.myDefaultEncoding.dereference();
    }

    @Override
    public String getDefaultCharsetName() {
        return myState.getDefaultCharsetName();
    }

    @Override
    public void setDefaultCharsetName(String name) {
        myState.setDefaultCharsetName(name);
    }

    @Override
    public Charset getDefaultConsoleEncoding() {
        return myState.myDefaultConsoleEncoding.dereference();
    }

    @Override
    public EncodingReference getDefaultConsoleEncodingReference() {
        return myState.myDefaultConsoleEncoding;
    }

    @Override
    public void setDefaultConsoleEncodingReference(EncodingReference encodingReference) {
        myState.myDefaultConsoleEncoding = encodingReference;
    }
}
