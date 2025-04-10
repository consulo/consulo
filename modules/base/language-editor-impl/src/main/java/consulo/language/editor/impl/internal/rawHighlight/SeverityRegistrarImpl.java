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

package consulo.language.editor.impl.internal.rawHighlight;

import consulo.colorScheme.TextAttributes;
import consulo.component.messagebus.MessageBus;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeveritiesProvider;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.JDOMExternalizableStringList;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author anna
 * @since 2006-02-24
 */
public class SeverityRegistrarImpl implements JDOMExternalizable, Comparator<HighlightSeverity>, SeverityRegistrar {
    private static final String INFO_TAG = "info";
    private static final String COLOR_ATTRIBUTE = "color";

    private final Map<String, SeverityBasedTextAttributes> myMap = new ConcurrentHashMap<>();
    private final Map<String, ColorValue> myRendererColors = new ConcurrentHashMap<>();
    @Nonnull
    private final MessageBus myMessageBus;

    private volatile ObjectIntMap<HighlightSeverity> myOrderMap;
    private JDOMExternalizableStringList myReadOrder;

    private static final Map<String, HighlightInfoType> STANDARD_SEVERITIES = new ConcurrentHashMap<>();

    public SeverityRegistrarImpl(@Nonnull MessageBus messageBus) {
        myMessageBus = messageBus;
    }

    static {
        registerStandard(HighlightInfoType.ERROR, HighlightSeverity.ERROR);
        registerStandard(HighlightInfoType.WARNING, HighlightSeverity.WARNING);
        registerStandard(HighlightInfoType.INFO, HighlightSeverity.INFO);
        registerStandard(HighlightInfoType.WEAK_WARNING, HighlightSeverity.WEAK_WARNING);
        registerStandard(HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER, HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING);
    }

    public static void registerStandard(@Nonnull HighlightInfoType highlightInfoType, @Nonnull HighlightSeverity highlightSeverity) {
        STANDARD_SEVERITIES.put(highlightSeverity.getName(), highlightInfoType);
    }

    @Nonnull
    public static SeverityRegistrar getSeverityRegistrar(@Nullable Project project) {
        return SeverityRegistrar.getSeverityRegistrar(project);
    }

    public void registerSeverity(@Nonnull SeverityBasedTextAttributes info, @Nullable ColorValue renderColor) {
        HighlightSeverity severity = info.getType().getSeverity(null);
        myMap.put(severity.getName(), info);
        if (renderColor != null) {
            myRendererColors.put(severity.getName(), renderColor);
        }
        myOrderMap = null;
        HighlightDisplayLevel.registerSeverity(severity, getHighlightInfoTypeBySeverity(severity).getAttributesKey(), null);
        severitiesChanged();
    }

    private void severitiesChanged() {
        myMessageBus.syncPublisher(SeverityRegistrarChangeListener.class).severitiesChanged();
    }

    public SeverityBasedTextAttributes unregisterSeverity(@Nonnull HighlightSeverity severity) {
        return myMap.remove(severity.getName());
    }

    @Override
    @Nonnull
    public HighlightInfoType.HighlightInfoTypeImpl getHighlightInfoTypeBySeverity(@Nonnull HighlightSeverity severity) {
        HighlightInfoType infoType = STANDARD_SEVERITIES.get(severity.getName());
        if (infoType != null) {
            return (HighlightInfoType.HighlightInfoTypeImpl)infoType;
        }

        if (severity == HighlightSeverity.INFORMATION) {
            return (HighlightInfoType.HighlightInfoTypeImpl)HighlightInfoType.INFORMATION;
        }

        SeverityBasedTextAttributes type = getAttributesBySeverity(severity);
        return (HighlightInfoType.HighlightInfoTypeImpl)(type == null ? HighlightInfoType.WARNING : type.getType());
    }

    private SeverityBasedTextAttributes getAttributesBySeverity(@Nonnull HighlightSeverity severity) {
        return myMap.get(severity.getName());
    }

