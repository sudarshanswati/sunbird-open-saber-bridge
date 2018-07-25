package org.sunbird.extension.util;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpHeaders;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.responsecode.ResponseCode;

public class OpensaberClientUtil {

  private static ObjectMapper mapper = new ObjectMapper();
  private static OpensaberClient client = createOpensaberClient();

  private static OpensaberClient createOpensaberClient() {
    OpensaberClient osClient = null;
    try {
      ITransformer<String> jsonToJsonldTransformer = JsonToJsonLDTransformer.getInstance();
      ITransformer<String> jsonldToJsonTransformer = JsonldToJsonTransformer.getInstance();
      osClient =
          OpensaberClient.builder()
              .requestTransformer(jsonToJsonldTransformer)
              .responseTransformer(jsonldToJsonTransformer)
              .build();
    } catch (Exception e) {
      ProjectLogger.log(
          "OpensaberClientUtil:createOpensaberClient: Open Saber Client Creation failed = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryClientCreation,
          ResponseCode.errorRegistryClientCreation.getErrorMessage());
    }
    return osClient;
  }

  public static String addEntity(Map<String, Object> requestMap, String accessToken) {

    ResponseData<String> responseData = null;
    try {
      responseData =
          client.addEntity(
              new RequestData<>(mapper.writeValueAsString(requestMap)), getHeader(accessToken));
    } catch (TransformationException | IOException | URISyntaxException e) {
      ProjectLogger.log(
          "OpensaberClientUtil:addEntity: Registry client add entity resulted in exception = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryAddEntity,
          ResponseCode.errorRegistryAddEntity.getErrorMessage());
    }

    Map<String, Object> responseMap = getResponseMap(responseData);
    Map<String, Object> paramsMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.PARAMS);

    String entityId = null;
    if (SunbirdExtensionConstants.STATUS_SUCCESS.equalsIgnoreCase(
        (String) paramsMap.get(SunbirdExtensionConstants.STATUS))) {
      Map<String, Object> resultMap =
          (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.RESULT);
      entityId = (String) resultMap.get(SunbirdExtensionConstants.ENTITY);
    } else {
      String errMsg = (String) paramsMap.get(SunbirdExtensionConstants.ERR_MSG);
      ProjectLogger.log(
          "OpensaberClientUtil:addEntity: Registry client add entity returned failure status = "
              + errMsg,
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryAddEntity,
          ResponseCode.errorRegistryAddEntity.getErrorMessage());
    }
    return entityId;
  }

  public static Map<String, Object> readEntity(String entityId, String accessToken) {

    ResponseData<String> responseData = null;
    try {
      responseData = client.readEntity(new URI(entityId), getHeader(accessToken));
    } catch (TransformationException | IOException | URISyntaxException e) {
      ProjectLogger.log(
          "OpensaberClientUtil:readEntity: Registry client read entity resulted in exception = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryReadEntity,
          ResponseCode.errorRegistryReadEntity.getErrorMessage());
    }

    Map<String, Object> responseMap = getResponseMap(responseData);
    Map<String, Object> paramsMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.PARAMS);

    Map<String, Object> resultMap = null;
    if (SunbirdExtensionConstants.STATUS_SUCCESS.equalsIgnoreCase(
        (String) paramsMap.get(SunbirdExtensionConstants.STATUS))) {
      resultMap = (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.RESULT);
    } else {
      String errMsg = (String) paramsMap.get(SunbirdExtensionConstants.ERR_MSG);
      ProjectLogger.log(
          "OpensaberClientUtil:readEntity: Registry client read entity returned failure status = "
              + errMsg,
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryReadEntity,
          ResponseCode.errorRegistryReadEntity.getErrorMessage());
    }

    return resultMap;
  }

  public static void updateEntity(Map<String, Object> requestMap, String accessToken) {

    ResponseData<String> responseData = null;
    try {
      responseData =
          client.updateEntity(
              new RequestData<>(mapper.writeValueAsString(requestMap)), getHeader(accessToken));
    } catch (TransformationException | IOException | URISyntaxException e) {
      ProjectLogger.log(
          "OpensaberClientUtil:updateEntity: Registry client update entity resulted in exception = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryUpdateEntity,
          ResponseCode.errorRegistryUpdateEntity.getErrorMessage());
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
          "OpensaberClientUtil:updateEntity: Registry client update entity returned failure status = "
              + errMsg,
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryUpdateEntity,
          ResponseCode.errorRegistryUpdateEntity.getErrorMessage());
    }
  }

  public static void deleteEntity(String entityId, String accessToken) {

    ResponseData<String> responseData = null;
    try {
      responseData = client.deleteEntity(new URI(entityId), getHeader(accessToken));
    } catch (IOException | URISyntaxException e) {
      ProjectLogger.log(
          "OpensaberClientUtil:deleteEntity: Registry client delete entity resulted in exception = "
              + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryDeleteEntity,
          ResponseCode.errorRegistryDeleteEntity.getErrorMessage());
    }

    Map<String, Object> responseMap = getResponseMap(responseData);
    Map<String, Object> paramsMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.PARAMS);

    if (SunbirdExtensionConstants.STATUS_SUCCESS.equalsIgnoreCase(
        (String) paramsMap.get(SunbirdExtensionConstants.STATUS))) {
      // do nothing if delete is successful
    } else {
      String errMsg = (String) paramsMap.get(SunbirdExtensionConstants.ERR_MSG);
      ProjectLogger.log(
          "OpensaberClientUtil:deleteEntity: Registry client delete entity returned failure status = "
              + errMsg,
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryDeleteEntity,
          ResponseCode.errorRegistryDeleteEntity.getErrorMessage());
    }
  }

  private static Map<String, String> getHeader(String accessToken) {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    headers.put(HeaderParam.X_Authenticated_User_Token.getName(), accessToken);
    return headers;
  }

  private static Map<String, Object> getResponseMap(ResponseData<String> responseData) {
    Map<String, Object> responseMap = null;
    try {
      responseMap = mapper.readValue(responseData.getResponseData(), HashMap.class);
    } catch (IOException e) {
      ProjectLogger.log(
          "OpensaberClientUtil:getResponseMap: Registry response parse failure = " + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorRegistryParseResponse,
          ResponseCode.errorRegistryParseResponse.getErrorMessage());
    }
    return responseMap;
  }
}
