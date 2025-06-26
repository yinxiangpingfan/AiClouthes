package com.aiclothes.app.model;

import java.util.List;

public class CityData {
    private String province;
    private List<City> city;
    
    public String getProvince() {
        return province;
    }
    
    public void setProvince(String province) {
        this.province = province;
    }
    
    public List<City> getCity() {
        return city;
    }
    
    public void setCity(List<City> city) {
        this.city = city;
    }
    
    public static class City {
        private String adcode;
        private String name;
        
        public String getAdcode() {
            return adcode;
        }
        
        public void setAdcode(String adcode) {
            this.adcode = adcode;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    @Override
    public String toString() {
        return province;
    }
}