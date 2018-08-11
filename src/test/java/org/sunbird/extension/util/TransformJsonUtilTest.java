package org.sunbird.extension.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.responsecode.ResponseCode;

@SuppressWarnings({"unchecked", "rawtypes", "serial"})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class TransformJsonUtilTest {

  private static String RESOURCE_PATH = "transformJsonUtilTest/";
  private String rootConfig = "user";
  private Config fieldsConfig = getConfig("test-write-user-mapping.conf");
  private Config enumsConfig = getConfig("test-write-user-enums-mapping.conf");

  @Test
  public void testTransformJsonUtilInstanceCreationSuccess() {
    assertNotNull(new TransformJsonUtil());
  }

  @Test
  public void testTransformSuccess() {
    Map preTransformMap = getJSONFileAsMap("test-write-user-success-pre-transform.json");
    Map expectedTransformedMap = getJSONFileAsMap("test-write-user-success-post-transform.json");
    try {
      Map transformedMap = transformWithWriteMode(preTransformMap);
      assertTrue(expectedTransformedMap.equals(transformedMap));
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testTransformSuccessWithBlankFilterValue() {
    Map preTransformMap =
        new HashMap() {
          {
            put("externalIds", new ArrayList());
          }
        };
    try {
      transformWithWriteMode(preTransformMap);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testTransformSuccessWithNullFilterValue() {
    Map preTransformMap =
        new HashMap() {
          {
            put("externalIds", null);
          }
        };
    try {
      transformWithWriteMode(preTransformMap);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithBlankToFieldName() {
    Map preTransformMap =
        new HashMap() {
          {
            put("field1", "value1");
          }
        };
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformBasicConfigMissing);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithBlankFromType() {
    Map preTransformMap =
        new HashMap() {
          {
            put("field2", "value2");
          }
        };
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformBasicConfigMissing);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithBlankToType() {
    Map preTransformMap =
        new HashMap() {
          {
            put("field3", "value3");
          }
        };
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformBasicConfigMissing);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithNullFilters() {
    Map preTransformMap = getJSONFileAsMap("test-write-user-failure-with-null-filters.json");
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidFilterConfig);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithBlankFilters() {
    Map preTransformMap = getJSONFileAsMap("test-write-user-failure-with-blank-filters.json");
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidFilterConfig);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithInvalidFilters() {
    Map preTransformMap = getJSONFileAsMap("test-write-user-failure-with-invalid-filters.json");
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidFilterConfig);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithBlankFilterField() {
    Map preTransformMap = getJSONFileAsMap("test-write-user-failure-with-blank-filterField.json");
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidFilterConfig);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithBlankFieldInFilters() {
    Map preTransformMap =
        getJSONFileAsMap("test-write-user-failure-with-blank-field-in-filters.json");
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidFilterConfig);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithBlankValuesInFilters() {
    Map preTransformMap =
        getJSONFileAsMap("test-write-user-failure-with-blank-values-in-filters.json");
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidFilterConfig);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithNullEnumMap() {
    Map preTransformMap =
        new HashMap() {
          {
            put("field15", "yes");
          }
        };
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformEnumValuesEmpty);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithInvalidListType() {
    Map preTransformMap =
        new HashMap() {
          {
            put("field16", "value16");
          }
        };
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidTypeConfig);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithInvalidFromDateFormat() {
    Map preTransformMap =
        new HashMap() {
          {
            put("field17", "2018-08-08");
          }
        };
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidDateFormat);
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformFailureWithInvalidToDateFormat() {
    Map preTransformMap =
        new HashMap() {
          {
            put("field18", "2018-08-08");
          }
        };
    transformAndCheckException(preTransformMap, ResponseCode.errorJsonTransformInvalidDateFormat);
  }

  private Map<String, Object> transformWithWriteMode(Map preTransformMap) {
    return TransformJsonUtil.transform(
        fieldsConfig,
        preTransformMap,
        rootConfig,
        enumsConfig,
        SunbirdExtensionConstants.OPERATION_MODE_WRITE);
  }

  private Map<String, Object> transformAndCheckException(
      Map preTransformMap, ResponseCode responseCode) {
    try {
      return transformWithWriteMode(preTransformMap);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(responseCode.getErrorCode()));
      throw e;
    }
  }

  private Map getJSONFileAsMap(String fileName) {
    return TestUtil.getJSONFileAsMap(RESOURCE_PATH + fileName);
  }

  private Config getConfig(String fileName) {
    return ConfigUtil.loadConfig(RESOURCE_PATH + fileName);
  }
}
