package org.sunbird.extension.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;

public class ConfigUtil {

  public static Config loadConfig(String fileName) {
    Config config = null;
    ProjectLogger.log(
        "ConfigUtil:loadConfig: Loading Configuration File = " + fileName, LoggerEnum.INFO.name());
    try {
      config = ConfigFactory.parseResources(fileName);
    } catch (Exception e) {
      ProjectLogger.log(
          "ConfigUtil:loadConfig: Loading of config file " + fileName + " failed." + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorLoadConfig, ResponseCode.errorLoadConfig.getErrorMessage());
    }
    return config;
  }

  public static Config loadConfigWithDefaultFallback(String fileName) {
    Config config = null;
    ProjectLogger.log(
        "ConfigUtil:loadConfigWithDefaultFallback: Loading Configuration File = " + fileName,
        LoggerEnum.INFO.name());
    try {
      config = ConfigFactory.load(fileName);
    } catch (Exception e) {
      ProjectLogger.log(
          "ConfigUtil:loadConfigWithDefaultFallback: Loading of config file "
              + fileName
              + " failed."
              + e.getMessage(),
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorLoadConfig, ResponseCode.errorLoadConfig.getErrorMessage());
    }
    return config;
  }

  public static String getConfigOriginFile(Config config) {
    String fileName = config.origin().filename();
    fileName = StringUtils.isBlank(fileName) ? "" : fileName;
    if (fileName.contains("/")) {
      int lastIndex = fileName.lastIndexOf("/");
      fileName = fileName.substring(lastIndex + 1);
    }
    return fileName;
  }
}
