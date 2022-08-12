// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.ide.IdeBundle;
import consulo.language.LangBundle;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.util.PerFileMappingsEx;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.ui.ex.action.ActionGroup;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.FileDocumentManager;
import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.util.lang.Trinity;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.awt.EnumComboBoxModel;
import consulo.ui.ex.awt.HyperlinkLabel;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ide.impl.idea.util.Consumer;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.ide.impl.idea.util.ui.tree.PerFileConfigurableBase;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

@ExtensionImpl
class FileEncodingConfigurable extends PerFileConfigurableBase<Charset> implements ProjectConfigurable  {
  private JPanel myPanel;
  private JCheckBox myTransparentNativeToAsciiCheckBox;
  private JPanel myPropertiesFilesEncodingCombo;
  private JPanel myTablePanel;
  private ComboBox<EncodingProjectManagerImpl.BOMForNewUTF8Files> myBOMForUTF8Combo;
  private HyperlinkLabel myExplanationLabel;

  private Charset myPropsCharset;
  private final Trinity<String, Supplier<Charset>, Consumer<Charset>> myProjectMapping;
  private final Trinity<String, Supplier<Charset>, Consumer<Charset>> myGlobalMapping;

  @Inject
  FileEncodingConfigurable(@Nonnull Project project) {
    super(project, createMappings(project));
    myBOMForUTF8Combo.setModel(new EnumComboBoxModel<>(EncodingProjectManagerImpl.BOMForNewUTF8Files.class));
    myBOMForUTF8Combo.addItemListener(e -> updateExplanationLabelText());
    myExplanationLabel.setHyperlinkTarget("https://en.wikipedia.org/wiki/Byte_order_mark#UTF-8");
    EncodingManager app = EncodingManager.getInstance();
    EncodingProjectManagerImpl prj = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject);
    myProjectMapping = Trinity.create(IdeBundle.message("file.encoding.option.global.encoding"), () -> app.getDefaultCharsetName().isEmpty() ? null : app.getDefaultCharset(),
                                      o -> app.setDefaultCharsetName(getCharsetName(o)));
    myGlobalMapping = Trinity.create(IdeBundle.message("file.encoding.option.project.encoding"), prj::getConfiguredDefaultCharset, o -> prj.setDefaultCharsetName(getCharsetName(o)));
  }

  @Override
  protected boolean isGlobalMapping(Trinity<String, Supplier<Charset>, Consumer<Charset>> prop) {
    return prop == myGlobalMapping || super.isGlobalMapping(prop);
  }

  @Override
  protected boolean isProjectMapping(Trinity<String, Supplier<Charset>, Consumer<Charset>> prop) {
    return prop == myProjectMapping || super.isProjectMapping(prop);
  }

  private void updateExplanationLabelText() {
    EncodingProjectManagerImpl.BOMForNewUTF8Files item = (EncodingProjectManagerImpl.BOMForNewUTF8Files)myBOMForUTF8Combo.getSelectedItem();
    String I = ApplicationNamesInfo.getInstance().getProductName();
    if (item != null) {
      switch (item) {
        case ALWAYS:
          myExplanationLabel.setHtmlText(IdeBundle.message("file.encoding.option.warning.always", I));
          break;
        case NEVER:
          myExplanationLabel.setHtmlText(IdeBundle.message("file.encoding.option.warning.never", I));
          break;
        case WINDOWS_ONLY:
          myExplanationLabel.setHtmlText(IdeBundle.message("file.encoding.option.warning.windows.only", I));
          break;
      }
    }
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return IdeBundle.message("file.encodings.configurable");
  }

  @Override
  @Nonnull
  public String getId() {
    return "file.encoding";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  protected <S> Object getParameter(@Nonnull Key<S> key) {
    if (key == DESCRIPTION) return IdeBundle.message("encodings.dialog.caption", ApplicationNamesInfo.getInstance().getFullProductName());
    if (key == MAPPING_TITLE) return IdeBundle.message("file.encoding.option.encoding.column");
    if (key == TARGET_TITLE) return IdeBundle.message("file.encoding.option.path.column");
    if (key == OVERRIDE_QUESTION) return null;
    if (key == OVERRIDE_TITLE) return null;
    if (key == EMPTY_TEXT) return IdeBundle.message("file.encodings.not.configured");
    return null;
  }

  @Override
  protected void renderValue(@Nullable Object target, @Nonnull Charset t, @Nonnull ColoredTextContainer renderer) {
    VirtualFile file = target instanceof VirtualFile ? (VirtualFile)target : null;
    EncodingUtil.FailReason result = file == null || file.isDirectory() ? null : EncodingUtil.checkCanConvertAndReload(file);

    String encodingText = t.displayName();
    SimpleTextAttributes attributes = result == null ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;
    renderer.append(encodingText + (result == null ? "" : " (" + EncodingUtil.reasonToString(result, file) + ")"), attributes);
  }

  @Nonnull
  @Override
  protected ActionGroup createActionListGroup(@Nullable Object target, @Nonnull Consumer<? super Charset> onChosen) {
    VirtualFile file = target instanceof VirtualFile ? (VirtualFile)target : null;
    byte[] b = null;
    try {
      b = file == null || file.isDirectory() ? null : file.contentsToByteArray();
    }
    catch (IOException ignored) {
    }
    byte[] bytes = b;
    Document document = file == null ? null : FileDocumentManager.getInstance().getDocument(file);

    return new ChangeFileEncodingAction(true) {
      @Override
      protected boolean chosen(Document document, Editor editor, VirtualFile virtualFile, byte[] bytes, @Nonnull Charset charset) {
        onChosen.consume(charset);
        return true;
      }
    }.createActionGroup(file, null, document, bytes, getClearValueText(target));
  }

  @Override
  @Nullable
  protected String getClearValueText(@Nullable Object target) {
    return target != null ? super.getClearValueText(target) : LangBundle.message("action.set.system.default.encoding.text");
  }

  @Override
  @Nullable
  protected String getNullValueText(@Nullable Object target) {
    return target != null ? super.getNullValueText(target) : IdeBundle.message("encoding.name.system.default", CharsetToolkit.getDefaultSystemCharset().displayName());
  }

  @Nonnull
  @Override
  protected Collection<Charset> getValueVariants(@Nullable Object target) {
    return Arrays.asList(CharsetToolkit.getAvailableCharsets());
  }

  @Nonnull
  @Override
  public JComponent createComponent() {
    myTablePanel.add(super.createComponent(), BorderLayout.CENTER);
    JPanel p = createActionPanel(null, new Value<Charset>() {
      @Override
      public void commit() {
      }

      @Override
      public Charset get() {
        return myPropsCharset;
      }

      @Override
      public void set(Charset value) {
        myPropsCharset = value;
      }
    });
    myPropertiesFilesEncodingCombo.add(p, BorderLayout.CENTER);
    return myPanel;
  }

  @Nonnull
  @Override
  protected List<Trinity<String, Supplier<Charset>, Consumer<Charset>>> getDefaultMappings() {
    return Arrays.asList(myProjectMapping, myGlobalMapping);
  }

  @Override
  protected Charset adjustChosenValue(@Nullable Object target, Charset chosen) {
    return chosen == ChooseFileEncodingAction.NO_ENCODING ? null : chosen;
  }

  @Override
  public boolean isModified() {
    if (super.isModified()) return true;
    EncodingProjectManagerImpl encodingManager = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject);
    boolean same = Comparing.equal(encodingManager.getDefaultCharsetForPropertiesFiles(null), myPropsCharset) &&
                   encodingManager.isNative2AsciiForPropertiesFiles() == myTransparentNativeToAsciiCheckBox.isSelected() &&
                   encodingManager.getBOMForNewUTF8Files() == myBOMForUTF8Combo.getSelectedItem();
    return !same;
  }

  @Nonnull
  private static String getCharsetName(@Nullable Charset c) {
    return c == null ? "" : c.name();
  }

  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    EncodingProjectManagerImpl encodingManager = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject);
    encodingManager.setDefaultCharsetForPropertiesFiles(null, myPropsCharset);
    encodingManager.setNative2AsciiForPropertiesFiles(null, myTransparentNativeToAsciiCheckBox.isSelected());
    EncodingProjectManagerImpl.BOMForNewUTF8Files option =
            ObjectUtils.notNull((EncodingProjectManagerImpl.BOMForNewUTF8Files)myBOMForUTF8Combo.getSelectedItem(), EncodingProjectManagerImpl.BOMForNewUTF8Files.NEVER);
    encodingManager.setBOMForNewUtf8Files(option);
  }

  @Override
  public void reset() {
    EncodingProjectManagerImpl encodingManager = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject);
    myTransparentNativeToAsciiCheckBox.setSelected(encodingManager.isNative2AsciiForPropertiesFiles());
    myPropsCharset = encodingManager.getDefaultCharsetForPropertiesFiles(null);
    myBOMForUTF8Combo.setSelectedItem(encodingManager.getBOMForNewUTF8Files());
    super.reset();
  }

  @Override
  protected boolean canEditTarget(@Nullable Object target, Charset value) {
    return target == null || target instanceof VirtualFile && (((VirtualFile)target).isDirectory() || EncodingUtil.checkCanConvertAndReload((VirtualFile)target) == null);
  }

  @Nonnull
  private static PerFileMappingsEx<Charset> createMappings(@Nonnull Project project) {
    EncodingProjectManagerImpl prjManager = (EncodingProjectManagerImpl)EncodingProjectManager.getInstance(project);
    return new PerFileMappingsEx<Charset>() {
      @Nonnull
      @Override
      public Map<VirtualFile, Charset> getMappings() {
        return new HashMap<>(prjManager.getAllMappings());
      }

      @Override
      public void setMappings(@Nonnull Map<VirtualFile, Charset> mappings) {
        prjManager.setMapping(mappings);
      }

      @Override
      public void setMapping(@Nullable VirtualFile file, Charset value) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Charset getMapping(@Nullable VirtualFile file) {
        throw new UnsupportedOperationException();
      }

      @Nullable
      @Override
      public Charset getDefaultMapping(@Nullable VirtualFile file) {
        return prjManager.getEncoding(file, true);
      }

      @Nullable
      @Override
      public Charset getImmediateMapping(@Nullable VirtualFile file) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
