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
package consulo.components.impl.stores.storage;

import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import consulo.logging.Logger;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.function.BiConsumer;

@SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
final class StateMap {
  private static final Logger LOG = Logger.getInstance(StateMap.class);

  private static final Format XML_FORMAT = Format.getRawFormat().
          setTextMode(Format.TextMode.TRIM).
          setOmitEncoding(true).
          setOmitDeclaration(true);

  private final Map<String, Object> states;

  public StateMap() {
    states = new HashMap<String, Object>();
  }

  public StateMap(StateMap stateMap) {
    states = new HashMap<String, Object>((Map<String, Object>)stateMap.states);
  }

  @Nonnull
  public Set<String> keys() {
    return states.keySet();
  }

  @Nonnull
  public Collection<Object> values() {
    return states.values();
  }

  @Nullable
  public Object get(@Nonnull String key) {
    return states.get(key);
  }

  @Nonnull
  public Element getElement(@Nonnull String key, @Nonnull Map<String, Element> newLiveStates) {
    Object state = states.get(key);
    return stateToElement(key, state, newLiveStates);
  }

  @Nonnull
  static Element stateToElement(@Nonnull String key, @Nullable Object state, @Nonnull Map<String, Element> newLiveStates) {
    if (state instanceof Element) {
      return ((Element)state).clone();
    }
    else {
      Element element = newLiveStates.get(key);
      if (element == null) {
        assert state != null;
        element = unarchiveState((byte[])state);
      }
      return element;
    }
  }

  public void put(@Nonnull String key, @Nonnull Object value) {
    states.put(key, value);
  }

  public boolean isEmpty() {
    return states.isEmpty();
  }

  @Nullable
  public Element getState(@Nonnull String key) {
    Object state = states.get(key);
    return state instanceof Element ? (Element)state : null;
  }

  public boolean hasState(@Nonnull String key) {
    return states.get(key) instanceof Element;
  }

  public boolean hasStates() {
    if (states.isEmpty()) {
      return false;
    }

    for (Object value : states.values()) {
      if (value instanceof Element) {
        return true;
      }
    }
    return false;
  }

  public void compare(@Nonnull String key, @Nonnull StateMap newStates, @Nonnull Set<String> diffs) {
    Object oldState = states.get(key);
    Object newState = newStates.get(key);
    if (oldState instanceof Element) {
      if (!JDOMUtil.areElementsEqual((Element)oldState, (Element)newState)) {
        diffs.add(key);
      }
    }
    else {
      assert newState != null;
      if (getNewByteIfDiffers(key, newState, (byte[])oldState) != null) {
        diffs.add(key);
      }
    }
  }

  @Nullable
  public static byte[] getNewByteIfDiffers(@Nonnull String key, @Nonnull Object newState, @Nonnull byte[] oldState) {
    byte[] newBytes = newState instanceof Element ? archiveState((Element)newState) : (byte[])newState;
    if (Arrays.equals(newBytes, oldState)) {
      return null;
    }
    else if (LOG.isDebugEnabled() && SystemProperties.getBooleanProperty("idea.log.changed.components", false)) {
      String before = stateToString(oldState);
      String after = stateToString(newState);
      if (before.equals(after)) {
        LOG.debug("Serialization error: serialized are different, but unserialized are equal");
      }
      else {
        LOG.debug(key + " " + StringUtil.repeat("=", 80 - key.length()) + "\nBefore:\n" + before + "\nAfter:\n" + after);
      }
    }
    return newBytes;
  }

  @Nonnull
  private static byte[] archiveState(@Nonnull Element state) {
    BufferExposingByteArrayOutputStream byteOut = new BufferExposingByteArrayOutputStream();
    try {
      try (OutputStreamWriter writer = new OutputStreamWriter(new LZ4BlockOutputStream(byteOut), CharsetToolkit.UTF8_CHARSET)) {
        XMLOutputter xmlOutputter = JDOMUtil.newXmlOutputter();
        xmlOutputter.setFormat(XML_FORMAT);
        xmlOutputter.output(state, writer);
      }
    }
    catch (IOException e) {
      throw new StateStorageException(e);
    }
    return ArrayUtil.realloc(byteOut.getInternalBuffer(), byteOut.size());
  }

  @Nullable
  public Element getStateAndArchive(@Nonnull String key) {
    Object state = states.get(key);
    if (!(state instanceof Element)) {
      return null;
    }

    states.put(key, archiveState((Element)state));
    return (Element)state;
  }

  @Nonnull
  public static Element unarchiveState(@Nonnull byte[] state) {
    InputStream in = null;
    try {
      try {
        in = new LZ4BlockInputStream(new UnsyncByteArrayInputStream(state));
        return JDOMUtil.loadDocument(in).detachRootElement();
      }
      finally {
        if (in != null) {
          in.close();
        }
      }
    }
    catch (IOException | JDOMException e) {
      throw new StateStorageException(e);
    }
  }

  @Nonnull
  public static String stateToString(@Nonnull Object state) {
    Element element;
    if (state instanceof Element) {
      element = (Element)state;
    }
    else {
      try {
        element = unarchiveState((byte[])state);
      }
      catch (Throwable e) {
        LOG.error(e);
        return "internal error";
      }
    }
    return JDOMUtil.writeParent(element, "\n");
  }

  @Nullable
  public Object remove(@Nonnull String key) {
    return states.remove(key);
  }

  public int size() {
    return states.size();
  }

  public void forEachEntry(@Nonnull BiConsumer<String, Object> consumer) {
    states.forEach(consumer);
  }
}