package org.sunbird.extension.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.TransformationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.responsecode.ResponseCode;

@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(OpensaberClient.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class OpensaberClientUtilTest {

  private static String RESOURCE_PATH = "opensaberClientUtilTest/";
  private String REGISTRY_ID = "http://localhost:8080/ba7b659d-3c24-4b71-ae67-5208854a700c";
  private String ACCESS_TOKEN = "accessToken";
  private static OpensaberClient openSaberClient = null;

  @BeforeClass
  public static void setup() throws Exception {
    openSaberClient = Mockito.mock(OpensaberClient.class);
    PowerMockito.whenNew(OpensaberClient.class).withAnyArguments().thenReturn(openSaberClient);
  }

  @Test
  public void testOpensaberClientUtilInstanceCreationSuccess() {
    assertNotNull(new OpensaberClientUtil());
  }

  @Test
  public void testAddEntitySuccess() {
    String addSuccessResponse = getJSONFileAsString("add-entity-success-response.json");
    Map validEntityRegistryFormatMap = getJSONFileAsMap("valid-entity-registry-format.json");
    try {
      String entityId = mockAndAddEntity(addSuccessResponse, validEntityRegistryFormatMap);
      assertTrue(StringUtils.isNotBlank(entityId));
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testReadEntitySuccess() {
    String readSuccessResponse = getJSONFileAsString("read-entity-success-response.json");
    Map validEntityRegistryFormatMap = getJSONFileAsMap("valid-entity-registry-format.json");
    try {
      Map mapFromRegistry = mockAndReadEntity(readSuccessResponse);
      assertTrue(mapFromRegistry.equals(validEntityRegistryFormatMap));
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testUpdateEntitySuccess() {
    String updateSuccessResponse = getJSONFileAsString("update-entity-success-response.json");
    Map validEntityRegistryFormatMap = getJSONFileAsMap("valid-entity-registry-format.json");
    try {
      mockAndUpdateEntity(updateSuccessResponse, validEntityRegistryFormatMap);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testDeleteEntitySuccess() {
    String deleteSuccessResponse = getJSONFileAsString("delete-entity-success-response.json");
    try {
      mockAndDeleteEntity(deleteSuccessResponse);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testAddEntityFailureWithErrorResponse() {
    String addFailureResponse = getJSONFileAsString("add-entity-failure-response.json");
    try {
      mockAndAddEntity(addFailureResponse, new HashMap());
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryAddEntity.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testReadEntityFailureWithErrorResponse() {
    String readFailureResponse = getJSONFileAsString("read-entity-failure-response.json");
    try {
      mockAndReadEntity(readFailureResponse);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryReadEntity.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testUpdateEntityFailureWithErrorResponse() {
    String updateFailureResponse = getJSONFileAsString("update-entity-failure-response.json");
    try {
      mockAndUpdateEntity(updateFailureResponse, new HashMap());
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryUpdateEntity.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testDeleteEntityFailureWithErrorResponse() {
    String deleteErrorResponse = getJSONFileAsString("delete-entity-failure-response.json");
    try {
      mockAndDeleteEntity(deleteErrorResponse);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryDeleteEntity.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testAddEntityFailureWithClientIOException() {
    try {
      Mockito.when(openSaberClient.addEntity(Mockito.any(), Mockito.any()))
          .thenThrow(IOException.class);
      OpensaberClientUtil.addEntity(new HashMap(), ACCESS_TOKEN);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryAddEntity.getErrorCode()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testReadEntityFailureWithClientIOException() {
    try {
      Mockito.when(openSaberClient.readEntity(Mockito.any(), Mockito.any()))
          .thenThrow(IOException.class);
      OpensaberClientUtil.readEntity(REGISTRY_ID, ACCESS_TOKEN);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryReadEntity.getErrorCode()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testUpdateEntityFailureWithClientIOException() {
    try {
      Mockito.when(openSaberClient.updateEntity(Mockito.any(), Mockito.any()))
          .thenThrow(IOException.class);
      OpensaberClientUtil.updateEntity(new HashMap(), ACCESS_TOKEN);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryUpdateEntity.getErrorCode()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testDeleteEntityFailureWithClientIOException() {
    try {
      Mockito.when(openSaberClient.deleteEntity(Mockito.any(), Mockito.any()))
          .thenThrow(IOException.class);
      OpensaberClientUtil.deleteEntity(REGISTRY_ID, ACCESS_TOKEN);
    } catch (IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryDeleteEntity.getErrorCode()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testDeleteEntityFailureWithInvalidResponse() {
    try {
      mockAndDeleteEntity("Invalid Response");
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryParseResponse.getErrorCode()));
      throw e;
    }
  }

  private String mockAndAddEntity(String mockResponse, Map requestMap) {
    String entityId = null;
    try {
      Mockito.when(openSaberClient.addEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(mockResponse));
      entityId = OpensaberClientUtil.addEntity(requestMap, ACCESS_TOKEN);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } finally {
      Mockito.reset(openSaberClient);
    }
    return entityId;
  }

  private Map<String, Object> mockAndReadEntity(String mockResponse) {
    Map<String, Object> mapFromRegistry = null;
    try {
      Mockito.when(openSaberClient.readEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(mockResponse));
      mapFromRegistry = OpensaberClientUtil.readEntity(REGISTRY_ID, ACCESS_TOKEN);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } finally {
      Mockito.reset(openSaberClient);
    }
    return mapFromRegistry;
  }

  private void mockAndUpdateEntity(String mockResponse, Map requestMap) {
    try {
      Mockito.when(openSaberClient.updateEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(mockResponse));
      OpensaberClientUtil.updateEntity(requestMap, ACCESS_TOKEN);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  private void mockAndDeleteEntity(String mockResponse) {
    try {
      Mockito.when(openSaberClient.deleteEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(mockResponse));
      OpensaberClientUtil.deleteEntity(REGISTRY_ID, ACCESS_TOKEN);
    } catch (IOException | URISyntaxException e) {
      fail();
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  private Map getJSONFileAsMap(String fileName) {
    return TestUtil.getJSONFileAsMap(RESOURCE_PATH + fileName);
  }

  private String getJSONFileAsString(String fileName) {
    return TestUtil.getJSONFileAsString(RESOURCE_PATH + fileName);
  }
}
