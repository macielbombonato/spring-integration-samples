/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.samples.restolat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.integration.samples.restolat.domain.UserList;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * RestHttpClientTest.java: Functional Test to test the REST HTTP Path usage. This test requires
 * restolat-http application running in HTTP environment. 
 * NOTE: If in your first execution you get a not class def found, delete @RunWith, run you test and 
 * put it again.
 * @author Vigil Bose
 * @author Maciel Escudero Bombonato - Adaptation for OLAT test.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/spring/integration/http-outbound-config.xml"})
public class RestHttpClientTest {

	@Autowired
	private RestTemplate restTemplate;
	private HttpMessageConverterExtractor<UserList> responseExtractor;
	private static Logger logger = Logger.getLogger(RestHttpClientTest.class);
	@Autowired
	private Jaxb2Marshaller marshaller;
	@Autowired
	private ObjectMapper jaxbJacksonObjectMapper;

	@Before
	public void setUp() {
		responseExtractor = new HttpMessageConverterExtractor<UserList>(UserList.class, restTemplate.getMessageConverters());
		
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");
		properties.put(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setMarshallerProperties(properties);
	}
	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetUsersAsXml() throws Exception{

		Map<String, Object> userSearchMap = getUserSearchMap("0");

		final String fullUrl = "http://localhost:8080/restolat-http/services/user/{id}/search";

		UserList userList = restTemplate.execute(fullUrl, HttpMethod.GET,
				new RequestCallback() {
					@Override
					public void doWithRequest(ClientHttpRequest request) throws IOException {
						HttpHeaders headers = getHttpHeadersWithUserCredentials(request);
						headers.add("Accept", "application/xml");
					}
		}, responseExtractor, userSearchMap);

		logger.info("The user list size :"+userList.getUser().size());

		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);

		marshaller.marshal(userList, sr);
		logger.info(sr.getWriter().toString());
		assertTrue(userList.getUser().size() > 0);
	}

	private Map<String, Object> getUserSearchMap(String id) {
		Map<String, Object> userSearchMap = new HashMap<String, Object>();
		userSearchMap.put("id", id);
		return userSearchMap;
	}
	
	@Test
	public void testGetUserAsJson() throws Exception{
		Map<String, Object> userSearchMap = getUserSearchMap("0");
		
		final String fullUrl = "http://localhost:8080/restolat-http/services/user/{id}/search?format=json";
		HttpHeaders headers = getHttpHeadersWithUserCredentials(new HttpHeaders());
		headers.add("Accept", "application/json");
		HttpEntity<Object> request = new HttpEntity<Object>(headers);
		
		ResponseEntity<?> httpResponse = restTemplate.exchange(fullUrl, HttpMethod.GET, request, UserList.class, userSearchMap);
		logger.info("Return Status :"+httpResponse.getHeaders().get("X-Return-Status"));
		logger.info("Return Status Message :"+httpResponse.getHeaders().get("X-Return-Status-Msg"));
		assertTrue(httpResponse.getStatusCode().equals(HttpStatus.OK));
		jaxbJacksonObjectMapper.writeValue(System.out, httpResponse.getBody());
	}

	private HttpHeaders getHttpHeadersWithUserCredentials(ClientHttpRequest request){
		return (getHttpHeadersWithUserCredentials(request.getHeaders()));
	}

	private HttpHeaders getHttpHeadersWithUserCredentials(HttpHeaders headers){

		String username = "SPRING";
		String password = "spring";

		String combinedUsernamePassword = username+":"+password;
		byte[] base64Token = Base64.encode(combinedUsernamePassword.getBytes());
		String base64EncodedToken = new String (base64Token);
		//adding Authorization header for HTTP Basic authentication
		headers.add("Authorization","Basic  "+base64EncodedToken);

		return headers;
	}
}


