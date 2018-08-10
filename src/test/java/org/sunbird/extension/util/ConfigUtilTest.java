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

  @Test
  public void testConfigUtilInstanceCreationSuccess() {
    assertNotNull(new ConfigUtil());
  }

  @Test
  public void testLoadConfigSuccess() {
    Config config = ConfigUtil.loadConfig(RESOURCE_PATH + "test-write-user-mapping.conf");
    assertNotNull(config);
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
}
