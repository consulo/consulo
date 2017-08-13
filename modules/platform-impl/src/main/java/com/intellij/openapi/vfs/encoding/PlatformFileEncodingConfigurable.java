package com.intellij.openapi.vfs.encoding;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.charset.Charset;

/**
 * Component that adds "IDE Encoding" option to General settings tab. Register it as generalOptionsProvider extension
 * if you want to use it in your product. 
 *
 * @author yole
 */
public class PlatformFileEncodingConfigurable implements SearchableConfigurable {
  private static final String SYSTEM_DEFAULT = IdeBundle.message("encoding.name.system.default");
  private PlatformEncodingOptionsPanel myPanel;

  @Override
  @NotNull
  public String getId() {
    return "GeneralEncodingOptions";
  }

  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "";
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new PlatformEncodingOptionsPanel();
    }
    return myPanel.getMainPanel();
  }

  @Override
  public boolean isModified() {
    final Object item = myPanel.myIDEEncodingComboBox.getSelectedItem();
    if (SYSTEM_DEFAULT.equals(item)) {
      return !StringUtil.isEmpty(EncodingManager.getInstance().getDefaultCharsetName());
    }

    return !Comparing.equal(item, EncodingManager.getInstance().getDefaultCharset());
  }

  @Override
  public void apply() throws ConfigurationException {
    final Object item = myPanel.myIDEEncodingComboBox.getSelectedItem();
    if (SYSTEM_DEFAULT.equals(item)) {
      EncodingManager.getInstance().setDefaultCharsetName("");
    }
    else if (item != null) {
      EncodingManager.getInstance().setDefaultCharsetName(((Charset)item).name());
    }
  }

  @Override
  public void reset() {
    final DefaultComboBoxModel encodingsModel = new DefaultComboBoxModel(CharsetToolkit.getAvailableCharsets());
    encodingsModel.insertElementAt(SYSTEM_DEFAULT, 0);
    myPanel.myIDEEncodingComboBox.setModel(encodingsModel);

    final String name = EncodingManager.getInstance().getDefaultCharsetName();
    if (StringUtil.isEmpty(name)) {
      myPanel.myIDEEncodingComboBox.setSelectedItem(SYSTEM_DEFAULT);
    }
    else {
      myPanel.myIDEEncodingComboBox.setSelectedItem(EncodingManager.getInstance().getDefaultCharset());
    }
  }

  @Override
  public void disposeUIResources() {
    myPanel = null;
  }

  public static class PlatformEncodingOptionsPanel {
    JComboBox myIDEEncodingComboBox;
    private JPanel myMainPanel;

    public JPanel getMainPanel() {
      return myMainPanel;
    }
  }
}