    @Override
    @Nullable
    public TextAttributes getTextAttributesBySeverity(@Nonnull HighlightSeverity severity) {
        SeverityBasedTextAttributes infoType = getAttributesBySeverity(severity);
        if (infoType != null) {
            return infoType.getAttributes();
        }
        return null;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        myMap.clear();
        myRendererColors.clear();
        List children = element.getChildren(INFO_TAG);
        for (Object child : children) {
            Element infoElement = (Element)child;

            SeverityBasedTextAttributes highlightInfo = new SeverityBasedTextAttributes(infoElement);

            ColorValue color = null;
            String colorStr = infoElement.getAttributeValue(COLOR_ATTRIBUTE);
            if (colorStr != null) {
                color = RGBColor.fromRGBValue(Integer.parseInt(colorStr, 16));
            }
            registerSeverity(highlightInfo, color);
        }
        myReadOrder = new JDOMExternalizableStringList();
        myReadOrder.readExternal(element);
        List<HighlightSeverity> read = new ArrayList<>(myReadOrder.size());
        List<HighlightSeverity> knownSeverities = getDefaultOrder();
        for (String name : myReadOrder) {
            HighlightSeverity severity = getSeverity(name);
            if (severity == null || !knownSeverities.contains(severity)) {
                continue;
            }
            read.add(severity);
        }
        ObjectIntMap<HighlightSeverity> orderMap = fromList(read);
        if (orderMap.isEmpty()) {
            orderMap = fromList(knownSeverities);
        }
        else {
            //enforce include all known
            List<HighlightSeverity> list = getOrderAsList(orderMap);
            for (int i = 0; i < knownSeverities.size(); i++) {
                HighlightSeverity stdSeverity = knownSeverities.get(i);
                if (!list.contains(stdSeverity)) {
                    for (int oIdx = 0; oIdx < list.size(); oIdx++) {
                        HighlightSeverity orderSeverity = list.get(oIdx);
                        HighlightInfoType type = STANDARD_SEVERITIES.get(orderSeverity.getName());
                        if (type != null && knownSeverities.indexOf(type.getSeverity(null)) > i) {
                            list.add(oIdx, stdSeverity);
                            myReadOrder = null;
                            break;
                        }
                    }
                }
            }
            orderMap = fromList(list);
        }
        myOrderMap = orderMap;
        severitiesChanged();
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        List<HighlightSeverity> list = getOrderAsList(getOrderMap());
        for (HighlightSeverity severity : list) {
            Element info = new Element(INFO_TAG);
            String severityName = severity.getName();
            SeverityBasedTextAttributes infoType = getAttributesBySeverity(severity);
            if (infoType != null) {
                infoType.writeExternal(info);
                ColorValue color = myRendererColors.get(severityName);
                if (color != null) {
                    info.setAttribute(COLOR_ATTRIBUTE, Integer.toString(RGBColor.toRGBValue(color.toRGB()) & 0xFFFFFF, 16));
                }
                element.addContent(info);
            }
        }

        if (myReadOrder != null && !myReadOrder.isEmpty()) {
            myReadOrder.writeExternal(element);
        }
        else if (!getDefaultOrder().equals(list)) {
            JDOMExternalizableStringList ext = new JDOMExternalizableStringList(Collections.nCopies(getOrderMap().size(), ""));
            for (ObjectIntMap.Entry<HighlightSeverity> entry : getOrderMap().entrySet()) {
                HighlightSeverity orderSeverity = entry.getKey();
                int oIdx = entry.getValue();
                ext.set(oIdx, orderSeverity.getName());
            }
            ext.writeExternal(element);
        }
    }

    @Nonnull
    private static List<HighlightSeverity> getOrderAsList(@Nonnull ObjectIntMap<HighlightSeverity> orderMap) {
        List<HighlightSeverity> list = new ArrayList<>();
        for (HighlightSeverity o : orderMap.keySet()) {
            list.add(o);
        }
        Collections.sort(list, (o1, o2) -> compare(o1, o2, orderMap));
        return list;
    }

    @Override
    public int getSeveritiesCount() {
        return createCurrentSeverityNames().size();
    }

