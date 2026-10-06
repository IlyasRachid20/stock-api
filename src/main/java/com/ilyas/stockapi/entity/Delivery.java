package com.ilyas.stockapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Where an online order goes, copied onto the order when it's placed. */
@Embeddable
public class Delivery {

    @Column(name = "delivery_name", length = 100)
    private String name;

    // Normalized, e.g. +212612345678 (see shop.Phones)
    @Column(name = "delivery_phone", length = 20)
    private String phone;

    @Column(name = "delivery_city", length = 80)
    private String city;

    @Column(name = "delivery_address", length = 255)
    private String address;

    @Column(name = "delivery_note", length = 500)
    private String note;

    protected Delivery() {
    }

    public Delivery(String name, String phone, String city, String address, String note) {
        this.name = name;
        this.phone = phone;
        this.city = city;
        this.address = address;
        this.note = note;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getCity() { return city; }
    public String getAddress() { return address; }
    public String getNote() { return note; }
}
