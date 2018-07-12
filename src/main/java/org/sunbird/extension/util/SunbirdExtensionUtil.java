package org.sunbird.extension.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.JsonToJsonLDTransformer;
import io.opensaber.registry.transform.JsonldToJsonTransformer;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;

public class SunbirdExtensionUtil {

  public static Config loadConfig(String fileName) {
    try {
      return ConfigFactory.load(fileName);
    } catch (Exception e) {
      ProjectLogger.log(
          "SunbirdExtensionUtil:loadConfig : Loading of config file "
              + fileName
              + " failed."
              + e.getStackTrace(),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorSunbirdExtensionLoadConfig.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.errorSunbirdExtensionLoadConfig.getErrorMessage(), fileName),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  public static OpensaberClient createOpensaberClient() {
    try {
      ITransformer<String> jsonToJsonldTransformer = JsonToJsonLDTransformer.getInstance();
      ITransformer<String> jsonldToJsonTransformer = JsonldToJsonTransformer.getInstance();
      return OpensaberClient.builder()
          .requestTransformer(jsonToJsonldTransformer)
          .responseTransformer(jsonldToJsonTransformer)
          .build();
    } catch (Exception e) {
      ProjectLogger.log(
          "SunbirdExtensionUtil:createOpensaberClient : Open Saber Client Creation failed ==> "
              + e.getStackTrace(),
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.errorOpenSaberClientCreation.getErrorCode(),
          ResponseCode.errorOpenSaberClientCreation.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }
}
