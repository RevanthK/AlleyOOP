package com.chives.serve.backend.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.json.JSONObject;

import com.chives.serve.backend.models.Order;
import com.chives.serve.backend.models.Restaurant;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
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
			BasicDBObject query = new BasicDBObject();
			query.put("id", id);
			FindIterable<Document> cursor = mc.getDatabase("serve").getCollection("restaurants").find(query);
			String accountId = "";
			if (cursor != null){
				accountId = cursor.first().get("accountId").toString();
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
			//deposit(accountId, order.subtotal);
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
		String body = "{\r\n" + 
				"				  \"medium\": \"balance\",\r\n" + 
				"				  \"transaction_date\": \"2017-04-02\",\r\n" + 
				"				  \"amount\":" + subtotal+ ",\r\n" + 
				"				  \"description\": \"order\"\r\n" + 
				"				}";
		this.webCallPost("accounts/" + accountId + "/deposit", body);
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
			Restaurant temp = new Restaurant(e.getString("name"), e.getInt("id"));
			e.getJSONArray("items").forEach((Object x) -> temp.menu.addItem((((JSONObject) x).getString("name")),
					(((JSONObject) x).getDouble("cost"))));
			if (!restaurants.contains(temp))
				restaurants.add(temp);
		});
	}

	public JSONObject webCallPost(String endpoint, String body) throws ClientProtocolException, IOException {
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost("http://api.reimaginebanking.com" + endpoint + "?key=" + API_KEY);
		// Request parameters and other properties.
		httppost.addHeader("Content-Type","application/json");
		httppost.addHeader("Accept","application/json");
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
		httpget.addHeader("Content-Type","application/json");
		httpget.addHeader("Accept","application/json");
		// Execute and get the response.
		HttpResponse response = httpclient.execute(httpget);
		String r = EntityUtils.toString(response.getEntity());
		return new JSONObject(r);
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException{
		RestaurantClient a = new RestaurantClient(new MongoClient(
				new MongoClientURI("mongodb://admin:adminofthedatabase@ds149040.mlab.com:49040/serve")));
		System.out.println(a.getAccountId(a.getCustomerId()));
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
		JSONObject _json = this.webCallPost("/customers/" + id + "/accounts", "{\r\n" + "  \"type\": \"Credit Card\",\r\n"
				+ "  \"nickname\": \"string\",\r\n" + "  \"rewards\": 0,\r\n" + "  \"balance\": 0,\r\n"
				+ "  \"account_number\": \"" + Integer.toString((int) (Math.random() * 10000))
				+ Integer.toString((int) (Math.random() * 10000)) + Integer.toString((int) (Math.random() * 10000))
				+ Integer.toString((int) (Math.random() * 10000)) + "\"\r\n" + "}");
		return _json.getJSONObject("objectCreated").getString("_id");
	}
	
	public String getTotalRevenue(String accountId) throws Throwable{
		JSONObject _json = this.webCallGet("/accounts/" + accountId);
		return _json.getString("balance");
	}

	public String getRestaurantData(String name) throws Throwable {
		BasicDBObject query = new BasicDBObject();
		query.put("name", name);
		FindIterable<Document> cursor = mc.getDatabase("serve").getCollection("restaurants").find(query);
		String accountId = cursor.first().get("accountId").toString();
		return getTotalRevenue(accountId);
	}
}
