package com.chives.serve.backend.models;

import java.util.ArrayList;

public class Restaurant {
	public Menu menu;
	public String name;
	public int id;
	public ArrayList<Order> orders;

	public Restaurant(String name, int id) {
		orders = new ArrayList<Order>();
		menu = new Menu();
		this.id = id;
		this.name = name;
	}

	public Item order(String name) {
		return menu.getItem(name);
	}

	public void addOrder(Order o) {
		orders.add(o);
	}
	
	public Order removeOrder(Order o){
		orders.remove(o);
		return o;
	}
	
	@Override
	public String toString() {
		return "Restaurant [menu=" + menu + ", name=" + name + ", id=" + id + ", orders="  + "]";
	}

}
