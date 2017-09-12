/*
 * Copyright 2011 Sergey Skladchikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.web.gwt.client.ui.advancedGwt;

import com.google.gwt.user.client.ui.*;
import org.gwt.advanced.client.ui.AdvancedWidget;
import org.gwt.advanced.client.ui.widget.LockingPanel;
import org.gwt.advanced.client.ui.widget.theme.ThemeImage;

/**
 * By VISTALL. This is copy-paste version of {@link org.gwt.advanced.client.ui.widget.TextButtonPanel}  with change:
 * -  use {@link ComboBoxSelectItem} as default widget - not text area
 * - now we extends FlexTable - without SimplePanel, simplified tree
 * <p/>
 * <p/>
 * This is a basic class for all text boxs with a button.
 *
 * @author <a href="mailto:sskladchikov@gmail.com">Sergey Skladchikov</a>
 * @see WidgetComboBox
 * @see org.gwt.advanced.client.ui.widget.DatePicker
 * @since 1.2.0
 */
public abstract class WidgetButtonPanel extends FlexTable implements AdvancedWidget, HasEnabled {
  /**
   * a selected value box
   */
  private ComboBoxSelectItem selectedValue;
  /**
   * a choice button
   */
  private ToggleButton choiceButton;
  /**
   * a choice button image
   */
  private Image choiceButtonImage;
  /**
   * a falg meaning whether the widget locked
   */
  private boolean locked;
  /**
   * a locking panel to lock the screen
   */
  private LockingPanel lockingPanel;
  /**
   * choice button visibility flag
   */
  private boolean choiceButtonVisible;

  /**
   * enabled panel controls flag
   */
  private boolean enabled;

  protected WidgetButtonPanel() {
    setCellPadding(0);
    setCellSpacing(0);
    setWidget(0, 0, getSelectedValue());
    setChoiceButtonVisible(true);
    setStyleName("advanced-WidgetButtonPanel");

    addComponentListeners();
  }

  @Override
  protected void onAttach() {
    super.onAttach();

    prepareSelectedValue();
  }

  /**
   * Setter for property 'choiceButtonImage'.
   *
   * @param choiceButtonImage Value to set for property 'choiceButtonImage'.
   */
  public void setChoiceButtonImage(Image choiceButtonImage) {
    this.choiceButtonImage = choiceButtonImage;
  }

  /**
   * Getter for property 'choiceButtonVisible'.
   *
   * @return Value for property 'choiceButtonVisible'.
   */
  public boolean isChoiceButtonVisible() {
    return choiceButtonVisible;
  }

  /**
   * Setter for property 'choiceButtonVisible'.
   *
   * @param choiceButtonVisible Value to set for property 'choiceButtonVisible'.
   */
  public void setChoiceButtonVisible(boolean choiceButtonVisible) {
    if (!choiceButtonVisible && isChoiceButtonVisible()) {
      removeCell(0, 1);
    }
    else if (choiceButtonVisible && !isChoiceButtonVisible()) {
      setWidget(0, 1, getChoiceButton());
      prepareChoiceButton();
    }
    this.choiceButtonVisible = choiceButtonVisible;
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated you don't have to use this method to display the widget any more
   */
  @Override
  public void display() {
  }


  /**
   * Checks whether the controls palced on this panel are enabled.
   *
   * @return a result of check.
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Enables or disables the controls inside the panel.
   *
   * @param enabled is a flag that means whether the controls must be enabled.
   */
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    //getSelectedValue().setEnabled(enabled);
    getChoiceButton().setEnabled(enabled);
  }

  /**
   * Cleans all the data displayed in the widget.
   */
  public void cleanSelection() {
    getSelectedValue().clear();
  }

  /**
   * This method should returns a default button image name.
   *
   * @return an image name.
   */
  protected abstract String getDefaultImageName();

  /**
   * This method adds component listeners.
   */
  protected abstract void addComponentListeners();

  /**
   * Prepares the selected value box for displaying.
   */
  protected void prepareSelectedValue() {
    SimplePanel selectedValue = getSelectedValue();

    //selectedValue.setReadOnly(!isCustomTextAllowed());
    selectedValue.setStyleName("selected-value");

    setHeight("100%");
    getCellFormatter().setHeight(0, 0, "100%");
    getSelectedValue().setHeight("100%");

    setWidth("100%");
    getCellFormatter().setWidth(0, 0, "100%");
    getSelectedValue().setWidth("100%");
  }

  /**
   * Prepares the drop down button for displaying.
   */
  protected void prepareChoiceButton() {
    ToggleButton dropDownButton = getChoiceButton();
    dropDownButton.getUpFace().setImage(getChoiceButtonImage());
    dropDownButton.getDownFace().setImage(getChoiceButtonImage());
    dropDownButton.setStyleName("choice-button");
  }

  /**
   * Getter for property 'selectedValue'.
   *
   * @return Value for property 'selectedValue'.
   */
  protected ComboBoxSelectItem getSelectedValue() {
    if (selectedValue == null) {
      selectedValue = new ComboBoxSelectItem();
    }
    return selectedValue;
  }

  /**
   * Getter for property 'choiceButton'.
   *
   * @return Value for property 'choiceButton'.
   */
  protected ToggleButton getChoiceButton() {
    if (choiceButton == null) choiceButton = new ToggleButton();
    return choiceButton;
  }

  /**
   * Getter for property 'choiceButtonImage'.
   *
   * @return Value for property 'choiceButtonImage'.
   */
  protected Image getChoiceButtonImage() {
    if (choiceButtonImage == null) choiceButtonImage = new ThemeImage(getDefaultImageName());
    return choiceButtonImage;
  }

  /**
   * Getter for property 'locked'.
   *
   * @return Value for property 'locked'.
   */
  public boolean isLocked() {
    return locked;
  }

  /**
   * This method locks the screen.
   */
  public void lock() {
    setLocked(true);
    getLockingPanel().lock();
  }

  /**
   * This method unlocks the screen and redisplays the widget.
   */
  public void unlock() {
    getLockingPanel().unlock();
    setLocked(false);
  }

  /**
   * Setter for property 'locked'.
   *
   * @param locked Value to set for property 'locked'.
   */
  protected void setLocked(boolean locked) {
    this.locked = locked;
  }

  /**
   * Getter for property 'lockingPanel'.
   *
   * @return Value for property 'lockingPanel'.
   */
  protected LockingPanel getLockingPanel() {
    if (lockingPanel == null) lockingPanel = new LockingPanel();
    return lockingPanel;
  }
}
