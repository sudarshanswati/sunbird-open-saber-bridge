package org.sunbird.extension.user.impl;

import com.typesafe.config.Config;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.extension.user.UserExtension;
import org.sunbird.extension.util.ConfigUtil;
import org.sunbird.extension.util.OpensaberClientUtil;
import org.sunbird.extension.util.SunbirdExtensionConstants;
import org.sunbird.extension.util.TransformJsonUtil;

/**
 * User profile extension using Open Saber registry for storing adopter specific custom user details
 *
 * @author Jaikumar Soundara Rajan
 */
public class UserProviderRegistryImpl implements UserExtension {

  private static Config userEnumsConfig;
  private static Config userWriteConfig;
  private static Config userReadConfig;
  private static String defaultUserType =
      ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_USER_TYPE);
  private static String userAccessToken = "accessToken";

  static {
    userEnumsConfig = ConfigUtil.loadConfig(SunbirdExtensionConstants.USER_ENUMS_MAPPING_FILE);
    userWriteConfig = ConfigUtil.loadConfig(SunbirdExtensionConstants.USER_WRITE_MAPPING_FILE);
    userReadConfig = ConfigUtil.loadConfig(SunbirdExtensionConstants.USER_READ_MAPPING_FILE);
  }

  @Override
  public void create(Map<String, Object> userProfileMap) {
    setDefaultUserType(userProfileMap);
    setDefaultUserAccessToken(userProfileMap);
    addUser(userProfileMap);
  }

  @Override
  public Map<String, Object> read(Map<String, Object> userIdMap) {
    setDefaultUserAccessToken(userIdMap);
    return readUser(userIdMap);
  }

  @Override
  public void update(Map<String, Object> userProfileMap) {
    setDefaultUserType(userProfileMap);
    setDefaultUserAccessToken(userProfileMap);
    updateUser(userProfileMap);
  }

  @Override
  public void delete(Map<String, Object> userIdMap) {
    setDefaultUserAccessToken(userIdMap);
    deleteUser(userIdMap);
  }

  private void addUser(Map<String, Object> userProfileMap) {
    String accessToken = getAccessToken(userProfileMap);
    Map<String, Object> userMap = getUserMapForWrite(userProfileMap);
    String registryId = OpensaberClientUtil.addEntity(userMap, accessToken);
    userProfileMap.put(JsonKey.REGISTRY_ID, registryId);
  }

  private Map<String, Object> readUser(Map<String, Object> userIdMap) {
    String accessToken = getAccessToken(userIdMap);
    String registryId = getRegistryId(userIdMap);
    Map<String, Object> resultMap = OpensaberClientUtil.readEntity(registryId, accessToken);

    setDefaultUserType(userIdMap);
    String userType = getUserType(userIdMap);
    Map<String, Object> userMap = (Map<String, Object>) resultMap.get(userType);
    userMap =
        TransformJsonUtil.transform(
            userReadConfig,
            userMap,
            userType,
            userEnumsConfig,
            SunbirdExtensionConstants.OPERATION_MODE_READ,
            SunbirdExtensionConstants.USER_READ_MAPPING_FILE);
    return userMap;
  }

  private void updateUser(Map<String, Object> userProfileMap) {
    String accessToken = getAccessToken(userProfileMap);
    Map<String, Object> userMap = getUserMapForWrite(userProfileMap);
    OpensaberClientUtil.updateEntity(userMap, accessToken);
  }

  private void deleteUser(Map<String, Object> userIdMap) {
    String accessToken = getAccessToken(userIdMap);
    String registryId = getRegistryId(userIdMap);
    OpensaberClientUtil.deleteEntity(registryId, accessToken);
  }

  private Map<String, Object> getUserMapForWrite(Map<String, Object> userProfileMap) {
    String userType = getUserType(userProfileMap);
    Map<String, Object> userMap =
        TransformJsonUtil.transform(
            userWriteConfig,
            userProfileMap,
            userType,
            userEnumsConfig,
            SunbirdExtensionConstants.OPERATION_MODE_WRITE,
            SunbirdExtensionConstants.USER_WRITE_MAPPING_FILE);
    return userMap;
  }

  private String getUserType(Map<String, Object> userProfileMap) {
    String userType = (String) userProfileMap.get(SunbirdExtensionConstants.USER_TYPE);
    if (StringUtils.isBlank(userType)) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:getUserType: User type is blank", LoggerEnum.ERROR.name());
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorRegistryEntityTypeBlank,
          ResponseCode.errorRegistryEntityTypeBlank.getErrorMessage());
    }
    return userType;
  }

  private String getAccessToken(Map<String, Object> userProfileMap) {
    String accessToken =
        (String) userProfileMap.get(HeaderParam.X_Authenticated_User_Token.getName());
    if (userProfileMap.containsKey(HeaderParam.X_Authenticated_User_Token.getName())) {
      userProfileMap.remove(HeaderParam.X_Authenticated_User_Token.getName());
    }
    return accessToken;
  }

  private String getRegistryId(Map<String, Object> userIdMap) {
    String registryId = (String) userIdMap.get(JsonKey.REGISTRY_ID);
    if (StringUtils.isBlank(registryId)) {
      ProjectLogger.log(
          "UserProviderRegistryImpl:getRegistryId: registryId is blank", LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryEntityIdBlank,
          ResponseCode.errorRegistryEntityIdBlank.getErrorMessage());
    }
    return registryId;
  }

  private void setDefaultUserType(Map userProfileMap) {
    if (null == userProfileMap.get(SunbirdExtensionConstants.USER_TYPE)) {
      userProfileMap.put(SunbirdExtensionConstants.USER_TYPE, defaultUserType);
    }
  }

  private void setDefaultUserAccessToken(Map userProfileMap) {
    if (null == userProfileMap.get(HeaderParam.X_Authenticated_User_Token.getName())) {
      userProfileMap.put(HeaderParam.X_Authenticated_User_Token.getName(), userAccessToken);
    }
  }
}