    @Override
    @Nullable
    public HighlightSeverity getSeverityByIndex(int i) {
        for (ObjectIntMap.Entry<HighlightSeverity> entry : getOrderMap().entrySet()) {
            if (entry.getValue() == i) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int getSeverityMaxIndex() {
        int[] values = getOrderMap().values().toArray();
        int max = values[0];
        for (int i = 1; i < values.length; ++i) {
            if (values[i] > max) {
                max = values[i];
            }
        }

        return max;
    }

    @Nullable
    public HighlightSeverity getSeverity(@Nonnull String name) {
        HighlightInfoType type = STANDARD_SEVERITIES.get(name);
        if (type != null) {
            return type.getSeverity(null);
        }
        SeverityBasedTextAttributes attributes = myMap.get(name);
        if (attributes != null) {
            return attributes.getSeverity();
        }
        return null;
    }

    @Nonnull
    private List<String> createCurrentSeverityNames() {
        List<String> list = new ArrayList<>();
        list.addAll(STANDARD_SEVERITIES.keySet());
        list.addAll(myMap.keySet());
        ContainerUtil.sort(list);
        return list;
    }

    @Nullable
    public Image getRendererIconByIndex(int i) {
        HighlightSeverity severity = getSeverityByIndex(i);
        HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
        if (level != null) {
            return level.getIcon();
        }

        return HighlightDisplayLevel.createIconByMask(myRendererColors.get(severity.getName()));
    }

    @Override
    public boolean isSeverityValid(@Nonnull String severityName) {
        return createCurrentSeverityNames().contains(severityName);
    }

    @Override
    public int compare(HighlightSeverity s1, HighlightSeverity s2) {
        return compare(s1, s2, getOrderMap());
    }

    private static int compare(HighlightSeverity s1, HighlightSeverity s2, ObjectIntMap<HighlightSeverity> orderMap) {
        int o1 = orderMap.getIntOrDefault(s1, -1);
        int o2 = orderMap.getIntOrDefault(s2, -1);
        return o1 - o2;
    }


    @Nonnull
    private ObjectIntMap<HighlightSeverity> getOrderMap() {
        ObjectIntMap<HighlightSeverity> orderMap;
        ObjectIntMap<HighlightSeverity> defaultOrder = null;
        while ((orderMap = myOrderMap) == null) {
            if (defaultOrder == null) {
                defaultOrder = fromList(getDefaultOrder());
            }
            boolean replaced = ORDER_MAP_UPDATER.compareAndSet(this, null, defaultOrder);
            if (replaced) {
                orderMap = defaultOrder;
                break;
            }
        }
        return orderMap;
    }

    private static final VarHandle ORDER_MAP_UPDATER;

    static {
        try {
            ORDER_MAP_UPDATER = MethodHandles.lookup().findVarHandle(SeverityRegistrarImpl.class, "myOrderMap", ObjectIntMap.class);
        }
        catch (Throwable e) {
            throw new Error(e);
        }
    }

    @Nonnull
    private static ObjectIntMap<HighlightSeverity> fromList(@Nonnull List<HighlightSeverity> orderList) {
        ObjectIntMap<HighlightSeverity> map = ObjectMaps.newObjectIntHashMap(orderList.size());
        for (int i = 0; i < orderList.size(); i++) {
            HighlightSeverity severity = orderList.get(i);
            map.putInt(severity, i);
        }
        return ObjectMaps.unmodified(map);
    }

    @Nonnull
    private List<HighlightSeverity> getDefaultOrder() {
        Collection<SeverityBasedTextAttributes> values = myMap.values();
        List<HighlightSeverity> order = new ArrayList<>(STANDARD_SEVERITIES.size() + values.size());
        for (HighlightInfoType type : STANDARD_SEVERITIES.values()) {
            order.add(type.getSeverity(null));
        }
        for (SeverityBasedTextAttributes attributes : values) {
            order.add(attributes.getSeverity());
        }
        ContainerUtil.sort(order);
        return order;
    }

    public void setOrder(@Nonnull List<HighlightSeverity> orderList) {
        myOrderMap = fromList(orderList);
        myReadOrder = null;
        severitiesChanged();
    }

    public int getSeverityIdx(@Nonnull HighlightSeverity severity) {
        return getOrderMap().getIntOrDefault(severity, -1);
    }

    public boolean isDefaultSeverity(@Nonnull HighlightSeverity severity) {
        return STANDARD_SEVERITIES.containsKey(severity.myName);
    }

    public static boolean isGotoBySeverityEnabled(@Nonnull HighlightSeverity minSeverity) {
        for (SeveritiesProvider provider : SeveritiesProvider.EP_NAME.getExtensionList()) {
            if (provider.isGotoBySeverityEnabled(minSeverity)) {
                return true;
            }
        }
        return minSeverity != HighlightSeverity.INFORMATION;
    }

    public static class SeverityBasedTextAttributes {
        private final TextAttributes myAttributes;
        private final HighlightInfoType.HighlightInfoTypeImpl myType;

        //read external
        public SeverityBasedTextAttributes(@Nonnull Element element) throws InvalidDataException {
            this(new TextAttributes(element), new HighlightInfoType.HighlightInfoTypeImpl(element));
        }

        public SeverityBasedTextAttributes(@Nonnull TextAttributes attributes, @Nonnull HighlightInfoType.HighlightInfoTypeImpl type) {
            myAttributes = attributes;
            myType = type;
        }

        @Nonnull
        public TextAttributes getAttributes() {
            return myAttributes;
        }

        @Nonnull
        public HighlightInfoType.HighlightInfoTypeImpl getType() {
            return myType;
        }

        private void writeExternal(@Nonnull Element element) throws WriteExternalException {
            myAttributes.writeExternal(element);
            myType.writeExternal(element);
        }

        @Nonnull
        public HighlightSeverity getSeverity() {
            return myType.getSeverity(null);
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                || o instanceof SeverityBasedTextAttributes that
                && myAttributes.equals(that.myAttributes)
                && myType.equals(that.myType);
        }

        @Override
        public int hashCode() {
            int result = myAttributes.hashCode();
            result = 31 * result + myType.hashCode();
            return result;
        }
    }

    @Nonnull
    public Collection<SeverityBasedTextAttributes> allRegisteredAttributes() {
        return new ArrayList<>(myMap.values());
    }

    @Nonnull
    public Collection<HighlightInfoType> standardSeverities() {
        return STANDARD_SEVERITIES.values();
    }
}
