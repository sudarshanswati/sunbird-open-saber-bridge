package org.sunbird.extension.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.TransformationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest(OpensaberClient.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class OpensaberClientUtilTest {

  private ObjectMapper mapper = new ObjectMapper();
  private String registryId = "http://localhost:8080/ba7b659d-3c24-4b71-ae67-5208854a700c";
  private String accessToken = "accessToken";
  private static OpensaberClient openSaberClient = null;

  @BeforeClass
  public static void setup() throws Exception {
    openSaberClient = Mockito.mock(OpensaberClient.class);
    PowerMockito.whenNew(OpensaberClient.class).withAnyArguments().thenReturn(openSaberClient);
  }

  @Test
  public void testAddEntity() {
    String addSuccessResponse = null;
    Map validEntityRegistryFormatMap = null;
    try {
      addSuccessResponse = getJSONFileAsString("add-entity-success-response.json");
      validEntityRegistryFormatMap = getJSONFileAsMap("valid-entity-registry-format.json");
    } catch (IOException e) {
      fail();
    }
    try {
      Mockito.when(openSaberClient.addEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(addSuccessResponse));
      String entityId = OpensaberClientUtil.addEntity(validEntityRegistryFormatMap, accessToken);
      assertTrue(StringUtils.isNotBlank(entityId));
    } catch (TransformationException
        | IOException
        | URISyntaxException
        | ProjectCommonException e) {
      fail();
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test
  public void testReadEntity() {
    String readSuccessResponse = null;
    Map validEntityRegistryFormatMap = null;
    try {
      readSuccessResponse = getJSONFileAsString("read-entity-success-response.json");
      validEntityRegistryFormatMap = getJSONFileAsMap("valid-entity-registry-format.json");
    } catch (IOException e) {
      fail();
    }
    try {
      Mockito.when(openSaberClient.readEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(readSuccessResponse));
      Map mapFromRegistry = OpensaberClientUtil.readEntity(registryId, accessToken);
      assertTrue(mapFromRegistry.equals(validEntityRegistryFormatMap));
    } catch (TransformationException
        | IOException
        | URISyntaxException
        | ProjectCommonException e) {
      fail();
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test
  public void testUpdateEntity() {
    String updateSuccessResponse = null;
    Map validEntityRegistryFormatMap = null;
    try {
      updateSuccessResponse = getJSONFileAsString("update-entity-success-response.json");
      validEntityRegistryFormatMap = getJSONFileAsMap("valid-entity-registry-format.json");
    } catch (IOException e) {
      fail();
    }
    try {
      Mockito.when(openSaberClient.updateEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(updateSuccessResponse));
      OpensaberClientUtil.updateEntity(validEntityRegistryFormatMap, accessToken);
    } catch (TransformationException
        | IOException
        | URISyntaxException
        | ProjectCommonException e) {
      fail();
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test
  public void testDeleteEntity() {
    String deleteSuccessResponse = null;
    try {
      deleteSuccessResponse = getJSONFileAsString("delete-entity-success-response.json");
    } catch (IOException e) {
      fail();
    }
    try {
      Mockito.when(openSaberClient.deleteEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(deleteSuccessResponse));
      OpensaberClientUtil.deleteEntity(registryId, accessToken);
    } catch (IOException | URISyntaxException | ProjectCommonException e) {
      fail();
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testAddEntityWithErrorResponse() {
    String addErrorResponse = null;
    try {
      addErrorResponse = getJSONFileAsString("add-entity-error-response.json");
    } catch (IOException e) {
      fail();
    }
    try {
      Mockito.when(openSaberClient.addEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(addErrorResponse));
      String entityId = OpensaberClientUtil.addEntity(new HashMap(), accessToken);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage().equalsIgnoreCase(ResponseCode.errorRegistryAddEntity.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testReadEntityWithErrorResponse() {
    String readErrorResponse = null;
    try {
      readErrorResponse = getJSONFileAsString("read-entity-error-response.json");
    } catch (IOException e) {
      fail();
    }
    try {
      Mockito.when(openSaberClient.readEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(readErrorResponse));
      Map mapFromRegistry = OpensaberClientUtil.readEntity(registryId, accessToken);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage().equalsIgnoreCase(ResponseCode.errorRegistryReadEntity.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testUpdateEntityWithErrorResponse() {
    String updateErrorResponse = null;
    try {
      updateErrorResponse = getJSONFileAsString("update-entity-error-response.json");
    } catch (IOException e) {
      fail();
    }
    try {
      Mockito.when(openSaberClient.updateEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(updateErrorResponse));
      OpensaberClientUtil.updateEntity(new HashMap(), accessToken);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage()
              .equalsIgnoreCase(ResponseCode.errorRegistryUpdateEntity.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testDeleteEntityWithErrorResponse() {
    String deleteErrorResponse = null;
    try {
      deleteErrorResponse = getJSONFileAsString("delete-entity-error-response.json");
    } catch (IOException e) {
      fail();
    }
    try {
      Mockito.when(openSaberClient.deleteEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>(deleteErrorResponse));
      OpensaberClientUtil.deleteEntity(registryId, accessToken);
    } catch (IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage()
              .equalsIgnoreCase(ResponseCode.errorRegistryDeleteEntity.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testAddEntityIfClientReturnsIOException() {
    try {
      Mockito.when(openSaberClient.addEntity(Mockito.any(), Mockito.any()))
          .thenThrow(IOException.class);
      String entityId = OpensaberClientUtil.addEntity(new HashMap(), accessToken);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage().equalsIgnoreCase(ResponseCode.errorRegistryAddEntity.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testReadEntityIfClientReturnsIOException() {
    try {
      Mockito.when(openSaberClient.readEntity(Mockito.any(), Mockito.any()))
          .thenThrow(IOException.class);
      Map mapFromRegistry = OpensaberClientUtil.readEntity(registryId, accessToken);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage().equalsIgnoreCase(ResponseCode.errorRegistryReadEntity.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testUpdateEntityIfClientReturnsIOException() {
    try {
      Mockito.when(openSaberClient.updateEntity(Mockito.any(), Mockito.any()))
          .thenThrow(IOException.class);
      OpensaberClientUtil.updateEntity(new HashMap(), accessToken);
    } catch (TransformationException | IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage()
              .equalsIgnoreCase(ResponseCode.errorRegistryUpdateEntity.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testDeleteEntityIfClientReturnsIOException() {
    try {
      Mockito.when(openSaberClient.deleteEntity(Mockito.any(), Mockito.any()))
          .thenThrow(IOException.class);
      OpensaberClientUtil.deleteEntity(registryId, accessToken);
    } catch (IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage()
              .equalsIgnoreCase(ResponseCode.errorRegistryDeleteEntity.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testDeleteEntityWithInvalidResponse() {
    try {
      Mockito.when(openSaberClient.deleteEntity(Mockito.any(), Mockito.any()))
          .thenReturn(new ResponseData<String>("Invalid Response"));
      OpensaberClientUtil.deleteEntity(registryId, accessToken);
    } catch (IOException | URISyntaxException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage()
              .equalsIgnoreCase(ResponseCode.errorRegistryParseResponse.getErrorMessage()));
      throw e;
    } finally {
      Mockito.reset(openSaberClient);
    }
  }

  private Map getJSONFileAsMap(String fileName) throws IOException {
    return mapper.readValue(getFileAsInputStream(fileName), Map.class);
  }

  private String getJSONFileAsString(String fileName) throws IOException {
    return new BufferedReader(new InputStreamReader(getFileAsInputStream(fileName)))
        .lines()
        .collect(Collectors.joining("\n"));
  }

  private InputStream getFileAsInputStream(String fileName) {
    return this.getClass().getClassLoader().getResourceAsStream(fileName);
  }
}
