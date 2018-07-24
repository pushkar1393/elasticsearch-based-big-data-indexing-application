package com.push.assignment.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AssignmentService {

	

	public List<String> retrievePersonList() throws Exception;

	public String inesertToRedis(String object) throws Exception;

	public String retrieveFromRedis(String id, HttpServletRequest request, HttpServletResponse response ) throws Exception;

	public String deleteFromRedis( HttpServletRequest request, HttpServletResponse response ,String id) throws Exception;


}
