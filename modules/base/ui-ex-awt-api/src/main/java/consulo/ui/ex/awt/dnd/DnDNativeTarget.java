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
package consulo.ui.ex.awt.dnd;

import consulo.logging.Logger;
import consulo.util.io.StreamUtil;

import jakarta.annotation.Nullable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.HashMap;
import java.util.Map;

public interface DnDNativeTarget extends DnDTarget {

  Logger LOG = Logger.getInstance(DnDNativeTarget.class);

  String EVENT_KEY = "DnDEvent";

  class EventInfo {
    DataFlavor[] myFlavors;
    Transferable myTransferable;

    private final Map<DataFlavor, String> myTexts = new HashMap<DataFlavor, String>();

    public EventInfo(DataFlavor[] flavors, Transferable transferable) {
      myFlavors = flavors;
      myTransferable = transferable;
    }

    public DataFlavor[] getFlavors() {
      return myFlavors;
    }

    @Nullable
    public String getTextForFlavor(DataFlavor flavor) {
      if (myTexts.containsKey(flavor)) {
        return myTexts.get(flavor);
      }

      try {
        String text = StreamUtil.readTextFrom(flavor.getReaderForText(myTransferable));
        myTexts.put(flavor, text);
        return text;
      }
      catch (Exception e) {
        myTexts.put(flavor, null);
        return null;
      }
    }

    public Transferable getTransferable() {
      return myTransferable;
    }
  }

}
