package com.chives.serve.backend.processor;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.json.JSONObject;

import com.chives.serve.backend.models.Order;
import com.chives.serve.backend.models.Restaurant;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

public class RestaurantClient {
	ArrayList<Restaurant> restaurants;
	MongoClient mc;
	final String API_KEY = "c108d72460d217557823ff13282a7b23";

	public RestaurantClient(MongoClient o) {
		this.mc = o;
		this.populate();
	}

	public Order placeOrder(String orderItem, int id, int echoId, int isFinished) {
		Restaurant r = findRestaurantById(id);
		Order order = findOrderById(r, echoId); // new
												// Order(findRestaurantById(id),
												// custId);
		if (isFinished == 1 || isFinished == 2) {
			order = finishOrder(r, order);
			System.out.println(id);
			FindIterable<Document> cursor = mc.getDatabase("serve").getCollection("restaurants").find(new BasicDBObject("restaurant.id", id+""));
			String accountId = "";
			if (cursor != null) {
				accountId = cursor.first().getString("accountId");
			}
			try {
				deposit(accountId, order.subtotal);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// deposit(accountId, order.subtotal);
			// webCall("")
		} else {

			if (order == null) {
				order = new Order(r, echoId);
				r.addOrder(order);
			}
			order.add(orderItem);
		}

		return order;
	}

	public void deposit(String accountId, double subtotal) throws ClientProtocolException, IOException {
		String body = "{\r\n" + "				  \"medium\": \"balance\",\r\n"
				+ "				  \"transaction_date\": \"2017-04-02\",\r\n" + "				  \"amount\":"
				+ (subtotal*1.07) + ",\r\n" + "				  \"description\": \"order\"\r\n" + "				}";
		System.out.println(body);
		this.webCallPost("/accounts/" + accountId + "/deposits", body);
	}

	// itemname,restid,echoid,isfinished
	public Order placeOrder(String id, String orderItem, String echoId, String isFinished) {
		return this.placeOrder(orderItem, Integer.parseInt(id), Integer.parseInt(echoId), Integer.parseInt(isFinished));
	}

	public Order finishOrder(Restaurant r, Order order) {
		r.removeOrder(order);
		return order;
	}

	public Order findOrderById(Restaurant r, int echoId) {
		ArrayList<Order> orders = r.orders;
		for (int i = 0; i < orders.size(); i++) {
			if (orders.get(i).echoId == echoId) {
				return orders.get(i);
			}
		}
		return null;
	}

	public Restaurant findRestaurantById(int id) {
		for (int i = 0; i < restaurants.size(); i++) {
			if (restaurants.get(i).id == id) {
				return restaurants.get(i);
			}
		}
		return null;
	}

	public void populate() {
		restaurants = new ArrayList<Restaurant>();
		MongoCollection<Document> resCollection = mc.getDatabase("serve").getCollection("restaurants");
		ArrayList<JSONObject> resJson = new ArrayList<JSONObject>();
		resCollection.find().forEach((Document e) -> resJson.add(new JSONObject(e.toJson())));
		resJson.forEach(e -> {
			e = e.getJSONObject("restaurant");
			Restaurant temp = new Restaurant(e.getString("name"), Integer.parseInt(e.getString("id")));
			e.getJSONArray("items").forEach((Object x) -> temp.menu.addItem((((JSONObject) x).getString("name")),
					(((JSONObject) x).getDouble("cost"))));
			if (!restaurants.contains(temp))
				restaurants.add(temp);
		});
	}

	public JSONObject webCallPost(String endpoint, String body) throws ClientProtocolException, IOException {
		HttpClient httpclient = HttpClients.createDefault();
		System.out.println(endpoint);
		HttpPost httppost = new HttpPost("http://api.reimaginebanking.com" + endpoint + "?key=" + API_KEY);
		// Request parameters and other properties.
		httppost.addHeader("Content-Type", "application/json");
		httppost.addHeader("Accept", "application/json");
		httppost.setEntity(new StringEntity(body));
		// Execute and get the response.
		HttpResponse response = httpclient.execute(httppost);
		String r = EntityUtils.toString(response.getEntity());
		return new JSONObject(r);
	}

	public JSONObject webCallGet(String endpoint) throws Throwable {
		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet("http://api.reimaginebanking.com" + endpoint + "?key=" + API_KEY);
		// Request parameters and other properties.
		httpget.addHeader("Content-Type", "application/json");
		httpget.addHeader("Accept", "application/json");
		// Execute and get the response.
		HttpResponse response = httpclient.execute(httpget);
		String r = EntityUtils.toString(response.getEntity());
		return new JSONObject(r);
	}

	@Override
	public String toString() {
		return "RestaurantClient [restaurants=" + restaurants + "]";
	}

	public String getCustomerId() throws ClientProtocolException, IOException {
		JSONObject _json = this.webCallPost("/customers",
				"{\r\n" + "  \"first_name\": \"string\",\r\n" + "  \"last_name\": \"string\",\r\n"
						+ "  \"address\": {\r\n" + "    \"street_number\": \"string\",\r\n"
						+ "    \"street_name\": \"string\",\r\n" + "    \"city\": \"string\",\r\n"
						+ "    \"state\": \"nj\",\r\n" + "    \"zip\": \"08854\"\r\n" + "  }\r\n" + "}");
		return _json.getJSONObject("objectCreated").getString("_id");
	}

	public Object getAccountId(String id) throws ClientProtocolException, IOException {
		JSONObject _json = this.webCallPost("/customers/" + id + "/accounts", "{\r\n"
				+ "  \"type\": \"Savings\",\r\n" + "  \"nickname\": \"string\",\r\n" + "  \"rewards\": 0,\r\n"
				+ "  \"balance\": 0,\r\n" + "  \"account_number\": \"" + Integer.toString((int) (Math.random() * 10000))
				+ Integer.toString((int) (Math.random() * 10000)) + Integer.toString((int) (Math.random() * 10000))
				+ Integer.toString((int) (Math.random() * 10000)) + "\"\r\n" + "}");
		System.out.println(_json.toString());
		return _json.getJSONObject("objectCreated").getString("_id");
	}

	public String getTotalRevenue(String accountId) throws Throwable {
		JSONObject _json = this.webCallGet("/accounts/" + accountId);
		return _json.get("balance").toString();
	}

	public String getRestaurantData(String name) throws Throwable {
		FindIterable<Document> cursor = mc.getDatabase("serve").getCollection("restaurants")
				.find(new BasicDBObject("restaurant.name", name));
		String accountId = cursor.first().getString("accountId");
		return getTotalRevenue(accountId);
	}
}
