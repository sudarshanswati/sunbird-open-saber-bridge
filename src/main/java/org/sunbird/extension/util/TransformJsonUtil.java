package org.sunbird.extension.util;

import com.typesafe.config.Config;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransformJsonUtil {

  private Config config;

  public Map<String, Object> transform(
      Config fieldsConfig, Map<String, Object> userInputMap, String rootConfig) {

    Map<String, Object> outputMap = new HashMap<String, Object>();
    config = fieldsConfig;
    Set<String> userInputSet = userInputMap.keySet();
    Iterator<String> userInputIterator = userInputSet.iterator();
    String prefix = rootConfig + TransformationConstants.DOT;
    while (userInputIterator.hasNext()) {
      String key = userInputIterator.next();
      try {
        Map<String, Object> fieldMap = (Map<String, Object>) config.getAnyRef(prefix + key);
        if (!fieldMap.containsKey(TransformationConstants.FROMFIELDNAME)) {
          fieldMap.put(TransformationConstants.FROMFIELDNAME, key);
        }
        transformField(fieldMap, userInputMap, outputMap);
      } catch (Exception e) {
        // handle exception
      }
    }

    return outputMap;
  }

  private void transformField(
      Map<String, Object> fieldMap,
      Map<String, Object> userInputMap,
      Map<String, Object> outputMap) {

    Object fieldValue = null;
    String fromField = null;
    List<String> fromFields = null;
    String toField = null;
    String fromType = null;
    String toType = null;

    toField = (String) fieldMap.get(TransformationConstants.TOFIELDNAME);
    fromType = (String) fieldMap.get(TransformationConstants.FROMTYPE);
    toType = (String) fieldMap.get(TransformationConstants.TOTYPE);
    if (fieldMap.get(TransformationConstants.FROMFIELDNAME) instanceof String) {
      fromField = (String) fieldMap.get(TransformationConstants.FROMFIELDNAME);
      fieldValue = getValueFromIncomingMap(fromField, userInputMap);
    } else if (fieldMap.get(TransformationConstants.FROMFIELDNAME) instanceof List) {
      fromFields = (List<String>) fieldMap.get(TransformationConstants.FROMFIELDNAME);
      fieldValue = getValueFromIncomingMap(fromFields, userInputMap);
    }

    if (!fromType.equalsIgnoreCase(toType) || fieldMap.containsKey(TransformationConstants.ENUM)) {
      Object enumName = fieldMap.get(TransformationConstants.ENUM);
      Map<String, Object> enumValues = null;
      if (enumName instanceof String) {
        enumValues =
            (Map<String, Object>)
                config.getAnyRef(
                    TransformationConstants.ENUMS + TransformationConstants.DOT + enumName);
      } else if (enumName instanceof Map) {
        enumValues = (Map<String, Object>) enumName;
      }
      fieldValue = transformValue(fromType, toType, fieldValue, enumValues, fieldMap);
    }

    putValueIntoOutgoingMap(toField, toType, fieldValue, outputMap);
  }

  private Object transformValue(
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, Object> enumValues,
      Map<String, Object> fieldMap) {

    if (!fromType.contains(TransformationConstants.LIST)
        && !toType.contains(TransformationConstants.LIST)) {
      fieldValue = convertSimpleTypes(fromType, toType, fieldValue, enumValues, fieldMap);
    } else if (fromType.contains(TransformationConstants.LIST)
        && toType.contains(TransformationConstants.LIST)) {
      if (isCustomListType(fromType) || isCustomListType(toType)) {
        fieldValue = convertCustomListTypes(fromType, toType, fieldValue, enumValues);
      } else {
        fieldValue = convertListTypes(fromType, toType, fieldValue, enumValues, fieldMap);
      }
    } else if (fromType.contains(TransformationConstants.LIST)
        && !toType.contains(TransformationConstants.LIST)) {
      fieldValue = convertListtoSimpleType(fromType, toType, fieldValue, enumValues, fieldMap);
    } else if (!fromType.contains(TransformationConstants.LIST)
        && toType.contains(TransformationConstants.LIST)) {
      fieldValue = convertSimpletoListType(fromType, toType, fieldValue, enumValues, fieldMap);
    }

    return fieldValue;
  }

  private Object convertSimpletoListType(
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, Object> enumValues,
      Map<String, Object> fieldMap) {
    String toListType =
        toType.substring(
            toType.indexOf(TransformationConstants.LEFTANGBRACKET) + 1,
            toType.indexOf(TransformationConstants.RIGHTANGBRACKET));
    Object newObj = convertSimpleTypes(fromType, toListType, fieldValue, enumValues, fieldMap);
    List<Object> list = new ArrayList<Object>();
    list.add(newObj);
    return list;
  }

  private Object convertListtoSimpleType(
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, Object> enumValues,
      Map<String, Object> fieldMap) {
    String fromListType =
        fromType.substring(
            fromType.indexOf(TransformationConstants.LEFTANGBRACKET) + 1,
            fromType.indexOf(TransformationConstants.RIGHTANGBRACKET));
    List<Object> list = (List<Object>) fieldValue;
    Object newObj = convertSimpleTypes(fromListType, toType, list.get(0), enumValues, fieldMap);
    return newObj;
  }

  private Object convertListTypes(
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, Object> enumValues,
      Map<String, Object> fieldMap) {
    String fromListType =
        fromType.substring(
            fromType.indexOf(TransformationConstants.LEFTANGBRACKET) + 1,
            fromType.indexOf(TransformationConstants.RIGHTANGBRACKET));
    String toListType =
        toType.substring(
            toType.indexOf(TransformationConstants.LEFTANGBRACKET) + 1,
            toType.indexOf(TransformationConstants.RIGHTANGBRACKET));
    List<Object> currentList = (List<Object>) fieldValue;
    List<Object> newList = new ArrayList<Object>();
    for (int i = 0; i < currentList.size(); i++) {
      newList.add(
          convertSimpleTypes(fromListType, toListType, currentList.get(i), enumValues, fieldMap));
    }
    return newList;
  }

  private Object convertCustomListTypes(
      String fromType, String toType, Object fieldValue, Map enumValues) {
    String customListType =
        fromType.substring(
            fromType.indexOf(TransformationConstants.LEFTANGBRACKET) + 1,
            fromType.indexOf(TransformationConstants.RIGHTANGBRACKET));
    List<Map<String, Object>> outputList = new ArrayList();
    if (isCustomListType(fromType) && isCustomListType(toType)) {
      List<Map> fromValueList = (List<Map>) fieldValue;
      for (Map m : fromValueList) {
        Map<String, Object> outMap = null;
        outMap = transform(config, m, customListType);
        outputList.add(outMap);
      }
    } else if (isCustomListType(fromType) && !isCustomListType(toType)) {
      // no implementation yet
    } else if (!isCustomListType(fromType) && isCustomListType(toType)) {
      // no implementation yet
    }
    return outputList;
  }

  private Object convertSimpleTypes(
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, Object> enumValues,
      Map<String, Object> fieldMap) {

    String value = fieldValue.toString();
    if (null != enumValues) {
      value = getEnumValue(enumValues, value);
    }
    switch (toType) {
      case TransformationConstants.STRINGTYPE:
        return new String(value);
      case TransformationConstants.INTEGERTYPE:
        return new Integer(value);
      case TransformationConstants.BOOLEANTYPE:
        return new Boolean(value);
      case TransformationConstants.DOUBLETYPE:
        return new Double(value);
      case TransformationConstants.LONGTYPE:
        return new Long(value);
      case TransformationConstants.DATESTRINGTYPE:
        String fromDateFormat = (String) fieldMap.get(TransformationConstants.FROMDATEFORMAT);
        String toDateFormat = (String) fieldMap.get(TransformationConstants.TODATEFORMAT);
        SimpleDateFormat sdf1 = new SimpleDateFormat(fromDateFormat);
        SimpleDateFormat sdf2 = new SimpleDateFormat(toDateFormat);
        String newValue = "";
        try {
          java.util.Date date = sdf1.parse(value);
          newValue = sdf2.format(date);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return newValue;
      default:
        return fieldValue;
    }
  }

  private boolean isCustomListType(String listType) {
    String type =
        listType.substring(
            listType.indexOf(TransformationConstants.LEFTANGBRACKET) + 1,
            listType.indexOf(TransformationConstants.RIGHTANGBRACKET));
    boolean isCustomList = true;
    for (SimpleDataTypes sdt : SimpleDataTypes.values()) {
      if (type.equalsIgnoreCase(sdt.name())) {
        isCustomList = false;
        break;
      }
    }
    return isCustomList;
  }

  /**
   * fetches the value to be processed from the incoming map based on fromFields. Multiple values
   * will be concatenated(separated by space) into a single value
   *
   * @param fromFields List<String>
   * @param inputMap Map<String, Object>
   * @return Object
   */
  private Object getValueFromIncomingMap(List<String> fromFields, Map<String, Object> inputMap) {

    String value = "";
    for (String fromField : fromFields) {
      String[] fromFieldHierarchy = fromField.split(TransformationConstants.DOTSEPARATOR);
      Map<String, Object> map = inputMap;
      for (int i = 0; i < fromFieldHierarchy.length - 1; i++) {
        map = (Map<String, Object>) map.get(fromFieldHierarchy[i]);
      }
      value =
          value
              + (String) (map.get(fromFieldHierarchy[fromFieldHierarchy.length - 1]))
              + TransformationConstants.SINGLESPACE;
    }
    return value.trim();
  }

  /**
   * fetches the value to be processed from the incoming map based on fromField
   *
   * @param fromField String
   * @param inputMap Map<String, Object>
   * @return Object
   */
  private Object getValueFromIncomingMap(String fromField, Map<String, Object> inputMap) {

    String[] fromFieldHierarchy = fromField.split(TransformationConstants.DOTSEPARATOR);
    Map<String, Object> map = inputMap;
    for (int i = 0; i < fromFieldHierarchy.length - 1; i++) {
      map = (Map<String, Object>) map.get(fromFieldHierarchy[i]);
    }
    return map.get(fromFieldHierarchy[fromFieldHierarchy.length - 1]);
  }

  /**
   * Iterates through toField tree and sets the value in the appropriate field of outgoing map
   *
   * @param toField String
   * @param toType String
   * @param value Object
   * @param outputMap Map<String, Object>
   */
  public void putValueIntoOutgoingMap(
      String toField, String toType, Object value, Map<String, Object> outputMap) {

    String[] tofieldHierarchy = toField.split(TransformationConstants.DOTSEPARATOR);
    Map<String, Object> map = outputMap;
    for (int i = 0; i < tofieldHierarchy.length - 1; i++) {
      if (!map.containsKey(tofieldHierarchy[i])) {
        map.put(tofieldHierarchy[i], new HashMap<String, Object>());
      }
      map = (Map<String, Object>) map.get(tofieldHierarchy[i]);
    }
    if (toType.contains(TransformationConstants.LIST)
        && map.containsKey(tofieldHierarchy[tofieldHierarchy.length - 1])) {
      List<Object> list = (List<Object>) map.get(tofieldHierarchy[tofieldHierarchy.length - 1]);
      list.add(value);
      map.put(tofieldHierarchy[tofieldHierarchy.length - 1], list);
    } else {
      map.put(tofieldHierarchy[tofieldHierarchy.length - 1], value);
    }
  }

  /**
   * fetches equivalent enum value for a given value based on the configuration
   *
   * @param enumValues <String,Object>
   * @param inputValue Object
   * @return String
   */
  public String getEnumValue(Map<String, Object> enumValues, Object inputValue) {
    return (String) enumValues.get(String.valueOf(inputValue));
  }
}
