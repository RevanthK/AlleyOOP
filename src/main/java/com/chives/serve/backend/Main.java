package com.chives.serve.backend;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.post;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.chives.serve.backend.models.Item;
import com.chives.serve.backend.models.Order;
import com.chives.serve.backend.models.Restaurant;
import com.chives.serve.backend.processor.RestaurantClient;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class Main {

	public static void main(String[] args) throws Throwable {
		final MongoClient mongoClient = new MongoClient(
				new MongoClientURI("mongodb://admin:adminofthedatabase@ds149040.mlab.com:49040/serve"));
		final RestaurantClient client = new RestaurantClient(mongoClient);

		options("/*", (request, response) -> {

			String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
			if (accessControlRequestHeaders != null) {
				response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
			}

			String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
			if (accessControlRequestMethod != null) {
				response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
			}

			return "OK";
		});

		before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));

		get("/cartSum/:id/:echoId", (req, res) -> {
			try {
				JSONObject obj = new JSONObject();
				obj.put("name", client.findRestaurantById(Integer.parseInt(req.params("id"))).name);
				Order o = client.findOrderById(client.findRestaurantById(Integer.parseInt(req.params("id"))),
						Integer.parseInt(req.params("echoId")));
				JSONArray array = new JSONArray();

				List<Item> x = ((List<Item>) o.items.clone()).parallelStream().distinct().collect(Collectors.toList());
				x.forEach(e -> {
					JSONObject jo = new JSONObject();
					int quantity = 0;
					for (Item f : o.items) {
						if (f.name.equals(e.name))
							quantity++;
					}
					jo.put("name", e.name);
					jo.put("quantity", quantity);
					jo.put("price", e.price);
					jo.put("total",
							Double.parseDouble(new DecimalFormat("#.##").format(new Double(quantity * e.price))));
					array.put(jo);
				});

				obj.put("items", array);
				obj.put("total_items", x.size());
				obj.put("subtotal", o.subtotal);
				System.err.println("Screen Update: " + obj.toString());
				return obj.toString();
			} catch (Throwable t) {
				JSONObject errorObj = new JSONObject();
				errorObj.put("status", "invalid order id");
				System.err.println("Screen Update: " + errorObj);
				return errorObj;
			}
		});

		get("/menu/:id", (req, res) -> {
			String id = req.params("id");
			Restaurant r = client.findRestaurantById(Integer.parseInt(id));
			return r.menu.itemList();
		});

		get("/restaurant/:name", (req, res) -> {
			String name = req.params("name");
			try {
				return client.getRestaurantData(name);
			} catch (Throwable e) { 
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "invalid restaurant";
		});

		post("/order", (req, res) -> {
			String response = req.body();
			String var1, var2, var3, var4;
			System.out.println("Incoming Request: " + response);
			var1 = response.substring(response.indexOf("=") + 1, response.indexOf("&"));
			response = response.substring(response.indexOf("&") + 1);
			var2 = response.substring(response.indexOf("=") + 1, response.indexOf("&"));
			response = response.substring(response.indexOf("&") + 1);
			var3 = response.substring(response.indexOf("=") + 1, response.indexOf("&"));
			response = response.substring(response.indexOf("&") + 1);
			var4 = response.substring(response.indexOf("=") + 1);
			Order o = client.placeOrder(var2, var1, var3, var4);

			System.out.println("Current Order: " + o);

			return Double.parseDouble(new DecimalFormat("#.##").format(new Double(o.subtotal * 1.07)));
		});

		post("/restaurant", (req, res) -> {
    String customerId = client.getCustomerId();
    Document d = new Document("restaurant", BasicDBObject.parse(req.body()));
    d.append("customerId", customerId);
    d.append("accountId", client.getAccountId(customerId));
			mongoClient.getDatabase("serve").getCollection("restaurants")
					.insertOne(d);
			client.populate();
			return mongoClient.getDatabase("serve").getCollection("restaurants").count();
		});
	}

	
}
