package org.sunbird.extension.user.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.sunbird.extension.user.UserExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonObject;

import io.opensaber.registry.client.OpensaberClient;
import io.opensaber.registry.client.data.RequestData;
import io.opensaber.registry.client.data.ResponseData;
import io.opensaber.registry.exception.TransformationException;
import io.opensaber.registry.transform.ITransformer;
import io.opensaber.registry.transform.JsonToJsonLDTransformer;
import io.opensaber.registry.transform.JsonldToJsonTransformer;

/**
 * Implementation Class of UserExtension Interface
 * @author Jaikumar Soundara Rajan
 *
 */
public class UserProviderRegistryImpl implements UserExtension {

	private OpensaberClient client;
	private ResponseData<String> responseData;
	private Map<String, String> headers = new HashMap<>();
	private String accessToken = "accessToken";

	/* (non-Javadoc)
	 * @see org.sunbird.extension.user.UserExtension#create(java.util.Map)
	 */
	public void create(Map<String,Object> extensionMap) {

		try {
			initialize();
			addUser(extensionMap);
		} catch (Exception | Error e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method to initialize registry client
	 */
	private void initialize() {

		headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.put("x-authenticated-user-token", accessToken);

		ITransformer<String> jsonToJsonldTransformer = JsonToJsonLDTransformer.getInstance();
		ITransformer<String> jsonldToJsonTransformer = JsonldToJsonTransformer.getInstance();
		client = OpensaberClient.builder().requestTransformer(jsonToJsonldTransformer)
				.responseTransformer(jsonldToJsonTransformer).build();
	}

	/**
	 * Adds User Entity to the Registry
	 * @param extensionMap
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws TransformationException
	 */
	private void addUser(Map<String,Object> extensionMap)
			throws JsonProcessingException, IOException, TransformationException {

		JsonObject nameObj = new JsonObject();
		nameObj.addProperty("teacherName",(String) extensionMap.get("name"));
		JsonObject teacherObj = new JsonObject();
		teacherObj.add("teacher", nameObj);

		responseData = client.addEntity(new RequestData<>(teacherObj.toString()), headers);

	}

}