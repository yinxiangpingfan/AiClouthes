package com.aiclothes.app.model;

import java.util.List;

public class WeatherData {
    private String status;
    private String count;
    private String info;
    private String infocode;
    private List<Forecast> forecasts;
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCount() {
        return count;
    }
    
    public void setCount(String count) {
        this.count = count;
    }
    
    public String getInfo() {
        return info;
    }
    
    public void setInfo(String info) {
        this.info = info;
    }
    
    public String getInfocode() {
        return infocode;
    }
    
    public void setInfocode(String infocode) {
        this.infocode = infocode;
    }
    
    public List<Forecast> getForecasts() {
        return forecasts;
    }
    
    public void setForecasts(List<Forecast> forecasts) {
        this.forecasts = forecasts;
    }
    
    public static class Forecast {
        private String city;
        private String adcode;
        private String province;
        private String reporttime;
        private List<Cast> casts;
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getAdcode() {
            return adcode;
        }
        
        public void setAdcode(String adcode) {
            this.adcode = adcode;
        }
        
        public String getProvince() {
            return province;
        }
        
        public void setProvince(String province) {
            this.province = province;
        }
        
        public String getReporttime() {
            return reporttime;
        }
        
        public void setReporttime(String reporttime) {
            this.reporttime = reporttime;
        }
        
        public List<Cast> getCasts() {
            return casts;
        }
        
        public void setCasts(List<Cast> casts) {
            this.casts = casts;
        }
    }
    
    public static class Cast {
        private String date;
        private String week;
        private String dayweather;
        private String nightweather;
        private String daytemp;
        private String nighttemp;
        private String daywind;
        private String nightwind;
        private String daypower;
        private String nightpower;
        private String daytemp_float;
        private String nighttemp_float;
        
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public String getWeek() {
            return week;
        }
        
        public void setWeek(String week) {
            this.week = week;
        }
        
        public String getDayweather() {
            return dayweather;
        }
        
        public void setDayweather(String dayweather) {
            this.dayweather = dayweather;
        }
        
        public String getNightweather() {
            return nightweather;
        }
        
        public void setNightweather(String nightweather) {
            this.nightweather = nightweather;
        }
        
        public String getDaytemp() {
            return daytemp;
        }
        
        public void setDaytemp(String daytemp) {
            this.daytemp = daytemp;
        }
        
        public String getNighttemp() {
            return nighttemp;
        }
        
        public void setNighttemp(String nighttemp) {
            this.nighttemp = nighttemp;
        }
        
        public String getDaywind() {
            return daywind;
        }
        
        public void setDaywind(String daywind) {
            this.daywind = daywind;
        }
        
        public String getNightwind() {
            return nightwind;
        }
        
        public void setNightwind(String nightwind) {
            this.nightwind = nightwind;
        }
        
        public String getDaypower() {
            return daypower;
        }
        
        public void setDaypower(String daypower) {
            this.daypower = daypower;
        }
        
        public String getNightpower() {
            return nightpower;
        }
        
        public void setNightpower(String nightpower) {
            this.nightpower = nightpower;
        }
        
        public String getDaytemp_float() {
            return daytemp_float;
        }
        
        public void setDaytemp_float(String daytemp_float) {
            this.daytemp_float = daytemp_float;
        }
        
        public String getNighttemp_float() {
            return nighttemp_float;
        }
        
        public void setNighttemp_float(String nighttemp_float) {
            this.nighttemp_float = nighttemp_float;
        }
        
        // 生成天气描述字符串
        public String getWeatherDescription() {
            return String.format("白天%s，%s风%s级，最高气温%s℃；夜间%s，%s风%s级，最低气温%s℃。",
                    dayweather, daywind, daypower, daytemp,
                    nightweather, nightwind, nightpower, nighttemp);
        }
    }
}