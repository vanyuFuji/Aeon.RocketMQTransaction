package org.example.model;

import java.util.List;

public class OrderModels {

    public static class Order {
        private String hkSaleType;
        private int saleType;
        private String orderNo;
        private String regionCode;
        private String corporationCode;
        private String storeCode;
        private double totalProductPrice;
        private double totalPayAmount;
        private double totalInvoiceAmount;
        private double bonusPointAmount;
        private double realPayAmount;
        private double totalGivePoint;
        private int givePointFlag;
        private int returnPointFlag;
        private int returnCouponFlag;
        private String orderTime;
        private String accountDate;
        private String cashierNo;
        private String posNo;
        private String receiptNo;
        private List<Item> itemList;
        private List<Payment> paymentList;
        private Object deliveryInfo;
        private int quicklyFlag;
        private String hkSourceId;
        private int historicalOrders;


        // Getters and Setters
        public String getHkSaleType() { return hkSaleType; }
        public void setHkSaleType(String hkSaleType) { this.hkSaleType = hkSaleType; }
        public int getSaleType() { return saleType; }
        public void setSaleType(int saleType) { this.saleType = saleType; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getRegionCode() { return regionCode; }
        public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
        public String getCorporationCode() { return corporationCode; }
        public void setCorporationCode(String corporationCode) { this.corporationCode = corporationCode; }
        public String getStoreCode() { return storeCode; }
        public void setStoreCode(String storeCode) { this.storeCode = storeCode; }
        public double getTotalProductPrice() { return totalProductPrice; }
        public void setTotalProductPrice(double totalProductPrice) { this.totalProductPrice = totalProductPrice; }
        public double getTotalPayAmount() { return totalPayAmount; }
        public void setTotalPayAmount(double totalPayAmount) { this.totalPayAmount = totalPayAmount; }
        public double getTotalInvoiceAmount() { return totalInvoiceAmount; }
        public void setTotalInvoiceAmount(double totalInvoiceAmount) { this.totalInvoiceAmount = totalInvoiceAmount; }
        public double getBonusPointAmount() { return bonusPointAmount; }
        public void setBonusPointAmount(double bonusPointAmount) { this.bonusPointAmount = bonusPointAmount; }
        public double getRealPayAmount() { return realPayAmount; }
        public void setRealPayAmount(double realPayAmount) { this.realPayAmount = realPayAmount; }
        public double getTotalGivePoint() { return totalGivePoint; }
        public void setTotalGivePoint(double totalGivePoint) { this.totalGivePoint = totalGivePoint; }
        public int getGivePointFlag() { return givePointFlag; }
        public void setGivePointFlag(int givePointFlag) { this.givePointFlag = givePointFlag; }
        public int getReturnPointFlag() { return returnPointFlag; }
        public void setReturnPointFlag(int returnPointFlag) { this.returnPointFlag = returnPointFlag; }
        public int getReturnCouponFlag() { return returnCouponFlag; }
        public void setReturnCouponFlag(int returnCouponFlag) { this.returnCouponFlag = returnCouponFlag; }
        public String getOrderTime() { return orderTime; }
        public void setOrderTime(String orderTime) { this.orderTime = orderTime; }
        public String getAccountDate() { return accountDate; }
        public void setAccountDate(String accountDate) { this.accountDate = accountDate; }
        public String getCashierNo() { return cashierNo; }
        public void setCashierNo(String cashierNo) { this.cashierNo = cashierNo; }
        public String getPosNo() { return posNo; }
        public void setPosNo(String posNo) { this.posNo = posNo; }
        public String getReceiptNo() { return receiptNo; }
        public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
        public List<Item> getItemList() { return itemList; }
        public void setItemList(List<Item> itemList) { this.itemList = itemList; }
        public List<Payment> getPaymentList() { return paymentList; }
        public void setPaymentList(List<Payment> paymentList) { this.paymentList = paymentList; }
        public Object getDeliveryInfo() { return deliveryInfo; }
        public void setDeliveryInfo(Object deliveryInfo) { this.deliveryInfo = deliveryInfo; }
        public int getQuicklyFlag() { return quicklyFlag; }
        public void setQuicklyFlag(int quicklyFlag) { this.quicklyFlag = quicklyFlag; }
        public String getHkSourceId() { return hkSourceId; }
        public void setHkSourceId(String hkSourceId) { this.hkSourceId = hkSourceId; }
        public int getHistoricalOrders() { return historicalOrders; }
        public void setHistoricalOrders(int historicalOrders) { this.historicalOrders = historicalOrders; }

    }

