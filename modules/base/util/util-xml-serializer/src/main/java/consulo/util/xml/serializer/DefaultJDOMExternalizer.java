/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.util.xml.serializer;

import consulo.util.lang.reflect.ReflectionUtil;
import org.jdom.Element;
import org.jdom.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * @author mike
 * @deprecated {@link XmlSerializer} should be used instead
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class DefaultJDOMExternalizer {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultJDOMExternalizer.class);

  private DefaultJDOMExternalizer() {
  }

  public interface JDOMFilter {
    boolean isAccept(@Nonnull Field field);
  }

  public static void writeExternal(@Nonnull Object data, @Nonnull Element parentNode) throws WriteExternalException {
    writeExternal(data, parentNode, null);
  }

  /**
   * @param data
   * @param parentNode
   * @param filter     null means all elements accepted
   * @throws WriteExternalException
   */
  public static void writeExternal(@Nonnull Object data, @Nonnull Element parentNode, @Nullable JDOMFilter filter) throws WriteExternalException {
    Field[] fields = data.getClass().getFields();

    for (Field field : fields) {
      if (field.getName().indexOf('$') >= 0) continue;
      int modifiers = field.getModifiers();
      if ((modifiers & Modifier.PUBLIC) == 0 || (modifiers & Modifier.STATIC) != 0) continue;
      field.setAccessible(true); // class might be non-public
      Class type = field.getType();
      if (filter != null && !filter.isAccept(field)) {
        continue;
      }
      String value = null;
      try {
        if (type.isPrimitive()) {
          if (type.equals(byte.class)) {
            value = Byte.toString(field.getByte(data));
          }
          else if (type.equals(short.class)) {
            value = Short.toString(field.getShort(data));
          }
          else if (type.equals(int.class)) {
            value = Integer.toString(field.getInt(data));
          }
          else if (type.equals(long.class)) {
            value = Long.toString(field.getLong(data));
          }
          else if (type.equals(float.class)) {
            value = Float.toString(field.getFloat(data));
          }
          else if (type.equals(double.class)) {
            value = Double.toString(field.getDouble(data));
          }
          else if (type.equals(char.class)) {
            value = String.valueOf(field.getChar(data));
          }
          else if (type.equals(boolean.class)) {
            value = Boolean.toString(field.getBoolean(data));
          }
          else {
            continue;
          }
        }
        else if (type.equals(String.class)) {
          value = filterXMLCharacters((String)field.get(data));
        }
        else if (type.equals(Color.class)) {
          Color color = (Color)field.get(data);
          if (color != null) {
            value = Integer.toString(color.getRGB() & 0xFFFFFF, 16);
          }
        }
        else if (ReflectionUtil.isAssignable(JDOMExternalizable.class, type)) {
          Element element = new Element("option");
          parentNode.addContent(element);
          element.setAttribute("name", field.getName());
          JDOMExternalizable domValue = (JDOMExternalizable)field.get(data);
          if (domValue != null) {
            Element valueElement = new Element("value");
            element.addContent(valueElement);
            domValue.writeExternal(valueElement);
          }
          continue;
        }
        else {
          LOG.debug("Wrong field type: " + type);
          continue;
        }
      }
      catch (IllegalAccessException e) {
        continue;
      }
      Element element = new Element("option");
      parentNode.addContent(element);
      element.setAttribute("name", field.getName());
      if (value != null) {
        element.setAttribute("value", value);
      }
    }
  }

  @Nullable
  public static String filterXMLCharacters(String value) {
    if (value != null) {
      StringBuilder builder = null;
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (Verifier.isXMLCharacter(c)) {
          if (builder != null) {
            builder.append(c);
          }
        }
        else {
          if (builder == null) {
            builder = new StringBuilder(value.length() + 5);
            builder.append(value, 0, i);
          }
        }
      }
      if (builder != null) {
        value = builder.toString();
      }
    }
    return value;
  }

  public static void readExternal(@Nonnull Object data, Element parentNode) throws InvalidDataException {
    if (parentNode == null) return;

    for (final Element e : parentNode.getChildren("option")) {
      String fieldName = e.getAttributeValue("name");
      if (fieldName == null) {
        throw new InvalidDataException();
      }
      try {
        Field field = data.getClass().getField(fieldName);
        Class type = field.getType();
        int modifiers = field.getModifiers();
        if ((modifiers & Modifier.PUBLIC) == 0 || (modifiers & Modifier.STATIC) != 0) continue;
        field.setAccessible(true); // class might be non-public
        if ((modifiers & Modifier.FINAL) != 0) {
          // read external contents of final field
          Object value = field.get(data);
          if (JDOMExternalizable.class.isInstance(value)) {
            final List children = e.getChildren("value");
            for (Object child : children) {
              Element valueTag = (Element)child;
              ((JDOMExternalizable)value).readExternal(valueTag);
            }
          }
          continue;
        }
        String value = e.getAttributeValue("value");
        if (type.isPrimitive()) {
          if (value != null) {
            if (type.equals(byte.class)) {
              try {
                field.setByte(data, Byte.parseByte(value));
              }
              catch (NumberFormatException ex) {
                throw new InvalidDataException();
              }
            }
            else if (type.equals(short.class)) {
              try {
                field.setShort(data, Short.parseShort(value));
              }
              catch (NumberFormatException ex) {
                throw new InvalidDataException();
              }
            }
            else if (type.equals(int.class)) {
              int i = toInt(value);
              field.setInt(data, i);
            }
            else if (type.equals(long.class)) {
              try {
                field.setLong(data, Long.parseLong(value));
              }
              catch (NumberFormatException ex) {
                throw new InvalidDataException();
              }
            }
            else if (type.equals(float.class)) {
              try {
                field.setFloat(data, Float.parseFloat(value));
              }
              catch (NumberFormatException ex) {
                throw new InvalidDataException();
              }
            }
            else if (type.equals(double.class)) {
              try {
                field.setDouble(data, Double.parseDouble(value));
              }
              catch (NumberFormatException ex) {
                throw new InvalidDataException();
              }
            }
            else if (type.equals(char.class)) {
              if (value.length() != 1) {
                throw new InvalidDataException();
              }
              field.setChar(data, value.charAt(0));
            }
            else if (type.equals(boolean.class)) {
              if (value.equals("true")) {
                field.setBoolean(data, true);
              }
              else if (value.equals("false")) {
                field.setBoolean(data, false);
              }
              else {
                throw new InvalidDataException();
              }
            }
            else {
              throw new InvalidDataException();
            }
          }
        }
        else if (type.equals(String.class)) {
          field.set(data, value);
        }
        else if (type.equals(Color.class)) {
          Color color = toColor(value);
          field.set(data, color);
        }
        else if (ReflectionUtil.isAssignable(JDOMExternalizable.class, type)) {
          final List<Element> children = e.getChildren("value");
          if (!children.isEmpty()) {
            // compatibility with Selena's serialization which writes an empty tag for a bean which has a default value
            JDOMExternalizable object = null;
            for (final Element el : children) {
              object = (JDOMExternalizable)type.newInstance();
              object.readExternal(el);
            }

            field.set(data, object);
          }
        }
        else {
          throw new InvalidDataException("wrong type: " + type);
        }
      }
      catch (NoSuchFieldException ex) {
        LOG.debug(ex.getMessage(), ex);
      }
      catch (SecurityException | InstantiationException ex) {
        throw new InvalidDataException();
      }
      catch (IllegalAccessException ex) {
        throw new InvalidDataException(ex);
      }
    }
  }

  public static int toInt(@Nonnull String value) throws InvalidDataException {
    int i;
    try {
      i = Integer.parseInt(value);
    }
    catch (NumberFormatException ex) {
      throw new InvalidDataException(value, ex);
    }
    return i;
  }

  public static Color toColor(@Nullable String value) throws InvalidDataException {
    Color color;
    if (value == null) {
      color = null;
    }
    else {
      try {
        int rgb = Integer.parseInt(value, 16);
        color = new Color(rgb);
      }
      catch (NumberFormatException ex) {
        LOG.debug("Wrong color value: " + value, ex);
        throw new InvalidDataException("Wrong color value: " + value, ex);
      }
    }
    return color;
  }
}
