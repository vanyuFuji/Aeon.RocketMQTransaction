package org.example.model;

import java.sql.Timestamp;

public class VBeTransaction {
    private Long id;
    private Timestamp time;
    private String region;
    private String locationName;
    private String machineName;
    private String machineNumber;
    private String devid;
    private String slot;
    private String productName;
    private String barCode;
    private String productTypeName;
    private Double amount;
    private String payType;
    private String payTransactionId;
    private String payUserId;
    private String payOutTradeNo;
    private Boolean isProcessed;
    private Timestamp processedDateTime;
    private String processedId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Timestamp getTime() { return time; }
    public void setTime(Timestamp time) { this.time = time; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public String getMachineNumber() { return machineNumber; }
    public void setMachineNumber(String machineNumber) { this.machineNumber = machineNumber; }
    public String getDevid() { return devid; }
    public void setDevid(String devid) { this.devid = devid; }
    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBarCode() { return barCode; }
    public void setBarCode(String barCode) { this.barCode = barCode; }
    public String getProductTypeName() { return productTypeName; }
    public void setProductTypeName(String productTypeName) { this.productTypeName = productTypeName; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public String getPayTransactionId() { return payTransactionId; }
    public void setPayTransactionId(String payTransactionId) { this.payTransactionId = payTransactionId; }
    public String getPayUserId() { return payUserId; }
    public void setPayUserId(String payUserId) { this.payUserId = payUserId; }
    public String getPayOutTradeNo() { return payOutTradeNo; }
    public void setPayOutTradeNo(String payOutTradeNo) { this.payOutTradeNo = payOutTradeNo; }
    public Boolean getIsProcessed() { return isProcessed; }
    public void setIsProcessed(Boolean isProcessed) { this.isProcessed = isProcessed; }
    public Timestamp getProcessedDateTime() { return processedDateTime; }
    public void setProcessedDateTime(Timestamp processedDateTime) { this.processedDateTime = processedDateTime; }
    public String getProcessedId() { return processedId; }
    public void setProcessedId(String processedId) { this.processedId = processedId; }
}