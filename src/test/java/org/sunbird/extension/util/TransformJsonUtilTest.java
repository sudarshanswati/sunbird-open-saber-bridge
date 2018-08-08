package org.sunbird.extension.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.responsecode.ResponseCode;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class TransformJsonUtilTest {

  private String rootConfig = "user";
  private ObjectMapper mapper = new ObjectMapper();
  private String fieldsConfigFile = "test-write-user-mapping.conf";
  private String enumsConfigFile = "test-write-user-enums-mapping.conf";
  private Config fieldsConfig = ConfigUtil.loadConfig(fieldsConfigFile);
  private Config enumsConfig = ConfigUtil.loadConfig(enumsConfigFile);

  @Test
  public void testTransform() {
    Map preTransformMap = null;
    Map expectedTransformedMap = null;
    try {
      preTransformMap = getJSONFileAsMap("test-write-user-pre-transform.json");
      expectedTransformedMap = getJSONFileAsMap("test-write-user-post-transform.json");
    } catch (IOException e) {
      fail();
    }
    Map transformedMap =
        TransformJsonUtil.transform(
            fieldsConfig,
            preTransformMap,
            rootConfig,
            enumsConfig,
            SunbirdExtensionConstants.OPERATION_MODE_WRITE,
            fieldsConfigFile);
    assertTrue(expectedTransformedMap.equals(transformedMap));
  }

  @Test
  public void testTransformWithBlankFilterValue() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("externalIds", new ArrayList());
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testTransformWithNullFilterValue() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("externalIds", null);
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithBlankToFieldName() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("field1", "value1");
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformBasicConfigMissing.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithBlankFromType() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("field2", "value2");
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformBasicConfigMissing.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithBlankToType() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("field3", "value3");
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformBasicConfigMissing.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithNullFilter() {
    Map preTransformMap = null;
    try {
      preTransformMap = getJSONFileAsMap("test-write-user-with-null-filter.json");
    } catch (IOException e) {
      fail();
    }
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidFilterConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithBlankFilter() {
    Map preTransformMap = null;
    try {
      preTransformMap = getJSONFileAsMap("test-write-user-with-blank-filter.json");
    } catch (IOException e) {
      fail();
    }
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidFilterConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithInvalidFilter() {
    Map preTransformMap = null;
    try {
      preTransformMap = getJSONFileAsMap("test-write-user-with-invalid-filter.json");
    } catch (IOException e) {
      fail();
    }
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidFilterConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithBlankFilterField() {
    Map preTransformMap = null;
    try {
      preTransformMap = getJSONFileAsMap("test-write-user-with-blank-filterField.json");
    } catch (IOException e) {
      fail();
    }
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidFilterConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithBlankFiltersField() {
    Map preTransformMap = null;
    try {
      preTransformMap = getJSONFileAsMap("test-write-user-with-blank-filters-field.json");
    } catch (IOException e) {
      fail();
    }
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidFilterConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithBlankFiltersValues() {
    Map preTransformMap = null;
    try {
      preTransformMap = getJSONFileAsMap("test-write-user-with-blank-filters-values.json");
    } catch (IOException e) {
      fail();
    }
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidFilterConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithNullEnumMap() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("field15", "yes");
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorJsonTransformEnumValuesEmpty.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithInvalidListType() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("field16", "value16");
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidTypeConfig.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithInvalidFromDateFormat() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("field17", "2018-08-08");
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidDateFormat.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testTransformWithInvalidToDateFormat() {
    Map preTransformMap = new HashMap();
    preTransformMap.put("field18", "2018-08-08");
    try {
      TransformJsonUtil.transform(
          fieldsConfig,
          preTransformMap,
          rootConfig,
          enumsConfig,
          SunbirdExtensionConstants.OPERATION_MODE_WRITE,
          fieldsConfigFile);
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidDateFormat.getErrorCode()));
      throw e;
    }
  }

  private Map getJSONFileAsMap(String fileName) throws IOException {
    return mapper.readValue(getFileAsInputStream(fileName), Map.class);
  }

  private InputStream getFileAsInputStream(String fileName) {
    return this.getClass().getClassLoader().getResourceAsStream(fileName);
  }
}
