package org.sunbird.extension.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;

public class ConfigUtil {

  public static Config loadConfig(String fileName) {
    Config config = null;
    try {
      config = ConfigFactory.load(fileName);
    } catch (Exception e) {
      ProjectLogger.log(
          "ConfigUtil:loadConfig: Loading of config file " + fileName + " failed." + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorLoadConfig, ResponseCode.errorLoadConfig.getErrorMessage());
    }
    return config;
  }
}
