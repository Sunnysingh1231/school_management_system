package com.example.demo.model;

public class DepthLevel {

	private double price;
	private long quantity;

	public DepthLevel() {
	}

	public DepthLevel(double price, long quantity) {
		this.price = price;
		this.quantity = quantity;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public long getQuantity() {
		return quantity;
	}

	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}
}