package org.sunbird.extension.user.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

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
import org.sunbird.extension.util.TestUtil;

@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(OpensaberClientUtil.class)
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*", "javax.security.*"})
public class UserProviderRegistryImplTest {

  private static String RESOURCE_PATH = "userProviderRegistryImplTest/";
  private UserExtension userExtension = new UserProviderRegistryImpl();
  private String REGISTRY_ID = "http://localhost:8080/ba7b659d-3c24-4b71-ae67-5208854a700c";

  @Before
  public void setup() {
    mockStatic(OpensaberClientUtil.class);
  }

  @Test
  public void testCreateUserSuccess() {
    try {
      Map userProfileMap = createUser("test-create-user-success.json");
      assertEquals(REGISTRY_ID, (String) userProfileMap.get(JsonKey.REGISTRY_ID));
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testReadUserSuccess() {
    Map validEntityRegistryFormat = getJSONFileAsMap("valid-entity-registry-format.json");
    PowerMockito.when(OpensaberClientUtil.readEntity(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(validEntityRegistryFormat);

    Map validEntitySunbirdFormat = getJSONFileAsMap("valid-entity-sunbird-format.json");
    Map userIdMap = getJSONFileAsMap("test-read-user-success.json");
    try {
      Map outputMap = userExtension.read(userIdMap);
      assertTrue(validEntitySunbirdFormat.equals(outputMap));
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testUpdateUserSuccess() {
    try {
      Map userProfileMap = getJSONFileAsMap("test-update-user-success.json");
      userExtension.update(userProfileMap);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test
  public void testDeleteUserSuccess() {
    try {
      Map userIdMap = getJSONFileAsMap("test-delete-user-success.json");
      userExtension.delete(userIdMap);
    } catch (ProjectCommonException e) {
      fail();
    }
  }

  @Test(expected = ProjectCommonException.class)
  public void testCreateUserFailureWithInvalidDate() {
    createUserAndCheckException(
        "test-create-user-failure-with-invalid-date.json",
        ResponseCode.errorJsonTransformInvalidInput);
  }

  @Test(expected = ProjectCommonException.class)
  public void testCreateUserFailureWithInvalidEnumInput() {
    createUserAndCheckException(
        "test-create-user-failure-with-invalid-enum-input.json",
        ResponseCode.errorJsonTransformInvalidEnumInput);
  }

  @Test(expected = ProjectCommonException.class)
  public void testReadUserFailureWithoutRegistryId() {
    try {
      userExtension.read(new HashMap());
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(ResponseCode.errorRegistryEntityIdBlank.getErrorCode()));
      throw e;
    }
  }

  private Map getJSONFileAsMap(String fileName) {
    return TestUtil.getJSONFileAsMap(RESOURCE_PATH + fileName);
  }

  private Map createUser(String fileName) {
    PowerMockito.when(OpensaberClientUtil.addEntity(Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(REGISTRY_ID);
    Map userProfileMap = getJSONFileAsMap(fileName);
    userExtension.create(userProfileMap);
    return userProfileMap;
  }

  private Map createUserAndCheckException(String fileName, ResponseCode responseCode) {
    try {
      return createUser(fileName);
    } catch (ProjectCommonException e) {
      assertTrue(e.getCode().equals(responseCode.getErrorCode()));
      throw e;
    }
  }
}
