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
package consulo.ide.impl.idea.codeInspection.offlineViewer;

import consulo.ide.impl.idea.codeInspection.InspectionApplication;
import consulo.ide.impl.idea.codeInspection.offline.OfflineProblemDescriptor;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.language.editor.impl.inspection.reference.SmartRefElementPointerImpl;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import gnu.trove.TObjectIntHashMap;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.*;

/**
 * @author anna
 * @since 2007-01-05
 */
public class OfflineViewParseUtil {
    private static final Logger LOGGER = Logger.getInstance(OfflineViewParseUtil.class);

    private static final String PACKAGE = "package";
    private static final String DESCRIPTION = "description";
    private static final String HINTS = "hints";
    private static final String LINE = "line";
    private static final String MODULE = "module";

    private OfflineViewParseUtil() {
    }

    public static Map<String, Set<OfflineProblemDescriptor>> parse(VirtualFile file) {
        TObjectIntHashMap<String> fqName2IdxMap = new TObjectIntHashMap<>();
        Map<String, Set<OfflineProblemDescriptor>> package2Result = new HashMap<>();
        try {
            Element rootElement = JDOMUtil.load(VfsUtil.virtualToIoFile(file));
            for (Element problemElement : rootElement.getChildren()) {
                OfflineProblemDescriptor descriptor = new OfflineProblemDescriptor();
                boolean added = false;

                for (Element childElement : problemElement.getChildren()) {
                    String chilName = childElement.getName();

                    switch (chilName) {
                        case SmartRefElementPointerImpl.ENTRY_POINT:
                            descriptor.setType(childElement.getAttributeValue(SmartRefElementPointerImpl.TYPE_ATTR));
                            String fqName = childElement.getAttributeValue(SmartRefElementPointerImpl.FQNAME_ATTR);
                            descriptor.setFQName(fqName);

                            if (!fqName2IdxMap.containsKey(fqName)) {
                                fqName2IdxMap.put(fqName, 0);
                            }
                            int idx = fqName2IdxMap.get(fqName);
                            descriptor.setProblemIndex(idx);
                            fqName2IdxMap.put(fqName, idx + 1);

                            List<String> parentTypes = new ArrayList<>();
                            List<String> parentNames = new ArrayList<>();

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

                if (!added) {
                    appendDescriptor(package2Result, "", descriptor);
                }
            }
        }
        catch (Exception e) {
            LOGGER.error(e);
        }
        return package2Result;
    }

    private static void appendDescriptor(
        Map<String, Set<OfflineProblemDescriptor>> package2Result,
        String packageName,
        OfflineProblemDescriptor descriptor
    ) {
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