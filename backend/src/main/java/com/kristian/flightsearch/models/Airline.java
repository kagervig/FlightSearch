package com.kristian.flightsearch.models;

public class Airline {
    private String name;
    private String country;
    private boolean budgetCarrier; // false = legacy carrier
    private String code; // flight number prefix
    private String[] hubs; // could be 1 hub, could be many
    private boolean longHaul;
    private boolean mediumHaul;
    private boolean shortHaul;
    private int destinations;

    public Airline(String name, String country, String code, boolean budgetCarrier, String[] hubs, boolean longHaul,
            boolean mediumHaul, boolean shortHaul, int destinations) {
        this.name = name;
        this.country = country;
        this.code = code;
        this.budgetCarrier = budgetCarrier;
        this.hubs = hubs;
        this.longHaul = longHaul;
        this.mediumHaul = mediumHaul;
        this.shortHaul = shortHaul;
        this.destinations = destinations;
    }

    //The constructor Airline(String, String, boolean, String, String, boolean, boolean, boolean, int) is undefined


    //overloaded constructor which parses airline hub data from file
    public Airline(String country, String code, boolean budgetCarrier, String name, String hubString, boolean longHaul,
            boolean mediumHaul, boolean shortHaul, int destinations) {
        this.name = name;
        this.country = country;
        this.code = code;
        this.budgetCarrier = budgetCarrier;

        // Parse hubString into hubs[]
        if (hubString != null && !hubString.isEmpty()) {
            String[] parsedHubs = hubString.split(":");
            this.hubs = new String[parsedHubs.length];
            for (int i = 0; i < parsedHubs.length; i++) {
                this.hubs[i] = parsedHubs[i];
            }
        } else {
            this.hubs = new String[] { "NONE" };
        }
        this.longHaul = longHaul;
        this.mediumHaul = mediumHaul;
        this.shortHaul = shortHaul;
        this.destinations = destinations;
    }

    public Airline() {
        this.name = "Error";
        this.country = "Nowhere";
        this.code = "ZZ";
        this.budgetCarrier = true;
        this.hubs = new String[] { "NONE" };
        this.longHaul = false;
        this.mediumHaul = false;
        this.shortHaul = false;
        this.destinations = 0;
    }

    public String getName() {
        return this.name;
    }

    public String getCountry() {
        return this.country;
    }

    public String getCode() {
        return this.code;
    }

    public boolean isBudgetCarrier() {
        return this.budgetCarrier;
    }

    public String[] getHubs() {
        return this.hubs;
    }

    public boolean isLongHaul() {
        return this.longHaul;
    }

    public boolean isMediumHaul() {
        return this.mediumHaul;
    }

    public boolean isShortHaul() {
        return this.shortHaul;
    }

    public int getDestinations() {
        return this.destinations;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setBudgetCarrier(boolean budgetCarrier) {
        this.budgetCarrier = budgetCarrier;
    }

    public void setHubs(String[] hubs) {
        this.hubs = hubs;
    }

    public void setLongHaul(boolean longHaul) {
        this.longHaul = longHaul;
    }

    public void setMediumHaul(boolean mediumHaul) {
        this.mediumHaul = mediumHaul;
    }

    public void setShortHaul(boolean shortHaul) {
        this.shortHaul = shortHaul;
    }

    public void setDestinations(int destinations) {
        this.destinations = destinations;
    }
}
