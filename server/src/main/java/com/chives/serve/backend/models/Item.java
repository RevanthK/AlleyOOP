package com.chives.serve.backend.models;

public class Item {

	public String name;
	public double price;

	public Item(String name, double price) {
		this.name = name;
		this.price = price;
	}

	@Override
	public String toString() {
		return "Item [name=" + name + ", price=" + price + "]";
	}
}
