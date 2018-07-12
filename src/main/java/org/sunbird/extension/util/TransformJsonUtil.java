package org.sunbird.extension.util;

import com.google.common.collect.HashBiMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Util to transform the structure of a map into another, based on the configuration
 *
 * @author Jaikumar Soundara Rajan
 * @version 1.0
 */
public class TransformJsonUtil {

  private static final Pattern listTypePattern = Pattern.compile("<(.*)>");

  /**
   * All fields available in userInputMap that are configured in config as well, will be transformed
   * according to the configuration and returned as a map
   *
   * @param fieldsConfig Config
   * @param userInputMap Map<String, Object>
   * @param rootConfig String
   * @param enumsConfig Config
   * @param operationMode String
   * @return Map<String, Object>
   */
  public static Map<String, Object> transform(
      Config fieldsConfig,
      Map<String, Object> userInputMap,
      String rootConfig,
      Config enumsConfig,
      String operationMode) {

    Map<String, Object> outputMap = new HashMap<String, Object>();
    Set<String> userInputSet = userInputMap.keySet();
    String prefix = rootConfig + TransformationConstants.DOT;

    for (String key : userInputSet) {
      try {
        Map<String, Object> fieldMap = (Map<String, Object>) fieldsConfig.getAnyRef(prefix + key);
        if (!fieldMap.containsKey(TransformationConstants.FROM_FIELD_NAME)) {
          fieldMap.put(TransformationConstants.FROM_FIELD_NAME, key);
        }
        transformField(fieldMap, userInputMap, outputMap, fieldsConfig, enumsConfig, operationMode);
      } catch (ConfigException e) {
        ProjectLogger.log(
            "TransformJsonUtil:transform : "
                + key
                + " field not found in config file "
                + fieldsConfig.origin(),
            LoggerEnum.INFO.name());
      }
    }

    return outputMap;
  }

