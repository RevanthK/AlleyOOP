package com.chives.serve.backend.models;

import java.util.ArrayList;

public class Menu {

	public ArrayList<Item> items;

	@Override
	public String toString() {
		return "Menu [items=" + items + "]";
	}

	public Menu() {
		items = new ArrayList<Item>();
	}

	public void addItem(String name, double price) {
		items.add(new Item(name, price));
	}

	public Item getItem(String name) {
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).name.equals(name)) {
				return items.get(i);
			}
		}
		return null;
	}

	public String itemList() {
		String list = "";
		for (int i = 0; i < items.size(); i++) {
			list += items.get(i).name;
			list += ":";
		}
		list = list.substring(0, list.length() - 1);
		return list;
	}

}
