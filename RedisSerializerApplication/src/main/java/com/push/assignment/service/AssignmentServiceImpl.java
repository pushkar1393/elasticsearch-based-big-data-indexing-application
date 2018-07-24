package com.push.assignment.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.push.assignment.dao.AssignmentDao;

@Service
public class AssignmentServiceImpl implements AssignmentService {

	@Autowired
	AssignmentDao assignmentDao;
	

	@Transactional
	@Override
	public List<String> retrievePersonList() throws Exception {
		return assignmentDao.retrievePersonList();
	}

	@Transactional
	@Override
	public String inesertToRedis(String object) throws Exception {
		return assignmentDao.insertToRedis(object);
	}

	@Transactional
	@Override
	public String retrieveFromRedis(String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		return assignmentDao.retrieveFromRedis(id, request, response);
	}

	@Transactional
	@Override
	public String deleteFromRedis(HttpServletRequest request, HttpServletResponse response,String id ) throws Exception {
		return assignmentDao.deleteFromRedis(request, response,id);
	}


}
