package com.aiclothes.app.model;

public class ClothingRecommendation {
    private ClothingMatch ClothingMatch;
    
    public ClothingMatch getClothingMatch() {
        return ClothingMatch;
    }
    
    public void setClothingMatch(ClothingMatch clothingMatch) {
        ClothingMatch = clothingMatch;
    }
    
    public static class ClothingMatch {
        private String UpperGarment;
        private String Bottoms;
        
        public String getUpperGarment() {
            return UpperGarment;
        }
        
        public void setUpperGarment(String upperGarment) {
            UpperGarment = upperGarment;
        }
        
        public String getBottoms() {
            return Bottoms;
        }
        
        public void setBottoms(String bottoms) {
            Bottoms = bottoms;
        }
        
        // 生成推荐信息字符串
        public String getRecommendationText() {
            return String.format("👕 上衣：%s\n👖 裤子：%s", UpperGarment, Bottoms);
        }
    }
}