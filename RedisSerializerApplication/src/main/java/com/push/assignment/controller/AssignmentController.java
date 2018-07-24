package com.push.assignment.controller;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.push.assignment.config.ValidationUtils;
import com.push.assignment.encrypt.AES;
import com.push.assignment.service.AssignmentService;

import redis.clients.jedis.Jedis;

/**
 * Provides all end points for web services in Assignment 1
 * 
 * @author pushkar
 *
 */

@RestController
public class AssignmentController {

	/**
	 * Service Layer
	 */
	@Autowired
	AssignmentService assignmentService;

	@Autowired
	Jedis jedis;
	
	/**
	 * Method to insert data into redis
	 * @param object
	 * @return
	 */
	@RequestMapping(value = "/plan", method = RequestMethod.POST)
	public String inesertToRedis(HttpServletRequest request, @RequestBody final String object) {
		String status = "";
		String schemaText = null;
		try {
			if(request.getHeader("Authorization") == null) {
				return "Authorization token not found.";
			}

			if (!tokenValidation(request)) {
				return "Invalid or Broken Token";
			}
			
			schemaText =jedis.get("schema");
			if (ValidationUtils.isJsonValid(schemaText, object)) {
				status = assignmentService.inesertToRedis(object);
			} else {
				status = "Invalid JSON";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return status;
	}

	
	/**
	 * Method to retrieve data from redis
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/plan/{id}", method = RequestMethod.GET,  produces = "application/json")
	public String retrieveFromRedis(HttpServletRequest request, HttpServletResponse response, @PathVariable String id) {
		String object = "";
		try {
			if(request.getHeader("Authorization") == null) {
				return  "Authorization token not found.";

			}

			if (!tokenValidation(request)) {
				return "Invalid or Broken Token";
			}
			
			object = assignmentService.retrieveFromRedis(id,request,response);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return object;
	}
	
	/**
	 * Method to delete data from redis
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/plan/{id}", method = RequestMethod.DELETE)
	public String deleteFromRedis(HttpServletRequest request, HttpServletResponse response, @PathVariable String id) {
		String status = "";
		try {
			if(request.getHeader("Authorization") == null) {
				return "Please provide authorization token";
			}

			if (!tokenValidation(request)) {
				return "Unauthorised";
			}
			
			status = assignmentService.deleteFromRedis(request,response,id);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * Method to update data in redis
	 * @param request
	 * @param response
	 * @param id
	 * @param object
	 * @return
	 * @throws JSONException
	 * @throws ProcessingException
	 * @throws IOException
	 */
	@RequestMapping(value = "/plan/{id}", method = RequestMethod.PUT)
	public String updatePlan(HttpServletRequest request, HttpServletResponse response,@PathVariable String id, @RequestBody String object)

			throws JSONException, ProcessingException, IOException {

		if(request.getHeader("Authorization") == null) {

			return "Authorization token not found.";

		}

		

		if (!tokenValidation(request)) {

			return "Invalid or Broken Token";

		}

		Map<String, String> plan = jedis.hgetAll("plan." + id);

		if (!plan.isEmpty()) {

			String headerETag = request.getHeader("If-Match");

			String storedEtag = plan.get("etag");

			if (headerETag == null || storedEtag == null || headerETag.equals(storedEtag)) {

				String saveStatus = inesertToRedis(request, object);

				if (saveStatus.equals("Invalid JSON")) {

					return saveStatus;

				} else {

					return "Plan updated";

				}

			} else {

				response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);

				return "";

			}

		} else {

			response.setStatus(HttpServletResponse.SC_NOT_FOUND);

			return "";

		}



	}

	
	/**
	 * Method to validate the received bearer token
	 * 
	 * @param request
	 * @return
	 */
	public boolean tokenValidation(HttpServletRequest request) {

		final String secretKey = "notthebestpracticeshoulduseapplicationproperties";

		String header = request.getHeader("Authorization");

		if (header.contains("Bearer")) {

			String token = header.split(" ")[1];

			String decryptedToken = AES.decrypt(token, secretKey);

			JSONObject js;

			try {

				js = new JSONObject(decryptedToken);

				Date cur_time = new Date();

				Date token_Time = new Date(js.getString("expiry"));

				if (token_Time.compareTo(cur_time) > 0) {

					return true;

				}

			} catch (JSONException e) {

				return false;

			}

		}

		return false;

	}

	
	
	/**
	 * Method to validate json payload against the json schema
	 * 
	 * @param request
	 * @param response
	 * @param object
	 * @return
	 * @throws JSONException
	 * @throws ProcessingException
	 * @throws IOException
	 */
	@RequestMapping(value = "/schema", method = RequestMethod.POST)
	public @ResponseBody String savePlanSchema(HttpServletRequest request, HttpServletResponse response,

			@RequestBody String object) throws JSONException, ProcessingException, IOException {

		JSONObject errorObject = new JSONObject();

		if (request.getHeader("Authorization") == null) {

			errorObject.put("Error", "Not Authorized");

			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

			return errorObject.toString();

		}



		if (!tokenValidation(request)) {

			errorObject.put("Error", "Not Authorized");

			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

			return errorObject.toString();

		}	

		jedis.set("schema",object);

		return "";

	}
}
