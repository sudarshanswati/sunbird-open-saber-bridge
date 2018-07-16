package org.sunbird.extension.user.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.TransformationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.extension.user.UserExtension;
import org.sunbird.extension.util.SunbirdExtensionConstants;
import org.sunbird.extension.util.SunbirdExtensionUtil;
import org.sunbird.extension.util.TransformJsonUtil;

/**
 * User profile extension using Open Saber registry for storing adopter specific custom user details
 *
 * @author Jaikumar Soundara Rajan
 */
public class UserProviderRegistryImpl implements UserExtension {

  private static ObjectMapper mapper = new ObjectMapper();
  private static OpensaberClient client;
  private static Config userEnumsConfig;
  private static Config userWriteConfig;
  private static Config userReadConfig;

  static {
    userEnumsConfig =
        SunbirdExtensionUtil.loadConfig(SunbirdExtensionConstants.USER_ENUMS_MAPPING_FILE);
    userWriteConfig =
        SunbirdExtensionUtil.loadConfig(SunbirdExtensionConstants.USER_WRITE_MAPPING_FILE);
    userReadConfig =
        SunbirdExtensionUtil.loadConfig(SunbirdExtensionConstants.USER_READ_MAPPING_FILE);
    client = SunbirdExtensionUtil.createOpensaberClient();
  }

  @Override
  public void create(Map<String, Object> userProfileMap) {

    // hardcoded to teacher till userType enhancement is done
    userProfileMap.put(SunbirdExtensionConstants.USER_TYPE, "teacher");
    userProfileMap.put(SunbirdExtensionConstants.X_AUTHENTICATED_USER_TOKEN, "accessToken");

    addUser(userProfileMap);
  }

  @Override
  public Map<String, Object> read(Map<String, Object> userIdMap) {

    // hardcoded to teacher till userType enhancement is done
    userIdMap.put(SunbirdExtensionConstants.USER_TYPE, "teacher");
    userIdMap.put(SunbirdExtensionConstants.X_AUTHENTICATED_USER_TOKEN, "accessToken");

    return readUser(userIdMap);
  }

  @Override
  public void update(Map<String, Object> userProfileMap) {

    // hardcoded to teacher till userType enhancement is done
    userProfileMap.put(SunbirdExtensionConstants.USER_TYPE, "teacher");

    updateUser(userProfileMap);
  }

  @Override
  public void delete(Map<String, Object> userProfileMap) {
    // to be implemented
  }

  private Map<String, String> getHeader(String accessToken) {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.put(SunbirdExtensionConstants.X_AUTHENTICATED_USER_TOKEN, accessToken);
    return headers;
  }

