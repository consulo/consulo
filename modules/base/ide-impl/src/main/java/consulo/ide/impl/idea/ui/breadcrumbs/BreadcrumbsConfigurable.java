// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.breadcrumbs;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.configurable.CompositeConfigurable;
import consulo.ide.impl.idea.openapi.options.colors.pages.GeneralColorsPage;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.HorizontalLayout;
import consulo.ide.impl.idea.ui.components.panels.VerticalLayout;
import consulo.language.Language;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.UIUtil;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import static consulo.application.ApplicationBundle.message;
import static consulo.ide.impl.idea.application.options.colors.ColorAndFontOptions.selectOrEditColor;
import static consulo.ide.impl.idea.openapi.util.text.StringUtil.naturalCompare;
import static consulo.ide.impl.idea.util.containers.ContainerUtil.newSmartList;
import static javax.swing.SwingConstants.LEFT;

/**
 * @author Sergey.Malenkov
 */
@ExtensionImpl
final class BreadcrumbsConfigurable extends CompositeConfigurable<BreadcrumbsConfigurable.BreadcrumbsProviderConfigurable> implements ApplicationConfigurable, SearchableConfigurable {
  private final HashMap<String, JCheckBox> map = new HashMap<>();
  private JComponent component;
  private JCheckBox show;
  private JRadioButton above;
  private JRadioButton below;
  private JLabel placement;
  private JLabel languages;

  @Inject
  BreadcrumbsConfigurable() {
  }

  @Nonnull
  @Override
  public String getId() {
    return "editor.breadcrumbs";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @Override
  public String getDisplayName() {
    return message("configurable.breadcrumbs");
  }

  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    if (component == null) {
      for (final BreadcrumbsProviderConfigurable configurable : getConfigurables()) {
        final String id = configurable.getId();
        if (!map.containsKey(id)) {
          map.put(id, configurable.createComponent());
        }
      }
      JPanel boxes = new JPanel(new GridLayout(0, 3, JBUIScale.scale(10), 0));
      map.values().stream().sorted((box1, box2) -> naturalCompare(box1.getText(), box2.getText())).forEach(box -> boxes.add(box));

      show = new JCheckBox(message("checkbox.show.breadcrumbs"));
      show.addItemListener(event -> updateEnabled());

      above = new JRadioButton(message("radio.show.breadcrumbs.above"));
      below = new JRadioButton(message("radio.show.breadcrumbs.below"));

      ButtonGroup group = new ButtonGroup();
      group.add(above);
      group.add(below);

      placement = new JLabel(message("label.breadcrumbs.placement"));

      JPanel placementPanel = new JPanel(new HorizontalLayout(JBUIScale.scale(UIUtil.DEFAULT_HGAP)));
      placementPanel.setBorder(JBUI.Borders.emptyLeft(24));
      placementPanel.add(placement);
      placementPanel.add(above);
      placementPanel.add(below);

      languages = new JLabel(message("label.breadcrumbs.languages"));

      JPanel languagesPanel = new JPanel(new VerticalLayout(JBUIScale.scale(6)));
      languagesPanel.setBorder(JBUI.Borders.empty(0, 24, 12, 0));
      languagesPanel.add(languages);
      languagesPanel.add(boxes);

      component = new JPanel(new VerticalLayout(JBUIScale.scale(12), LEFT));
      component.add(show);
      component.add(placementPanel);
      component.add(languagesPanel);
      component.add(LinkLabel.create(message("configure.breadcrumbs.colors"), () -> {
        DataContext context = DataManager.getInstance().getDataContext(component);
        selectOrEditColor(context, "Breadcrumbs//Current", GeneralColorsPage.class);
      }));
    }
    return component;
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
    setBreadcrumbsAbove(settings.isBreadcrumbsAbove());
    setBreadcrumbsShown(settings.isBreadcrumbsShown());
    for (Entry<String, JCheckBox> entry : map.entrySet()) {
      entry.getValue().setSelected(settings.isBreadcrumbsShownFor(entry.getKey()));
    }
    updateEnabled();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
    if (isBreadcrumbsAbove() != settings.isBreadcrumbsAbove()) return true;
    if (isBreadcrumbsShown() != settings.isBreadcrumbsShown()) return true;
    for (Entry<String, JCheckBox> entry : map.entrySet()) {
      if (settings.isBreadcrumbsShownFor(entry.getKey()) != entry.getValue().isSelected()) return true;
    }
    return false;
  }

  @Nonnull
  @Override
  protected List<BreadcrumbsProviderConfigurable> createConfigurables() {
    final List<BreadcrumbsProviderConfigurable> configurables = newSmartList();
    for (final BreadcrumbsProvider provider : BreadcrumbsProvider.EP_NAME.getExtensionList()) {
      for (final Language language : provider.getLanguages()) {
        configurables.add(new BreadcrumbsProviderConfigurable(provider, language));
      }
    }
    return configurables;
  }

  @RequiredUIAccess
  @Override
  public void apply() {
    boolean modified = false;
    EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
    if (settings.setBreadcrumbsAbove(isBreadcrumbsAbove())) modified = true;
    if (settings.setBreadcrumbsShown(isBreadcrumbsShown())) modified = true;
    for (Entry<String, JCheckBox> entry : map.entrySet()) {
      if (settings.setBreadcrumbsShownFor(entry.getKey(), entry.getValue().isSelected())) modified = true;
    }
    if (modified) UISettings.getInstance().fireUISettingsChanged();
  }

  private boolean isBreadcrumbsAbove() {
    return above != null && above.isSelected();
  }

  private void setBreadcrumbsAbove(boolean value) {
    JRadioButton button = value ? above : below;
    if (button != null) button.setSelected(true);
  }

  private boolean isBreadcrumbsShown() {
    return show != null && show.isSelected();
  }

  private void setBreadcrumbsShown(boolean value) {
    if (show != null) show.setSelected(value);
  }

  private void updateEnabled() {
    boolean enabled = isBreadcrumbsShown();
    if (above != null) above.setEnabled(enabled);
    if (below != null) below.setEnabled(enabled);
    if (placement != null) placement.setEnabled(enabled);
    if (languages != null) languages.setEnabled(enabled);
    for (JCheckBox box : map.values()) box.setEnabled(enabled);
  }

  static class BreadcrumbsProviderConfigurable implements SearchableConfigurable {

    private final BreadcrumbsProvider myProvider;
    private final Language myLanguage;

    private BreadcrumbsProviderConfigurable(@Nonnull final BreadcrumbsProvider provider, @Nonnull final Language language) {
      myProvider = provider;
      myLanguage = language;
    }

    @Nullable
    @Override
    public JCheckBox createComponent() {
      return new JCheckBox(myLanguage.getDisplayName());
    }

    @Override
    public boolean isModified() {
      return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    @Nonnull
    @Override
    public String getId() {
      return myLanguage.getID();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
      return myLanguage.getDisplayName();
    }

    @Nonnull
    @Override
    public Class<?> getOriginalClass() {
      return myProvider.getClass();
    }
  }
}
