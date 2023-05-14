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

/*
 * User: anna
 * Date: 05-Jan-2007
 */
package consulo.ide.impl.idea.codeInspection.offlineViewer;

import consulo.ide.impl.idea.codeInspection.InspectionApplication;
import consulo.ide.impl.idea.codeInspection.offline.OfflineProblemDescriptor;
import consulo.language.editor.impl.inspection.reference.SmartRefElementPointerImpl;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.logging.Logger;
import gnu.trove.TObjectIntHashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;
import java.util.*;

public class OfflineViewParseUtil {
  private static final Logger LOGGER = Logger.getInstance(OfflineViewParseUtil.class);

  @NonNls
  private static final String PACKAGE = "package";
  @NonNls
  private static final String DESCRIPTION = "description";
  @NonNls
  private static final String HINTS = "hints";
  @NonNls
  private static final String LINE = "line";
  @NonNls
  private static final String MODULE = "module";

  private OfflineViewParseUtil() {
  }

  public static Map<String, Set<OfflineProblemDescriptor>> parse(VirtualFile file) {
    final TObjectIntHashMap<String> fqName2IdxMap = new TObjectIntHashMap<>();
    final Map<String, Set<OfflineProblemDescriptor>> package2Result = new HashMap<>();
    try {
      Element rootElement = JDOMUtil.load(VfsUtil.virtualToIoFile(file));
      for (Element problemElement : rootElement.getChildren()) {
        final OfflineProblemDescriptor descriptor = new OfflineProblemDescriptor();
        boolean added = false;

        for (Element childElement : problemElement.getChildren()) {
          String chilName = childElement.getName();

          switch (chilName) {
            case SmartRefElementPointerImpl.ENTRY_POINT:
              descriptor.setType(childElement.getAttributeValue(SmartRefElementPointerImpl.TYPE_ATTR));
              final String fqName = childElement.getAttributeValue(SmartRefElementPointerImpl.FQNAME_ATTR);
              descriptor.setFQName(fqName);

              if (!fqName2IdxMap.containsKey(fqName)) {
                fqName2IdxMap.put(fqName, 0);
              }
              int idx = fqName2IdxMap.get(fqName);
              descriptor.setProblemIndex(idx);
              fqName2IdxMap.put(fqName, idx + 1);

              final List<String> parentTypes = new ArrayList<>();
              final List<String> parentNames = new ArrayList<>();

              for (Element element : childElement.getChildren()) {
                parentTypes.add(element.getAttributeValue(SmartRefElementPointerImpl.TYPE_ATTR));
                parentNames.add(element.getAttributeValue(SmartRefElementPointerImpl.FQNAME_ATTR));
              }

              if (!parentTypes.isEmpty() && !parentNames.isEmpty()) {
                descriptor.setParentType(ArrayUtil.toStringArray(parentTypes));
                descriptor.setParentFQName(ArrayUtil.toStringArray(parentNames));
              }
              break;
            case DESCRIPTION:
              descriptor.setDescription(childElement.getText());
              break;
            case LINE:
              descriptor.setLine(Integer.parseInt(childElement.getText()));
              break;
            case MODULE:
              descriptor.setModule(childElement.getText());
              break;
            case HINTS:
              for (Element hintElement : childElement.getChildren()) {
                List<String> hints = descriptor.getHints();
                if (hints == null) {
                  hints = new ArrayList<>();
                  descriptor.setHints(hints);
                }
                hints.add(hintElement.getAttributeValue("value"));
              }
              break;
            case PACKAGE:
              appendDescriptor(package2Result, childElement.getText(), descriptor);
              added = true;
              break;
            default:
              for (Element nextElement : childElement.getChildren()) {
                if (PACKAGE.equals(nextElement.getName())) {
                  appendDescriptor(package2Result, nextElement.getText(), descriptor);
                  added = true;
                }
              }
              break;
          }
        }

        if (!added) appendDescriptor(package2Result, "", descriptor);
      }
    }
    catch (Exception e) {
      LOGGER.error(e);
    }
    return package2Result;
  }

  private static void appendDescriptor(final Map<String, Set<OfflineProblemDescriptor>> package2Result, final String packageName, final OfflineProblemDescriptor descriptor) {
    Set<OfflineProblemDescriptor> descriptors = package2Result.get(packageName);
    if (descriptors == null) {
      descriptors = new HashSet<>();
      package2Result.put(packageName, descriptors);
    }
    descriptors.add(descriptor);
  }

  @Nullable
  public static String parseProfileName(VirtualFile virtualFile) {
    try {
      return JDOMUtil.load(VfsUtil.virtualToIoFile(virtualFile)).getAttributeValue(InspectionApplication.PROFILE);
    }
    catch (Exception e) {
      return null;
    }
  }
}