    public static class Payment {
        private String payCode;
        private String payName;
        private double payAmount;
        private double originalValue;
        private Object payNo;
        private String cardNo;
        private String deviceCode;
        private String orgNo;
        private String authCode;
        private double givePoint;
        private int payType;

        // Getters and Setters
        public String getPayCode() { return payCode; }
        public void setPayCode(String payCode) { this.payCode = payCode; }
        public String getPayName() { return payName; }
        public void setPayName(String payName) { this.payName = payName; }
        public double getPayAmount() { return payAmount; }
        public void setPayAmount(double payAmount) { this.payAmount = payAmount; }
        public double getOriginalValue() { return originalValue; }
        public void setOriginalValue(double originalValue) { this.originalValue = originalValue; }
        public Object getPayNo() { return payNo; }
        public void setPayNo(Object payNo) { this.payNo = payNo; }
         public String getCardNo() { return cardNo; }
        public void setCardNo(String cardNo) { this.cardNo = cardNo; }
        public String getDeviceCode() { return deviceCode; }
        public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
        public String getOrgNo() { return orgNo; }
        public void setOrgNo(String orgNo) { this.orgNo = orgNo; }
        public String getAuthCode() { return authCode; }
        public void setAuthCode(String authCode) { this.authCode = authCode; }
        public double getGivePoint() { return givePoint; }
        public void setGivePoint(double givePoint) { this.givePoint = givePoint; }
        public int getPayType() { return payType; }
        public void setPayType(int payType) { this.payType = payType; }
    }

    public static class Item {
        private int orderItemNo;
        private String skuName;

        private String itemCode;

        private String barcode;
        private double saleQty;
        private double salePrice;
        private double payAmount;
        private double bonusPointAmount;
        private double promoDiscount;
        private double couponDiscount;
        private double couponAmount;
        private double pointAmount;
        private double givePoint;

        private int itemType;
        private int saleType;

        private Object promoList;
        private Object couponList;
        private Object pointList;

        // Getters and Setters
        public int getOrderItemNo() { return orderItemNo; }
        public void setOrderItemNo(int orderItemNo) { this.orderItemNo = orderItemNo; }
           public String getSkuName() { return skuName; }
        public void setSkuName(String skuName) { this.skuName = skuName; }
           public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
         public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public double getSaleQty() { return saleQty; }
        public void setSaleQty(double saleQty) { this.saleQty = saleQty; }
        public double getSalePrice() { return salePrice; }
        public void setSalePrice(double salePrice) { this.salePrice = salePrice; }
        public double getPayAmount() { return payAmount; }
        public void setPayAmount(double payAmount) { this.payAmount = payAmount; }
        public double getBonusPointAmount() { return bonusPointAmount; }
        public void setBonusPointAmount(double bonusPointAmount) { this.bonusPointAmount = bonusPointAmount; }
        public double getPromoDiscount() { return promoDiscount; }
        public void setPromoDiscount(double promoDiscount) { this.promoDiscount = promoDiscount; }
        public double getCouponDiscount() { return couponDiscount; }
        public void setCouponDiscount(double couponDiscount) { this.couponDiscount = couponDiscount; }
        public double getCouponAmount() { return couponAmount; }
        public void setCouponAmount(double couponAmount) { this.couponAmount = couponAmount; }
        public double getPointAmount() { return pointAmount; }
        public void setPointAmount(double pointAmount) { this.pointAmount = pointAmount; }
        public double getGivePoint() { return givePoint; }
        public void setGivePoint(double givePoint) { this.givePoint = givePoint; }
         public int getItemType() { return itemType; }
        public void setItemType(int itemType) { this.itemType = itemType; }
        public int getSaleType() { return saleType; }
        public void setSaleType(int saleType) { this.saleType = saleType; }
           public Object getPromoList() { return promoList; }
        public void setPromoList(Object promoList) { this.promoList = promoList; }
        public Object getCouponList() { return couponList; }
        public void setCouponList(Object couponList) { this.couponList = couponList; }
        public Object getPointList() { return pointList; }
        public void setPointList(Object pointList) { this.pointList = pointList; }
    }
}