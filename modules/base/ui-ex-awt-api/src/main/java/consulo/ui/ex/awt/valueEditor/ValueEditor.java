// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.valueEditor;

import org.jspecify.annotations.Nullable;

public interface ValueEditor<T> {
  /**
   * Update an implementing component with the new value.
   *
   * @param newValue The value to set.
   */
  void setValue(T newValue);

  /**
   * Get a current value from the component if possible.
   *
   * @return The current value or the default one if component doesn't contain valid data.
   */
  T getValue();

  /**
   * Set the default value.
   *
   * @param defaultValue The new default value.
   */
  void setDefaultValue(T defaultValue);

  /**
   * @return The current default value.
   */
  T getDefaultValue();

  /**
   * @return The value name used in validation messages.
   */
  @Nullable String getValueName();

  /**
   * Check if the current component content is valid and throw ConfigurationException if not.
   *
   * @throws ValueValidationException The configuration exception.
   */
  void validateContent() throws ValueValidationException;

  String getValueText();

  void setValueText(String text);

  /**
   * Try parsing the text and convert it to the object of type T. Throw InvalidDataException if parsing fails.
   *
   * @param text The text to parse.
   * @return Parsed data.
   * @throws ValueValidationException if parsing fails.
   */
  T parseValue(@Nullable String text) throws ValueValidationException;

  /**
   * Convert the value to an equivalent text string.
   *
   * @param value The value convert.
   * @return The resulting string (the same value should be returned when the string is converted back with {@link #parseValue} method).
   */
  String valueToString(T value);

  /**
   * Check the the given value is valid. For example, an integer number is within an expected range and so on.
   *
   * @param value The value to check.
   */
  boolean isValid(T value);

  void addListener(Listener<T> editorListener);

  interface Listener<T> {
    void valueChanged(T newValue);
  }
}
