package com.push.assignment.dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.DigestUtils;

import com.push.assignment.config.QueueConfigurer;


import redis.clients.jedis.Jedis;

@Repository
public class AssignmentDaoImpl implements AssignmentDao {

	@Autowired
	Jedis jedis;

	// private static final String REDIS_INDEX_KEY = "PERSON";

	Map<String, String> planMap = new HashMap<String, String>();
	Map<String, String> planCostShareMap = new HashMap<String, String>();
	Map<String, String> linkedPlanServices = new HashMap<String, String>();
	Map<String, String> linkedService = new HashMap<String, String>();
	Map<String, String> planserviceCostShares = new HashMap<String, String>();


	@Override
	public List<String> retrievePersonList() throws Exception {
		// Map<Object, Object> map;
		List<String> personList = null;

		return personList;
	}

	@Override
	public String insertToRedis(String object) throws Exception {
		JSONObject planJsonObj = new JSONObject(object);
		String inputstream = planJsonObj.toString();
		InputStream is = new ByteArrayInputStream(inputstream.getBytes());
		String etag = generateETagHeaderValue(is);
		QueueConfigurer qc = new QueueConfigurer(jedis);
		
		Iterator<?> keys = planJsonObj.keys();
		String key = "";
		String status = "";
		while (keys.hasNext()) {
			key = (String) keys.next();
			if (planJsonObj.get(key) instanceof JSONObject) {
				JSONObject planCostShareJsonObj = (JSONObject) planJsonObj.get(key);
				planCostShareMap = insertKV(planCostShareJsonObj);
				save(planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId") + "." + key,
						planCostShareMap);
				qc.sendJobToWaitQueue(planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId") + "." + key);
			} else if (planJsonObj.get(key) instanceof JSONArray) {
				JSONArray linkedPlanServicesJsonArray = (JSONArray) planJsonObj.get(key);
				for (int i = 0; i < linkedPlanServicesJsonArray.length(); i++) {
					JSONObject linkedServiceJsonObj = (JSONObject) linkedPlanServicesJsonArray.get(i);
					Iterator<?> linkedServiceKeys = linkedServiceJsonObj.keys();
					String linkedServiceKey = "";
					while (linkedServiceKeys.hasNext()) {
						linkedServiceKey = (String) linkedServiceKeys.next();
						if (linkedServiceJsonObj.get(linkedServiceKey) instanceof JSONObject) {
							JSONObject obj1 = (JSONObject) linkedServiceJsonObj.get(linkedServiceKey);
							linkedService = insertKV(obj1);
							save(planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId") + "."
									+ key + "." + linkedServiceJsonObj.getString("objectId") + "." + linkedServiceKey,
									linkedService);
							qc.sendJobToWaitQueue(planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId") + "."
									+ key + "." + linkedServiceJsonObj.getString("objectId") + "." + linkedServiceKey);
							linkedService.clear();
						} else {
							linkedPlanServices.put(linkedServiceKey,
									String.valueOf(linkedServiceJsonObj.getString(linkedServiceKey)));
						}
					}
					save(planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId") + "." + key
							+ "." +linkedServiceJsonObj.getString("objectId") + "." + linkedServiceJsonObj.getString("objectType"), linkedPlanServices);
					qc.sendJobToWaitQueue(planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId") + "." + key
							+ "." +linkedServiceJsonObj.getString("objectId") + "." + linkedServiceJsonObj.getString("objectType"));
					String sKey = planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId") + "."
							+ key;
					System.out.println(sKey);
					jedis.sadd(sKey,planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId") + "." + key
							+ "." +linkedServiceJsonObj.getString("objectId") + "." + linkedServiceJsonObj.getString("objectType"));
				}
			} else {
				planMap.put(key, String.valueOf(planJsonObj.get(key)));
			}
		}
		planMap.put("etag", etag);
		save(planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId"), planMap);
		qc.sendJobToWaitQueue(planJsonObj.getString("objectType") + "." + planJsonObj.getString("objectId"));
		status = "Plan ID -> "+ planJsonObj.getString("objectId")+ "\n Product saved.";
		return status;
	}

	public String generateETagHeaderValue(InputStream inputStream) throws IOException {

		StringBuilder builder = new StringBuilder(37);

		DigestUtils.appendMd5DigestAsHex(inputStream, builder);

		return builder.toString();

	}

	public static Map<String, String> insertKV(JSONObject object) throws JSONException {
		Map<String, String> map = new HashMap<String, String>();

		Iterator<String> it = object.keys();
		while (it.hasNext()) {
			String key = it.next();
			Object value = object.get(key);
			if (value instanceof JSONObject) {
				value = insertKV((JSONObject) value);
			}
			map.put(key, String.valueOf(value));
		}
		return map;
	}

	public void save(String key, Map<String, String> planCostShareMap2) {
		jedis.hmset(key, planCostShareMap2);
	}

	@Override
	public String retrieveFromRedis(String id, HttpServletRequest request, HttpServletResponse response) throws Exception {
		JSONObject object = new JSONObject();
		Map<String, String> plan = jedis.hgetAll("plan." + id);
		if (!plan.isEmpty()) {
			String headerETag = request.getHeader("If-None-Match");
			String storedEtag = plan.get("etag");
			
			if (headerETag == null || storedEtag == null || !headerETag.equals(storedEtag)) {
				response.setHeader("Etag", storedEtag);
		Iterator it = plan.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			object.put((String) pairs.getKey(), pairs.getValue());
		}
		Map<String, String> planCostShare = jedis.hgetAll("plan." + id + ".planCostShares");
		if(planCostShare != null)
		object.put("planCostShares", planCostShare);

		Set<String> setLinkedPlans = jedis.smembers("plan." + id + ".linkedPlanServices");
		
		JSONArray linkedpricearray = new JSONArray();
		Iterator<String> iter = setLinkedPlans.iterator();
		while (iter.hasNext()) {
			// System.out.println(iter.next());
			String aaa = iter.next();
			Map<String, String> linkedPlanServices = jedis.hgetAll(aaa);
			JSONObject object1 = new JSONObject();
			Iterator iter1 = linkedPlanServices.entrySet().iterator();
			String idkey = "";

			while (iter1.hasNext()) {
				Map.Entry pairs = (Map.Entry) iter1.next();
				if (pairs.getKey().equals("objectId")) {
					idkey = (String) pairs.getValue();

				}

				object1.put((String) pairs.getKey(), pairs.getValue());
			}

			System.out.println("idkey " + idkey);
			String key1 = "plan." + id + ".linkedPlanServices." + idkey + ".planserviceCostShares";
			String key2 = "plan." + id + ".linkedPlanServices." + idkey + ".linkedService";
			System.out.println(key1);
			System.out.println(key2);
			Map<String, String> planserviceCostShares = jedis.hgetAll(key1);
			Map<String, String> linkedService = jedis.hgetAll(key2);
			object1.put("planserviceCostShares", planserviceCostShares);
			object1.put("linkedService", linkedService);

			linkedpricearray.put(object1);

			System.out.println(linkedpricearray);
		}
		

		object.put("linkedPlanServices", linkedpricearray);
		

		return object.toString();
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			}
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return object.toString();
		}
		return "";
	}

	@Override
	public String deleteFromRedis( HttpServletRequest request, HttpServletResponse response ,String id) throws Exception {
		Set<String> keys = jedis.keys("plan." + id + "*");
		Set<String> linkedPlanServicesKeys = new HashSet<String>();
		for (String key : keys) {
			String o = "plan." + id + ".linkedPlanServices";
			if (key.equals(o)) {
				linkedPlanServicesKeys = jedis.smembers(o);
				for (String k : linkedPlanServicesKeys) {
					linkedPlanServicesKeys.add(k);
				}
			}
		}

		if (!keys.isEmpty()) {
			jedis.del(linkedPlanServicesKeys.toArray(new String[linkedPlanServicesKeys.size()]));
			jedis.del(keys.toArray(new String[keys.size()]));
			return "Plan deleted successfully";
		}
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		return "Plan does not exists";
	}


}