  private void addUser(Map<String, Object> userProfileMap) {

    String accessToken = getAccessToken(userProfileMap);
    Map<String, Object> userMap = getUserMapForWrite(userProfileMap);

    ResponseData<String> responseData = null;
    try {
      responseData =
          client.addEntity(
              new RequestData<>(mapper.writeValueAsString(userMap)), getHeader(accessToken));
    } catch (JsonProcessingException | TransformationException e) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:addUser : User Registry Add Entity failed ==> "
              + e.getStackTrace(),
          LoggerEnum.ERROR.name());
      throwUserRegistryAddEntityException();
    }

    Map<String, Object> responseMap = getResponseMap(responseData);
    Map<String, Object> paramsMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.PARAMS);

    if (SunbirdExtensionConstants.STATUS_SUCCESS.equalsIgnoreCase(
        (String) paramsMap.get(SunbirdExtensionConstants.STATUS))) {
      Map<String, Object> resultMap =
          (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.RESULT);
      userProfileMap.put(
          SunbirdExtensionConstants.REGISTRY_ID, resultMap.get(SunbirdExtensionConstants.ENTITY));
    } else {
      String errMsg = (String) paramsMap.get(SunbirdExtensionConstants.ERR_MSG);
      ProjectLogger.log(
          "UserProviderRegistryImpl:addUser : User Registry Add Entity failed - " + errMsg,
          LoggerEnum.ERROR.name());
      throwUserRegistryAddEntityException();
    }
  }

  private Map<String, Object> readUser(Map<String, Object> userIdMap) {

    String accessToken = getAccessToken(userIdMap);
    ResponseData<String> responseData = null;
    String registryId = getRegistryId(userIdMap);

    try {
      responseData = client.readEntity(new URI(registryId), getHeader(accessToken));
    } catch (TransformationException | URISyntaxException e) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:readUser : User Registry Read Entity failed ==> "
              + e.getStackTrace(),
          LoggerEnum.ERROR.name());
      throwUserRegistryReadEntityException();
    }

    Map<String, Object> responseMap = getResponseMap(responseData);
    Map<String, Object> paramsMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.PARAMS);
    Map<String, Object> userMap = null;

    if (SunbirdExtensionConstants.STATUS_SUCCESS.equalsIgnoreCase(
        (String) paramsMap.get(SunbirdExtensionConstants.STATUS))) {
      Map<String, Object> resultMap =
          (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.RESULT);
      String userType = getUserType(userIdMap);
      userMap = (Map<String, Object>) resultMap.get(userType);
      userMap =
          TransformJsonUtil.transform(
              userReadConfig,
              userMap,
              userType,
              userEnumsConfig,
              SunbirdExtensionConstants.OPERATION_MODE_READ);
    } else {
      String errMsg = (String) paramsMap.get(SunbirdExtensionConstants.ERR_MSG);
      ProjectLogger.log(
          "UserProviderRegistryImpl:readUser : User Registry Read Entity failed - " + errMsg,
          LoggerEnum.ERROR.name());
      throwUserRegistryReadEntityException();
    }

    return userMap;
  }

  private void updateUser(Map<String, Object> userProfileMap) {

    String accessToken = getAccessToken(userProfileMap);
    Map<String, Object> userMap = getUserMapForWrite(userProfileMap);
    ResponseData<String> responseData = null;

    try {
      responseData =
          client.updateEntity(
              new RequestData<>(mapper.writeValueAsString(userMap)), getHeader(accessToken));
    } catch (JsonProcessingException | TransformationException e) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:updateUser : User Registry Update Entity failed ==> "
              + e.getStackTrace(),
          LoggerEnum.ERROR.name());
      throwUserRegistryUpdateEntityException();
    }

    Map<String, Object> responseMap = getResponseMap(responseData);
    Map<String, Object> paramsMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.PARAMS);

    if (SunbirdExtensionConstants.STATUS_SUCCESS.equalsIgnoreCase(
        (String) paramsMap.get(SunbirdExtensionConstants.STATUS))) {
      // do nothing if update is successful
    } else {
      String errMsg = (String) paramsMap.get(SunbirdExtensionConstants.ERR_MSG);
      ProjectLogger.log(
          "UserProviderRegistryImpl:updateUser : User Registry Update Entity failed - " + errMsg,
          LoggerEnum.ERROR.name());
      throwUserRegistryUpdateEntityException();
    }
  }

  private Map<String, Object> getUserMapForWrite(Map<String, Object> userProfileMap) {

    String userType = getUserType(userProfileMap);
    Map<String, Object> userMap =
        TransformJsonUtil.transform(
            userWriteConfig,
            userProfileMap,
            userType,
            userEnumsConfig,
            SunbirdExtensionConstants.OPERATION_MODE_WRITE);
    return userMap;
  }

  private String getUserType(Map<String, Object> userProfileMap) {

    String userType = (String) userProfileMap.get(SunbirdExtensionConstants.USER_TYPE);
    if (StringUtils.isBlank(userType)) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:getUserType : User Registry - UserType is blank",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorUserRegistryUserTypeBlank.getErrorCode(),
          ResponseCode.errorUserRegistryUserTypeBlank.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return userType;
  }

  private Map<String, Object> getResponseMap(ResponseData<String> responseData) {
    try {
      return mapper.readValue(responseData.getResponseData(), HashMap.class);
    } catch (IOException e) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:getResponseMap : User Registry Parse Response failed ==> "
              + e.getStackTrace(),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorUserRegistryParseResponse.getErrorCode(),
          ResponseCode.errorUserRegistryParseResponse.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  private String getRegistryId(Map<String, Object> userIdMap) {
    String registryId = (String) userIdMap.get(SunbirdExtensionConstants.REGISTRY_ID);
    if (StringUtils.isBlank(registryId)) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:getRegistryId : User Registry - RegistryId is blank",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorUserRegistryUniqueIdBlank.getErrorCode(),
          ResponseCode.errorUserRegistryUniqueIdBlank.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return registryId;
  }

  private String getAccessToken(Map<String, Object> userMap) {
    String accessToken = (String) userMap.get(SunbirdExtensionConstants.X_AUTHENTICATED_USER_TOKEN);
    return accessToken;
  }

  private void throwUserRegistryAddEntityException() {
    throw new ProjectCommonException(
        ResponseCode.errorUserRegistryAddEntity.getErrorCode(),
        ResponseCode.errorUserRegistryAddEntity.getErrorMessage(),
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  private void throwUserRegistryReadEntityException() {
    throw new ProjectCommonException(
        ResponseCode.errorUserRegistryReadEntity.getErrorCode(),
        ResponseCode.errorUserRegistryReadEntity.getErrorMessage(),
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  private void throwUserRegistryUpdateEntityException() {
    throw new ProjectCommonException(
        ResponseCode.errorUserRegistryUpdateEntity.getErrorCode(),
        ResponseCode.errorUserRegistryUpdateEntity.getErrorMessage(),
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  @SuppressWarnings("unused")
  private void throwUserRegistryDeleteEntityException() {
    throw new ProjectCommonException(
        ResponseCode.errorUserRegistryDeleteEntity.getErrorCode(),
        ResponseCode.errorUserRegistryDeleteEntity.getErrorMessage(),
        ResponseCode.SERVER_ERROR.getResponseCode());
  }
}
