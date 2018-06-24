package org.sunbird.extension.user.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.TransformationException;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.JsonToJsonLDTransformer;
import io.opensaber.registry.transform.JsonldToJsonTransformer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.extension.user.UserExtension;
import org.sunbird.extension.util.SunbirdExtensionConstants;
import org.sunbird.extension.util.TransformJsonUtil;

/**
 * User profile extension using Open Saber registry for storing adopter specific custom user details
 *
 * @author Jaikumar Soundara Rajan
 */
public class UserProviderRegistryImpl implements UserExtension {

  private OpensaberClient client;
  private ResponseData<String> responseData;
  private Map<String, String> headers = new HashMap<>();
  private String accessToken = "accessToken";
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public void create(Map<String, Object> userProfileMap) {

    try {
      initialize();
      // hardcoded to teacher till userType enhancement is done
      userProfileMap.put("userType", "teacher");
      addUser(userProfileMap);
    } catch (IOException | TransformationException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void read(Map<String, Object> userProfileMap) {

    initialize();
    // hardcoded to teacher till userType enhancement is done
    userProfileMap.put("userType", "teacher");
    readUser(userProfileMap);
  }

  @Override
  public void update(Map<String, Object> userProfileMap) {

    try {
      initialize();
      // hardcoded to teacher till userType enhancement is done
      userProfileMap.put("userType", "teacher");
      updateUser(userProfileMap);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void delete(Map<String, Object> userProfileMap) {
    // TODO Auto-generated method stub

  }

  /** Method to initialize registry client */
  private void initialize() {

    headers.put(SunbirdExtensionConstants.CONTENTTYPE, MediaType.APPLICATION_JSON_VALUE);
    headers.put(SunbirdExtensionConstants.ACCESSTOKEN, accessToken);

    ITransformer<String> jsonToJsonldTransformer = JsonToJsonLDTransformer.getInstance();
    ITransformer<String> jsonldToJsonTransformer = JsonldToJsonTransformer.getInstance();
    client =
        OpensaberClient.builder()
            .requestTransformer(jsonToJsonldTransformer)
            .responseTransformer(jsonldToJsonTransformer)
            .build();
  }

  /**
   * Adds User Entity to the Registry
   *
   * @param extensionMap
   * @throws JsonProcessingException
   * @throws IOException
   * @throws TransformationException
   */
  private void addUser(Map<String, Object> userProfileMap)
      throws JsonProcessingException, IOException, TransformationException {

    try {
      Config config = ConfigFactory.load(SunbirdExtensionConstants.USER_CREATE_MAPPING_FILE);
      String userType = userProfileMap.get(SunbirdExtensionConstants.USERTYPE).toString();
      Map<String, Object> userMap =
          new TransformJsonUtil().transform(config, userProfileMap, userType);

      String mainProvider = config.getString(SunbirdExtensionConstants.SUNBIRD_MAIN_PROVIDER);
      List<Map> externalIds = (List<Map>) userMap.get(SunbirdExtensionConstants.EXTERNALIDS);
      for (Map externalIdDetails : externalIds) {
        if (mainProvider.equalsIgnoreCase(
            (String) externalIdDetails.get(SunbirdExtensionConstants.PROVIDER))) {
          userMap.put(
              SunbirdExtensionConstants.TEACHERID,
              externalIdDetails.get(SunbirdExtensionConstants.ID));
        }
      }

      /* System.out.println("userMap-->"+userMap); */

      responseData =
          client.addEntity(new RequestData<>(mapper.writeValueAsString(userMap)), headers);

      Map<String, Object> responseMap =
          mapper.readValue(responseData.getResponseData(), HashMap.class);
      Map<String, Object> resultMap =
          (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.RESULT);

      /*
       * System.out.println("responseData-->"+responseData);
       * System.out.println("responseMap-->"+responseMap);
       * System.out.println("resultMap-->"+resultMap);
       * System.out.println("registryId-->"+resultMap.get("entity"));
       */

      userProfileMap.put(
          SunbirdExtensionConstants.REGISTRYID, resultMap.get(SunbirdExtensionConstants.ENTITY));
    } catch (Exception | Error e) {
      e.printStackTrace();
    }
  }

  private void readUser(Map<String, Object> userProfileMap) throws ProjectCommonException {

    String registryId = (String) userProfileMap.get(SunbirdExtensionConstants.REGISTRYID);
    try {
      responseData = client.readEntity(new URI(registryId), headers);
    } catch (TransformationException | URISyntaxException e) {
      // throw ProjectCommonException
    }

    Map<String, Object> responseMap = null;
    try {
      responseMap = mapper.readValue(responseData.getResponseData(), HashMap.class);
    } catch (IOException e) {
      // throw ProjectCommonException
    }
    Map<String, Object> resultMap =
        (Map<String, Object>) responseMap.get(SunbirdExtensionConstants.RESULT);

    String userType = userProfileMap.get(SunbirdExtensionConstants.USERTYPE).toString();
    Map<String, Object> userMap = (Map<String, Object>) resultMap.get(userType);
    userMap.remove(SunbirdExtensionConstants.ID);
    userProfileMap.put(SunbirdExtensionConstants.REGISTRY, userMap);
    userProfileMap.remove(SunbirdExtensionConstants.REGISTRYID);
  }

  private void updateUser(Map<String, Object> userProfileMap) {

    // to be implemented
  }
}
