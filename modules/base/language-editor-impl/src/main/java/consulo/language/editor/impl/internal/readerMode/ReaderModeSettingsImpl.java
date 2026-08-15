// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.readerMode;

import consulo.annotation.component.ServiceImpl;
import consulo.colorScheme.EditorColorsManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.editor.readerMode.ReaderModeProvider.ReaderMode;
import consulo.language.editor.readerMode.ReaderModeSettings;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@State(name = "ReaderModeSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
@Singleton
public class ReaderModeSettingsImpl implements ReaderModeSettings, PersistentStateComponent<ReaderModeSettingsImpl.State> {
    public static class SchemeState {
        public String name = CodeStyleScheme.DEFAULT_SCHEME_NAME;
        public boolean isProjectLevel = false;
    }

    public static class State {
        public SchemeState visualFormattingChosenScheme = new SchemeState();
        public boolean enableVisualFormatting = true;
        public boolean useActiveSchemeForVisualFormatting = true;
        public boolean showLigatures = false;
        public boolean increaseLineSpacing = false;
        public boolean enabled = true;
        public boolean showRenderedDocs = true;
        public boolean showInlayHints = true;
        public boolean showWarnings = false;
        public ReaderMode mode = ReaderMode.LIBRARIES_AND_READ_ONLY;
    }

    private State myState = new State();

    @Override
    public boolean isEnabled() {
        return myState.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        myState.enabled = enabled;
    }

    @Override
    public ReaderMode getMode() {
        return myState.mode;
    }

    @Override
    public void setMode(ReaderMode mode) {
        myState.mode = mode;
    }

    @Override
    public boolean isShowRenderedDocs() {
        return myState.showRenderedDocs;
    }

    @Override
    public void setShowRenderedDocs(boolean showRenderedDocs) {
        myState.showRenderedDocs = showRenderedDocs;
    }

    @Override
    public boolean isShowWarnings() {
        return myState.showWarnings;
    }

    @Override
    public void setShowWarnings(boolean showWarnings) {
        myState.showWarnings = showWarnings;
    }

    @Override
    public boolean isShowLigatures() {
        return myState.showLigatures;
    }

    @Override
    public void setShowLigatures(boolean showLigatures) {
        myState.showLigatures = showLigatures;
    }

    @Override
    public boolean isIncreaseLineSpacing() {
        return myState.increaseLineSpacing;
    }

    @Override
    public void setIncreaseLineSpacing(boolean increaseLineSpacing) {
        myState.increaseLineSpacing = increaseLineSpacing;
    }

    @Override
    public boolean isShowInlaysHints() {
        return myState.showInlayHints;
    }

    @Override
    public void setShowInlaysHints(boolean showInlaysHints) {
        myState.showInlayHints = showInlaysHints;
    }

    @Override
    public boolean isEnableVisualFormatting() {
        return myState.enableVisualFormatting;
    }

    @Override
    public void setEnableVisualFormatting(boolean enableVisualFormatting) {
        myState.enableVisualFormatting = enableVisualFormatting;
    }

    @Override
    public boolean isUseActiveSchemeForVisualFormatting() {
        return myState.useActiveSchemeForVisualFormatting;
    }

    @Override
    public void setUseActiveSchemeForVisualFormatting(boolean useActiveSchemeForVisualFormatting) {
        myState.useActiveSchemeForVisualFormatting = useActiveSchemeForVisualFormatting;
    }

    @Override
    public Scheme getVisualFormattingChosenScheme() {
        SchemeState schemeState = myState.visualFormattingChosenScheme;
        return new Scheme(schemeState.name, schemeState.isProjectLevel);
    }

    @Override
    public void setVisualFormattingChosenScheme(Scheme scheme) {
        SchemeState schemeState = new SchemeState();
        schemeState.name = scheme.name();
        schemeState.isProjectLevel = scheme.isProjectLevel();
        myState.visualFormattingChosenScheme = schemeState;
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
}
