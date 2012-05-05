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
package org.springframework.integration.samples.restolat.service;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.samples.restolat.domain.AuthOlatMessage;
import org.springframework.integration.samples.restolat.domain.UserList;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * UserSearchService.java: This is the default employee search service
 * @author Vigil Bose
 * @author Maciel Escudero Bombonato - Adaptation for Olat Access.
 */
@Service("userSearchService")
public class UserSearchService {

	private static Logger logger = Logger.getLogger(UserSearchService.class);
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private Jaxb2Marshaller marshaller;
	
	/**
	 * The API <code>getEmployee()</code> looks up the mapped in coming message header's id param
	 * and fills the return object with the appropriate employee details. The return
	 * object is wrapped in Spring Integration Message with response headers filled in.
	 * This example shows the usage of URL path variables and how the service act on
	 * those variables.
	 * @param inMessage
	 * @return an instance of <code>{@link Message}</code> that wraps <code>{@link UserList}</code>
	 */
	public Message<UserList> getUser(Message<?> inMessage){
	
		UserList userList = null;
		Message<UserList> message = null;
		Map<String, Object> responseHeaderMap = new HashMap<String, Object>();
		
		try{
			// Authentication
			final String fullUrlAuth = "http://demo.olat.org/demo/restapibeta/auth/administrator?password=olat";
			
			HttpHeaders headersAuth = new HttpHeaders();
			headersAuth.add("Accept", "application/xml");
			HttpEntity<Object> requestAuth = new HttpEntity<Object>(headersAuth);
			
			ResponseEntity<?> httpResponseAuth = restTemplate.exchange(fullUrlAuth, HttpMethod.GET, requestAuth, AuthOlatMessage.class);
			
			logger.info("Return Status :"+httpResponseAuth.getHeaders().get("X-Return-Status"));
			logger.info("Return Status Message :"+httpResponseAuth.getHeaders().get("X-Return-Status-Msg"));
			
			final String olatToken = httpResponseAuth.getHeaders().get("X-OLAT-TOKEN").get(0);
			
			// User list
			Map<String, Object> userSearchMap = getUserSearchMap("0");
			
			final String fullUrl = "http://demo.olat.org/demo/restapibeta/users";

			HttpHeaders headers = new HttpHeaders();
			
			headers.add("Accept", "application/xml");
			headers.add("X-OLAT-TOKEN", olatToken);
			
			HttpEntity<Object> request = new HttpEntity<Object>(headers);
			
			ResponseEntity<?> httpResponse = restTemplate.exchange(fullUrl, HttpMethod.GET, request, UserList.class, userSearchMap);
			
			if (httpResponse.getBody() != null && httpResponse.getBody() instanceof UserList) {
				userList = (UserList) httpResponse.getBody();
				
				if (userList != null) {
					logger.info("The user list size :"+userList.getUser().size());
					
					StringWriter sw = new StringWriter();
					StreamResult sr = new StreamResult(sw);

					marshaller.marshal(userList, sr);
					logger.info(sr.getWriter().toString());
					
					message = new GenericMessage<UserList>(userList, responseHeaderMap);
				}
			}
		}catch (Throwable e){
			setReturnStatusAndMessage("1", "System Error", userList, responseHeaderMap);
			logger.error("System error occured :"+e);
		}
		
		return message;		
	}
	
	private Map<String, Object> getUserSearchMap(String id) {
		Map<String, Object> userSearchMap = new HashMap<String, Object>();
		userSearchMap.put("id", id);
		return userSearchMap;
	}
	
	/**
	 * The API <code>setReturnStatusAndMessage()</code> sets the return status and return message
	 * in the return message payload and its header.
	 * @param status
	 * @param message
	 * @param userList
	 * @param responseHeaderMap
	 */
	private void setReturnStatusAndMessage(String status, 
						String message, 
						UserList userList, 
						Map<String, Object> responseHeaderMap){
		
		responseHeaderMap.put("Return-Status", status);
		responseHeaderMap.put("Return-Status-Msg", message);
	}
	
	public void setMarshaller(Jaxb2Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
}


