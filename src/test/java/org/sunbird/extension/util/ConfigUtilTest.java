package org.sunbird.extension.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.typesafe.config.Config;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.responsecode.ResponseCode;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class ConfigUtilTest {

  private static String RESOURCE_PATH = "configUtilTest/";
  private String VALID_TEST_FILE = "valid-file.conf";
  private String INVALID_TEST_FILE = "invalid-file";
  private String FILE_WITH_INVALID_SYNTAX = "file-with-invalid-syntax.conf";
  private String REGISTRY_USER_READ_MAPPING = "registry-user-read-mapping.conf";
  private String REGISTRY_USER_WRITE_MAPPING = "registry-user-write-mapping.conf";
  private String REGISTRY_USER_ENUMS_MAPPING = "registry-user-enums-mapping.conf";

  @Test
  public void testConfigUtilInstanceCreationSuccess() {
    assertNotNull(new ConfigUtil());
  }

  @Test
  public void testLoadConfigSuccessWithValidFile() {
    Config config = ConfigUtil.loadConfig(RESOURCE_PATH + VALID_TEST_FILE);
    assertTrue(config.entrySet().size() == 2);
  }

  @Test
  public void testLoadConfigFailureWithInvalidFile() {
    Config config = ConfigUtil.loadConfig(INVALID_TEST_FILE);
    assertTrue(config.entrySet().size() == 0);
  }

  @Test(expected = ProjectCommonException.class)
  public void testLoadConfigFailureFileWithInvalidSyntax() {
    try {
      ConfigUtil.loadConfig(RESOURCE_PATH + FILE_WITH_INVALID_SYNTAX);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorLoadConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testLoadConfigFailureWithNullValue() {
    try {
      ConfigUtil.loadConfig(null);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorLoadConfig.getErrorCode()));
      throw e;
    }
  }

  @Test
  public void testGetConfigOriginFileSuccessWithValidFileName() {
    Config config = ConfigUtil.loadConfig(RESOURCE_PATH + VALID_TEST_FILE);
    String fileName = ConfigUtil.getConfigOriginFile(config);
    assertTrue(fileName.equals(VALID_TEST_FILE));
  }

  @Test
  public void testGetConfigOriginFileSuccessWithInvalidFileName() {
    Config config = ConfigUtil.loadConfig(RESOURCE_PATH + INVALID_TEST_FILE);
    String fileName = ConfigUtil.getConfigOriginFile(config);
    assertTrue(fileName.equals(""));
  }

  @Test
  public void testGetConfigOriginFileSuccessWithDefaultFallback() {
    Config config = ConfigUtil.loadConfigWithDefaultFallback(RESOURCE_PATH + VALID_TEST_FILE);
    String fileName = ConfigUtil.getConfigOriginFile(config);
    assertTrue(fileName.equals(""));
  }

  @Test
  public void testLoadConfigWithDefaultFallbackSuccessWithValidFile() {
    Config config = ConfigUtil.loadConfigWithDefaultFallback(RESOURCE_PATH + VALID_TEST_FILE);
    assertTrue(config.entrySet().size() > 2);
  }

  @Test
  public void testLoadConfigWithDefaultFallbackFailureWithInvalidFile() {
    Config config = ConfigUtil.loadConfigWithDefaultFallback(INVALID_TEST_FILE);
    assertTrue(config.entrySet().size() > 0);
  }

  @Test(expected = ProjectCommonException.class)
  public void testLoadConfigWithDefaultFallbackFailureFileWithInvalidSyntax() {
    try {
      ConfigUtil.loadConfigWithDefaultFallback(RESOURCE_PATH + FILE_WITH_INVALID_SYNTAX);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorLoadConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testLoadConfigWithDefaultFallbackFailureWithNullValue() {
    try {
      ConfigUtil.loadConfigWithDefaultFallback(null);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorLoadConfig.getErrorCode()));
      throw e;
    }
  }

  @Test
  public void testLoadConfigSuccessWithRegistryUserWriteMapping() {
    Config config = ConfigUtil.loadConfig(REGISTRY_USER_WRITE_MAPPING);
    assertTrue(config.entrySet().size() > 0);
  }

  @Test
  public void testLoadConfigSuccessWithRegistryUserReadMapping() {
    Config config = ConfigUtil.loadConfig(REGISTRY_USER_READ_MAPPING);
    assertTrue(config.entrySet().size() > 0);
  }

  @Test
  public void testLoadConfigSuccessWithRegistryUserEnumsMapping() {
    Config config = ConfigUtil.loadConfig(REGISTRY_USER_ENUMS_MAPPING);
    assertTrue(config.entrySet().size() > 0);
  }
}
