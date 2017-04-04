package com.chives.serve.backend.models;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Order {

	public Restaurant restaurant;
	public ArrayList<Item> items;
	public int echoId = -1;
	public double subtotal;
	
	public Order(Restaurant restaurant, int echoId) {
		this.restaurant = restaurant;
		this.echoId = echoId;
		items = new ArrayList<Item>();
		subtotal = 0;
	}

	public void add(String name) {
		Item item = restaurant.order(name);
		items.add(item);
		subtotal += item.price;
	}
	
	public String finish(){
		String order = items.toString();
		order += " Subtotal: $" + subtotal;
		return order;
	}

	@Override
	public String toString() {
		return "Order [restaurant=" + restaurant + ", items=" + items + ", custId=" + echoId + ", subtotal=" + subtotal
				+ "]";
	}
}
