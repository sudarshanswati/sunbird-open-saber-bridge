package org.sunbird.extension.user.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.extension.user.UserExtension;
import org.sunbird.extension.util.OpensaberClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest(OpensaberClientUtil.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class UserProviderRegistryImplTest {

  private UserExtension userExtension = new UserProviderRegistryImpl();
  private ObjectMapper mapper = new ObjectMapper();
  private String registryId = "http://localhost:8080/ba7b659d-3c24-4b71-ae67-5208854a700c";

  @Before
  public void setup() {
    mockStatic(OpensaberClientUtil.class);
  }

  @Test
  public void testCreateUser() {
    Map userProfileMap = null;
    try {
      userProfileMap = createUser("test-create-user.json");
    } catch (IOException e) {
      fail();
    }
    assertEquals(registryId, (String) userProfileMap.get(JsonKey.REGISTRY_ID));
  }

  @Test
  public void testReadUser() {
    Map validEntityRegistryFormat = null;
    try {
      validEntityRegistryFormat = getJSONFileAsMap("valid-entity-registry-format.json");
    } catch (IOException e) {
      fail();
    }
    PowerMockito.when(OpensaberClientUtil.readEntity(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(validEntityRegistryFormat);

    Map validEntitySunbirdFormat = null;
    try {
      validEntitySunbirdFormat = getJSONFileAsMap("valid-entity-sunbird-format.json");
    } catch (IOException e) {
      fail();
    }

    Map userIdMap = null;
    try {
      userIdMap = getJSONFileAsMap("test-read-user.json");
    } catch (IOException e) {
      fail();
    }
    Map outputMap = userExtension.read(userIdMap);
    assertTrue(validEntitySunbirdFormat.equals(outputMap));
  }

  @Test
  public void testUpdateUser() {
    Map userProfileMap = null;
    try {
      userProfileMap = getJSONFileAsMap("test-update-user.json");
    } catch (IOException e) {
      fail();
    }
    try {
      userExtension.update(userProfileMap);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testDeleteUser() {
    Map userIdMap = null;
    try {
      userIdMap = getJSONFileAsMap("test-delete-user.json");
    } catch (IOException e) {
      fail();
    }
    try {
      userExtension.delete(userIdMap);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testCreateUserWithInvalidDate() {
    try {
      createUser("test-create-user-with-invalid-date.json");
    } catch (IOException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorJsonTransformInvalidInput.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testCreateUserWithInvalidEnumInput() {
    try {
      createUser("test-create-user-with-invalid-enum-input.json");
    } catch (IOException e) {
      fail();
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getCode().equals(ResponseCode.errorJsonTransformInvalidEnumInput.getErrorCode()));
      throw e;
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testReadUserWithoutRegistryId() {
    try {
      userExtension.read(new HashMap());
    } catch (ProjectCommonException e) {
      assertTrue(
          e.getMessage()
              .equalsIgnoreCase(ResponseCode.errorRegistryEntityIdBlank.getErrorMessage()));
      throw e;
    }
  }

  private Map getJSONFileAsMap(String fileName) throws IOException {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
    return mapper.readValue(is, Map.class);
  }

  private Map createUser(String fileName) throws IOException {
    PowerMockito.when(OpensaberClientUtil.addEntity(Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(registryId);

    Map userProfileMap = getJSONFileAsMap(fileName);
    userExtension.create(userProfileMap);
    return userProfileMap;
  }
}
