package org.sunbird.extension.user.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.TransformationException;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.JsonToJsonLDTransformer;
import io.opensaber.registry.transform.JsonldToJsonTransformer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
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
    try {
      userEnumsConfig = ConfigFactory.load(SunbirdExtensionConstants.USER_ENUMS_MAPPING_FILE);
      userWriteConfig = ConfigFactory.load(SunbirdExtensionConstants.USER_WRITE_MAPPING_FILE);
      userReadConfig = ConfigFactory.load(SunbirdExtensionConstants.USER_READ_MAPPING_FILE);
    } catch (Exception e) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:static : Loading of configurations for User Registry failed ==> "
              + e.getStackTrace(),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.userRegistryLoadConfigFailed.getErrorCode(),
          ResponseCode.userRegistryLoadConfigFailed.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    try {
      ITransformer<String> jsonToJsonldTransformer = JsonToJsonLDTransformer.getInstance();
      ITransformer<String> jsonldToJsonTransformer = JsonldToJsonTransformer.getInstance();
      client =
          OpensaberClient.builder()
              .requestTransformer(jsonToJsonldTransformer)
              .responseTransformer(jsonldToJsonTransformer)
              .build();
    } catch (Exception e) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:static : User Registry Client Creation failed ==> "
              + e.getStackTrace(),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.userRegistryClientCreationFailed.getErrorCode(),
          ResponseCode.userRegistryClientCreationFailed.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
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
    userProfileMap.put(SunbirdExtensionConstants.X_AUTHENTICATED_USER_TOKEN, "accessToken");

    updateUser(userProfileMap);
  }

  @Override
  public void delete(Map<String, Object> userProfileMap) {
    // TODO Auto-generated method stub

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
      throw new ProjectCommonException(
          ResponseCode.userRegistryAddEntityFailed.getErrorCode(),
          ResponseCode.userRegistryAddEntityFailed.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    Map<String, Object> responseMap = getResponseMap(responseData);

    Map<String, Object> resultMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.RESULT);

    userProfileMap.put(
        SunbirdExtensionConstants.REGISTRY_ID, resultMap.get(SunbirdExtensionConstants.ENTITY));
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
      throw new ProjectCommonException(
          ResponseCode.userRegistryReadEntityFailed.getErrorCode(),
          ResponseCode.userRegistryReadEntityFailed.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    Map<String, Object> responseMap = getResponseMap(responseData);

    Map<String, Object> resultMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.RESULT);
    String userType = getUserType(userIdMap);
    Map<String, Object> userMap = (Map<String, Object>) resultMap.get(userType);
    userMap.remove(SunbirdExtensionConstants.ID);

    userMap =
        TransformJsonUtil.transform(
            userReadConfig,
            userMap,
            userType,
            userEnumsConfig,
            SunbirdExtensionConstants.OPERATION_MODE_READ);

    return userMap;
  }

  private void updateUser(Map<String, Object> userProfileMap) {

    String accessToken = getAccessToken(userProfileMap);
    Map<String, Object> userMap = getUserMapForWrite(userProfileMap);
    setIdforUpdate(userMap);
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
      throw new ProjectCommonException(
          ResponseCode.userRegistryUpdateEntityFailed.getErrorCode(),
          ResponseCode.userRegistryUpdateEntityFailed.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }

    Map<String, Object> responseMap = getResponseMap(responseData);
  }

  private void setIdforUpdate(Map<String, Object> userMap) {
    String registryId = getRegistryId(userMap);
    userMap.put(SunbirdExtensionConstants.ID, registryId);
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
    setMainProviderId(userMap);
    return userMap;
  }

  private String getUserType(Map<String, Object> userProfileMap) {

    String userType = (String) userProfileMap.get(SunbirdExtensionConstants.USER_TYPE);
    if (StringUtils.isBlank(userType)) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:getUserType : User Registry - UserType is blank",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.userRegistryUserTypeBlank.getErrorCode(),
          ResponseCode.userRegistryUserTypeBlank.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return userType;
  }

  private void setMainProviderId(Map<String, Object> userMap) {

    String mainProvider =
        userWriteConfig.getString(SunbirdExtensionConstants.SUNBIRD_MAIN_PROVIDER);
    if (StringUtils.isBlank(mainProvider)) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:setMainProviderId : User Registry - Main Provider is not configured",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.userRegistryMainProviderNotConfigured.getErrorCode(),
          ResponseCode.userRegistryMainProviderNotConfigured.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    List<Map> externalIds = (List<Map>) userMap.get(SunbirdExtensionConstants.EXTERNAL_IDS);
    if (null != externalIds) {
      for (Map externalIdDetails : externalIds) {
        if (mainProvider.equalsIgnoreCase(
            (String) externalIdDetails.get(SunbirdExtensionConstants.PROVIDER))) {
          userMap.put(
              SunbirdExtensionConstants.USER_ID,
              externalIdDetails.get(SunbirdExtensionConstants.ID));
        }
      }
    }
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
          ResponseCode.userRegistryParseResponseFailed.getErrorCode(),
          ResponseCode.userRegistryParseResponseFailed.getErrorMessage(),
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
          ResponseCode.userRegistryUniqueIdBlank.getErrorCode(),
          ResponseCode.userRegistryUniqueIdBlank.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return registryId;
  }

  private String getAccessToken(Map<String, Object> userMap) {
    String accessToken = (String) userMap.get(SunbirdExtensionConstants.X_AUTHENTICATED_USER_TOKEN);
    if (StringUtils.isBlank(accessToken)) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:getAccessToken : User Registry - User Access Token is blank",
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.userRegistryAccessTokenBlank.getErrorCode(),
          ResponseCode.userRegistryAccessTokenBlank.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return accessToken;
  }
}