  /**
   * Based on fieldMap configuration, value will be retrieved from userInputMap, transformed and set
   * into the outputMap
   *
   * @param fieldMap Map<String, Object>
   * @param userInputMap Map<String, Object>
   * @param outputMap Map<String, Object>
   * @param fieldsConfig Config
   * @param enumsConfig Config
   * @param operationMode String
   * @throws ProjectCommonException
   */
  private static void transformField(
      Map<String, Object> fieldMap,
      Map<String, Object> userInputMap,
      Map<String, Object> outputMap,
      Config fieldsConfig,
      Config enumsConfig,
      String operationMode) {

    Object fieldValue = null;
    String fromField = null;
    List<String> fromFields = null;
    String toField = null;
    String fromType = null;
    String toType = null;

    toField = (String) fieldMap.get(TransformationConstants.TO_FIELD_NAME);
    fromType = (String) fieldMap.get(TransformationConstants.FROM_TYPE);
    toType = (String) fieldMap.get(TransformationConstants.TO_TYPE);
    if (fieldMap.get(TransformationConstants.FROM_FIELD_NAME) instanceof String) {
      fromField = (String) fieldMap.get(TransformationConstants.FROM_FIELD_NAME);
      fieldValue = getValueFromIncomingMap(fromField, userInputMap);
    } else if (fieldMap.get(TransformationConstants.FROM_FIELD_NAME) instanceof List) {
      fromFields = (List<String>) fieldMap.get(TransformationConstants.FROM_FIELD_NAME);
      fieldValue = getValueFromIncomingMap(fromFields, userInputMap);
    }

    if (StringUtils.isBlank(toField)
        || StringUtils.isBlank(fromType)
        || StringUtils.isBlank(toType)) {
      ProjectLogger.log(
          "TransformJsonUtil:transformField : Basic Config Fields(toFieldName, fromType, toType) are missing for field "
              + fromField,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorJsonTransformBasicConfigMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.errorJsonTransformBasicConfigMissing.getErrorMessage(), fromField),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    // Skip the transformation for the field if there is no value to be transformed
    if (skipTransformationForField(fieldValue)) {
      return;
    }

    if (!fromType.equalsIgnoreCase(toType)
        || fieldMap.containsKey(TransformationConstants.ENUM)
        || fromType.contains(TransformationConstants.DATE_STRING_TYPE)) {
      if (isCustomListType(fromField, fromType) || isCustomListType(fromField, toType)) {
        fieldValue =
            getTransformedFieldValueCustomListType(
                fromField, fromType, fieldValue, fieldsConfig, enumsConfig, operationMode);
      } else {
        Map<String, String> enumValues =
            getConfiguredEnumValues(fromField, fieldMap, enumsConfig, operationMode);
        fieldValue =
            getTransformedFieldValue(fromField, fromType, toType, fieldValue, enumValues, fieldMap);
      }
    }

    putValueIntoOutgoingMap(toField, toType, fieldValue, outputMap);
  }

  /**
   * Method to call actual transformation methods based on fromType and toType of the field
   *
   * @param fromField String
   * @param fromType String
   * @param toType String
   * @param fieldValue Object
   * @param enumValues Map<String, String>
   * @param fieldMap Map<String, Object>
   * @return Object
   */
  private static Object getTransformedFieldValue(
      String fromField,
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, String> enumValues,
      Map<String, Object> fieldMap) {

    if (!isListType(fromType) && !isListType(toType)) {
      return getTransformedFieldValueSimpleType(
          fromField, fromType, toType, fieldValue, enumValues, fieldMap);
    }
    if (isListType(fromType) && isListType(toType)) {
      return getTransformedFieldValueListType(
          fromField, fromType, toType, fieldValue, enumValues, fieldMap);
    }
    if (isListType(fromType) && !isListType(toType)) {
      return getTransformedFieldValueListToSimpleType(
          fromField, fromType, toType, fieldValue, enumValues, fieldMap);
    }
    if (!isListType(fromType) && isListType(toType)) {
      return getTransformedFieldValueSimpleToListType(
          fromField, fromType, toType, fieldValue, enumValues, fieldMap);
    }

    return fieldValue;
  }

  /**
   * Method to transform values from simple to list type E.g. String to List<Double>
   *
   * @param fromField String
   * @param fromType String
   * @param toType String
   * @param fieldValue Object
   * @param enumValues Map<String, String>
   * @param fieldMap Map<String, Object>
   * @return Object
   */
  private static Object getTransformedFieldValueSimpleToListType(
      String fromField,
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, String> enumValues,
      Map<String, Object> fieldMap) {
    String toListType = getListType(fromField, toType);
    Object transformedValue =
        getTransformedFieldValueSimpleType(
            fromField, fromType, toListType, fieldValue, enumValues, fieldMap);
    List<Object> toList = new ArrayList<Object>();
    toList.add(transformedValue);
    return toList;
  }

  /**
   * Method to transform values from list to simple type E.g. List<Integer> to Long
   *
   * @param fromField String
   * @param fromType String
   * @param toType String
   * @param fieldValue Object
   * @param enumValues Map<String, String>
   * @param fieldMap Map<String, Object>
   * @return Object
   */
  private static Object getTransformedFieldValueListToSimpleType(
      String fromField,
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, String> enumValues,
      Map<String, Object> fieldMap) {
    Object fromValue = null;
    String fromListType = getListType(fromField, fromType);
    if (isCustomListType(fromField, fromType)) {
      fromValue = filterValue(fromField, fieldMap, fieldValue);
    } else {
      List<Object> fromList = (List<Object>) fieldValue;
      fromValue = fromList.get(0);
    }
    return getTransformedFieldValueSimpleType(
        fromField, fromListType, toType, fromValue, enumValues, fieldMap);
  }

  /**
   * Method to transform values of list type E.g. List<Integer> to List<String>
   *
   * @param fromField String
   * @param fromType String
   * @param toType String
   * @param fieldValue Object
   * @param enumValues Map<String, String>
   * @param fieldMap Map<String, Object>
   * @return Object
   */
  private static Object getTransformedFieldValueListType(
      String fromField,
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, String> enumValues,
      Map<String, Object> fieldMap) {
    String fromListType = getListType(fromField, fromType);
    String toListType = getListType(fromField, toType);
    List<Object> fromList = (List<Object>) fieldValue;
    List<Object> toList = new ArrayList<Object>();
    for (int i = 0; i < fromList.size(); i++) {
      toList.add(
          getTransformedFieldValueSimpleType(
              fromField, fromListType, toListType, fromList.get(i), enumValues, fieldMap));
    }
    return toList;
  }

  /**
   * Method to transform values of custom list type(list of maps) E.g. List<Custom> to List<T>
   *
   * @param fromField String
   * @param fromType String
   * @param fieldValue Object
   * @param fieldsConfig Config
   * @param enumsConfig Config
   * @param operationMode String
   * @return Object
   * @throws ProjectCommonException
   */
  private static Object getTransformedFieldValueCustomListType(
      String fromField,
      String fromType,
      Object fieldValue,
      Config fieldsConfig,
      Config enumsConfig,
      String operationMode) {
    String fromListElementType = getListType(fromField, fromType);
    List<Map<String, Object>> outputList = new ArrayList<Map<String, Object>>();
    if (isCustomListType(fromField, fromType)) {
      List<Map<String, Object>> fromValueList = (List<Map<String, Object>>) fieldValue;
      for (Map<String, Object> inputMapFromList : fromValueList) {
        Map<String, Object> outMap = null;
        outMap =
            transform(
                fieldsConfig, inputMapFromList, fromListElementType, enumsConfig, operationMode);
        outputList.add(outMap);
      }
    } else {
      ProjectLogger.log(
          "TransformJsonUtil:getTransformedFieldValueCustomListType : Invalid Custom ListType Configuration. For Custom List Transformation, both FROM and TO types should be of custom type",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorJsonTransformInvalidTypeConfig.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.errorJsonTransformInvalidTypeConfig.getErrorMessage(), fromField),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return outputList;
  }

  /**
   * Method to transform type of fieldValue from fromType to toType Supported types are String,
   * Integer, Boolean, Double, Long, DateString if toType is of unsupported type, fieldValue will be
   * returned without transformation
   *
   * @param fromField String
   * @param fromType String
   * @param toType String
   * @param fieldValue Object
   * @param enumValues Map<String, String>
   * @param fieldMap Map<String, Object>
   * @return Object
   */
  private static Object getTransformedFieldValueSimpleType(
      String fromField,
      String fromType,
      String toType,
      Object fieldValue,
      Map<String, String> enumValues,
      Map<String, Object> fieldMap) {

    String value = fieldValue.toString();
    value = getEnumValue(fromField, enumValues, value);

    switch (toType) {
      case TransformationConstants.STRING_TYPE:
        return new String(value);
      case TransformationConstants.INTEGER_TYPE:
        return new Integer(value);
      case TransformationConstants.BOOLEAN_TYPE:
        return new Boolean(value);
      case TransformationConstants.DOUBLE_TYPE:
        return new Double(value);
      case TransformationConstants.LONG_TYPE:
        return new Long(value);
      case TransformationConstants.DATE_STRING_TYPE:
        return getTransformedFieldValueDate(fromField, value, fieldMap);
      default:
        return fieldValue;
    }
  }

  /**
   * Method to transform dateformat of date fields based on fromDateFormat and toDateFormat
   * configured
   *
   * @param fromField String
   * @param value String
   * @param fieldMap Map<String, Object>
   * @return String
   * @throws ProjectCommonException
   */
  private static String getTransformedFieldValueDate(
      String fromField, String value, Map<String, Object> fieldMap) {
    String fromDateFormat = (String) fieldMap.get(TransformationConstants.FROM_DATE_FORMAT);
    String toDateFormat = (String) fieldMap.get(TransformationConstants.TO_DATE_FORMAT);
    if (StringUtils.isBlank(fromDateFormat) || StringUtils.isBlank(toDateFormat)) {
      ProjectLogger.log(
          "TransformJsonUtil:getTransformedFieldValueDate : fromDateFormat or toDateFormat configuration is missing",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorJsonTransformInvalidDateFormat.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.errorJsonTransformInvalidDateFormat.getErrorMessage(), fromField),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    SimpleDateFormat sdf1 = new SimpleDateFormat(fromDateFormat);
    SimpleDateFormat sdf2 = new SimpleDateFormat(toDateFormat);
    String transformedValue = null;
    try {
      java.util.Date date = sdf1.parse(value);
      transformedValue = sdf2.format(date);
    } catch (ParseException e) {
      ProjectLogger.log(
          "TransformJsonUtil:getTransformedFieldValueDate : Invalid value for date transformation - "
              + value,
          LoggerEnum.ERROR.name());
      throwErrorJsonTransformInvalidInput(fromField);
    }
    return transformedValue;
  }

  /**
   * Method to determine whether the given type is of CustomList or not returns true, if the given
   * type is of CustomList
   *
   * @param fromField String
   * @param listType String
   * @return boolean
   */
  private static boolean isCustomListType(String fromField, String listType) {
    if (!isListType(listType)) {
      return false;
    }
    String type = getListType(fromField, listType);
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
   * Method to determine whether the given type is of List or not returns true, if the given type is
   * of List
   *
   * @param type String
   * @return boolean
   */
  private static boolean isListType(String type) {
    if (type.contains(TransformationConstants.LIST)) {
      return true;
    }
    return false;
  }

  /**
   * Fromfields should be of String Type This method fetches the value to be processed from the
   * incoming map based on fromFields. Multiple values will be concatenated(separated by space) into
   * a single value.
   *
   * @param fromFields List<String>
   * @param inputMap Map<String, Object>
   * @return Object
   */
  private static Object getValueFromIncomingMap(
      List<String> fromFields, Map<String, Object> inputMap) {

    String value = "";
    for (String fromField : fromFields) {
      String[] fromFieldHierarchy = fromField.split(TransformationConstants.DOT_REGEX);
      Map<String, Object> map = inputMap;
      for (int i = 0; i < fromFieldHierarchy.length - 1; i++) {
        map = (Map<String, Object>) map.get(fromFieldHierarchy[i]);
      }
      value =
          value
              + (String) (map.get(fromFieldHierarchy[fromFieldHierarchy.length - 1]))
              + TransformationConstants.SINGLE_SPACE;
    }
    return value.trim();
  }

  /**
   * Method to fetch the value to be processed from the incoming map based on fromField
   *
   * @param fromField String
   * @param inputMap Map<String, Object>
   * @return Object
   */
  private static Object getValueFromIncomingMap(String fromField, Map<String, Object> inputMap) {

    String[] fromFieldHierarchy = fromField.split(TransformationConstants.DOT_REGEX);
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
  private static void putValueIntoOutgoingMap(
      String toField, String toType, Object value, Map<String, Object> outputMap) {

    String[] tofieldHierarchy = toField.split(TransformationConstants.DOT_REGEX);
    Map<String, Object> map = outputMap;
    for (int i = 0; i < tofieldHierarchy.length - 1; i++) {
      if (!map.containsKey(tofieldHierarchy[i])) {
        map.put(tofieldHierarchy[i], new HashMap<String, Object>());
      }
      map = (Map<String, Object>) map.get(tofieldHierarchy[i]);
    }
    if (isListType(toType) && map.containsKey(tofieldHierarchy[tofieldHierarchy.length - 1])) {
      List<Object> list = (List<Object>) map.get(tofieldHierarchy[tofieldHierarchy.length - 1]);
      list.add(value);
      map.put(tofieldHierarchy[tofieldHierarchy.length - 1], list);
    } else {
      map.put(tofieldHierarchy[tofieldHierarchy.length - 1], value);
    }
  }

  /**
   * Method to fetch equivalent enum value for a given value based on the configuration
   *
   * @param fromField String
   * @param enumValues <String,String>
   * @param inputValue Object
   * @return String
   * @throws ProjectCommonException
   */
  private static String getEnumValue(
      String fromField, Map<String, String> enumValues, String inputValue) {
    if (null == enumValues) {
      return inputValue;
    }
    Set<String> enumKeySet = enumValues.keySet();
    for (String key : enumKeySet) {
      if (key.equalsIgnoreCase(inputValue)) {
        return enumValues.get(key);
      }
    }
    ProjectLogger.log(
        "TransformJsonUtil:getEnumValue : enum value not configured for " + inputValue,
        LoggerEnum.ERROR.name());
    throw new ProjectCommonException(
        ResponseCode.errorJsonTransformInvalidEnumInput.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.errorJsonTransformInvalidEnumInput.getErrorMessage(), fromField),
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  /**
   * Method to fetch listType E.g. Return value will be T, if fieldType is List<T> Throws
   * ProjectCommonException, if listType is not configured
   *
   * @param fromField String
   * @param fieldType String
   * @return String
   * @throws ProjectCommonException
   */
  private static String getListType(String fromField, String fieldType) {
    Matcher matcher = listTypePattern.matcher(fieldType);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      ProjectLogger.log(
          "TransformJsonUtil:getListType : Invalid ListType configuration",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorJsonTransformInvalidTypeConfig.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.errorJsonTransformInvalidTypeConfig.getErrorMessage(), fromField),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  /**
   * Method to fetch configured enum values for a field. Throws ProjectCommonException, if enum key
   * is present and enum values are not configured for a field
   *
   * @param fromField String
   * @param fieldMap Map<String, Object>
   * @param enumsConfig Config
   * @param operationMode String
   * @return Map<String, String>
   * @throws ProjectCommonException
   */
  private static Map<String, String> getConfiguredEnumValues(
      String fromField, Map<String, Object> fieldMap, Config enumsConfig, String operationMode) {
    Object enumName = fieldMap.get(TransformationConstants.ENUM);
    Map<String, String> enumValues = null;
    if (enumName instanceof String) {
      enumValues =
          (Map<String, String>)
              enumsConfig.getAnyRef(
                  TransformationConstants.ENUMS + TransformationConstants.DOT + enumName);
    } else if (enumName instanceof Map) {
      enumValues = (Map<String, String>) enumName;
    }
    if (null != enumName && null == enumValues) {
      ProjectLogger.log(
          "TransformJsonUtil:getConfiguredEnumValues : enum values missing in the configuration for field "
              + (String) fieldMap.get(TransformationConstants.TO_FIELD_NAME),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorJsonTransformEnumValuesEmpty.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.errorJsonTransformEnumValuesEmpty.getErrorMessage(), fromField),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    if (null != enumValues
        && SunbirdExtensionConstants.OPERATION_MODE_READ.equalsIgnoreCase(operationMode)) {
      HashBiMap<String, String> biMap = HashBiMap.create(enumValues);
      enumValues = biMap.inverse();
    }
    return enumValues;
  }

  /**
   * Method to determine whether to skip transformation for a field Returns true, if there's no
   * value to transform
   *
   * @param fieldValue Object
   * @return boolean
   */
  private static boolean skipTransformationForField(Object fieldValue) {
    if (null == fieldValue) {
      return true;
    }
    if (fieldValue instanceof String) {
      if (StringUtils.isBlank((String) fieldValue)) {
        return true;
      }
    }
    if (fieldValue instanceof List) {
      if (((List) fieldValue).size() == 0) {
        return true;
      }
    }
    return false;
  }

  private static Object filterValue(
      String fromField, Map<String, Object> fieldMap, Object fieldValue) {
    List<Map<String, Object>> filters = null;
    String filterField = null;
    Object filteredValue = null;
    try {
      filters = (List<Map<String, Object>>) fieldMap.get(TransformationConstants.FILTERS);
      filterField = (String) fieldMap.get(TransformationConstants.FILTER_FIELD);
    } catch (Exception e) {
      throwErrorJsonTransformInvalidFilterConfig(fromField);
    }
    if (null == filters || filters.isEmpty() || StringUtils.isBlank(filterField)) {
      throwErrorJsonTransformInvalidFilterConfig(fromField);
    }

    List<Map<String, Object>> fromValueList = (List<Map<String, Object>>) fieldValue;
    if (null == fromValueList || fromValueList.isEmpty()) {
      throwErrorJsonTransformInvalidInput(fromField);
    }

    for (Map<String, Object> filter : filters) {
      fromValueList = applyFilter(fromField, filter, fromValueList);
    }
    if (!fromValueList.isEmpty()) {
      filteredValue = fromValueList.get(0).get(filterField);
    }

    return filteredValue;
  }

  private static List<Map<String, Object>> applyFilter(
      String fromField, Map<String, Object> filter, List<Map<String, Object>> fieldValues) {

    List<Map<String, Object>> filteredList = new ArrayList<Map<String, Object>>();

    String configuredField = (String) filter.get(TransformationConstants.FIELD);
    List<String> configuredValues = (List<String>) filter.get(TransformationConstants.VALUES);
    if (null == configuredValues
        || configuredValues.isEmpty()
        || StringUtils.isBlank(configuredField)) {
      throwErrorJsonTransformInvalidFilterConfig(fromField);
    }

    for (Map<String, Object> fieldValue : fieldValues) {
      Object value = fieldValue.get(configuredField);
      if (configuredValues.contains(value)) {
        filteredList.add(fieldValue);
      }
    }

    return filteredList;
  }

  private static void throwErrorJsonTransformInvalidFilterConfig(String fromField) {
    throw new ProjectCommonException(
        ResponseCode.errorJsonTransformInvalidFilterConfig.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.errorJsonTransformInvalidFilterConfig.getErrorMessage(), fromField),
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  private static void throwErrorJsonTransformInvalidInput(String fromField) {
    throw new ProjectCommonException(
        ResponseCode.errorJsonTransformInvalidInput.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.errorJsonTransformInvalidInput.getErrorMessage(), fromField),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }
}
