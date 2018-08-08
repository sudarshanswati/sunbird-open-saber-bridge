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

  @Test
  public void testLoadConfig() {
    Config config = ConfigUtil.loadConfig("test-write-user-mapping.conf");
    assertNotNull(config);
  }

  @Test(expected = ProjectCommonException.class)
  public void testLoadConfigWithInvalidFile() {
    try {
      ConfigUtil.loadConfig(null);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorLoadConfig.getErrorCode()));
      throw e;
    }
  }
}
