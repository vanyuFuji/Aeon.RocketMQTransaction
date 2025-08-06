package org.example;

import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class TransactionProducer {
    public static class RocketMQConfig {
        private String namesrvAddr;
        private String producerGroup;
        private String topic;
        private String tag;
        private int retryTimes;
        private int sendTimeout;

        // Getters
        public String getNamesrvAddr() { return namesrvAddr; }
        public String getProducerGroup() { return producerGroup; }
        public String getTopic() { return topic; }
        public String getTag() { return tag; }
        public int getRetryTimes() { return retryTimes; }
        public int getSendTimeout() { return sendTimeout; }
    }

    public static void main(String[] args) throws Exception {
        // Load configuration from config.json
        Gson gson = new Gson();
        RocketMQConfig config;
        try (InputStream inputStream = TransactionProducer.class.getClassLoader().getResourceAsStream("config.json");
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject rocketMQConfig = jsonObject.getAsJsonObject("rocketmq");
            config = gson.fromJson(rocketMQConfig, RocketMQConfig.class);
        } catch (Exception e) {
            System.err.println("Failed to load config.json: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Log the loaded JSON settings
        System.out.println("Loaded RocketMQ Config:");
        System.out.println("namesrvAddr: " + config.getNamesrvAddr());
        System.out.println("producerGroup: " + config.getProducerGroup());
        System.out.println("topic: " + config.getTopic());
        System.out.println("tag: " + config.getTag());
        System.out.println("retryTimes: " + config.getRetryTimes());
        System.out.println("sendTimeout: " + config.getSendTimeout());

        // Configure producer
        TransactionMQProducer producer = new TransactionMQProducer(config.getProducerGroup());
        producer.setNamesrvAddr(config.getNamesrvAddr());
        producer.setRetryTimesWhenSendFailed(config.getRetryTimes());
        producer.setSendMsgTimeout(config.getSendTimeout());
        producer.setVipChannelEnabled(false);

        // Optional: If ACL is enabled
        // producer.setAclEnable(true);
        // producer.setAccessKey("YourAccessKey");
        // producer.setSecretKey("YourSecretKey");

        // Set transaction listener
        producer.setTransactionListener(new TransactionListenerImpl());

        // Start producer
        try {
            producer.start();
            System.out.println("Producer started successfully");
        } catch (Exception e) {
            System.err.println("Failed to start producer: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Build payload from objects
        OrderModels.Order order = new OrderModels.Order();
        // Generate orderNo as Unix timestamp padded to 32 characters
        long timestamp = System.currentTimeMillis();
        String orderNo = String.format("%032d", timestamp);
        double price = 15.9;
        // Format orderTime as YYYYMMddHH:mm:ss
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH:mm:ss");
        SimpleDateFormat sdfAccountDate = new SimpleDateFormat("yyyyMMdd");
        String orderTime = sdf.format(new java.util.Date(timestamp));
        String accountDate = sdfAccountDate.format(new java.util.Date(timestamp));
        String cashierNo = "7777"; //posNo 7778
        String cardNo = "12345678"; //octopus no
        String devid = "586ADF"; //octopus devid
        String spid = "123456"; //octopus spid
        String storecode = "210";
        String barcode = storecode; //storecode + sku + checkdigits

        order.setHkSaleType("1");
        order.setSaleType(1);
        order.setOrderNo(orderNo);
        order.setRegionCode("002");
        order.setCorporationCode("601");
        order.setStoreCode(storecode);
        order.setTotalProductPrice(price);
        order.setTotalPayAmount(price);
        order.setTotalInvoiceAmount(0);
        order.setBonusPointAmount(0);
        order.setRealPayAmount(price);
        order.setTotalGivePoint(0);
        order.setGivePointFlag(0);
        order.setReturnPointFlag(0);
        order.setReturnCouponFlag(0);
        order.setOrderTime(orderTime);
        order.setAccountDate(accountDate);
        order.setCashierNo(cashierNo);
        order.setPosNo(cashierNo);
        order.setReceiptNo(cashierNo);
        order.setQuicklyFlag(0);
        order.setHkSourceId("90");
        order.setHistoricalOrders(0);
        order.setDeliveryInfo(null);

        // Payment list
        List<OrderModels.Payment> paymentList = new ArrayList<>();
        OrderModels.Payment payment = new OrderModels.Payment();
        if (price < 2.0) {
            payment.setPayCode("VM1");
            payment.setPayName("VM OCTOPUS 1 SALES");
        } else if (price >= 2.0 && price <= 10.0) {
            payment.setPayCode("VM2");
            payment.setPayName("VM OCTOPUS 2 SALES");
        } else {
            payment.setPayCode("VM3");
            payment.setPayName("VM OCTOPUS 3 SALES");
        }
        payment.setPayAmount(price);
        payment.setOriginalValue(0);
        payment.setPayNo(null);
        payment.setCardNo(cardNo);
        payment.setDeviceCode(devid);
        payment.setOrgNo(spid);
        payment.setAuthCode(spid);
        payment.setGivePoint(0);
        payment.setPayType(0);
        paymentList.add(payment);
        order.setPaymentList(paymentList);

        // Item list
        List<OrderModels.Item> itemList = new ArrayList<>();
        OrderModels.Item item = new OrderModels.Item();
        item.setOrderItemNo(1);
        item.setSkuName("TVHC塑膠圓形水樽500毫升灰色(20/80)");
        item.setItemCode("090000035"); //9 digits
        item.setBarcode(barcode+item.getItemCode()+"1");
        item.setSaleQty(1);
        item.setSalePrice(price);
        item.setPayAmount(price);
        item.setBonusPointAmount(0);
        item.setPromoDiscount(0);
        item.setCouponDiscount(0);
        item.setCouponAmount(0);
        item.setPointAmount(0);
        item.setGivePoint(0);
        item.setItemType(0);
        item.setSaleType(0);
        item.setPromoList(null);
        item.setCouponList(null);
        item.setPointList(null);
        itemList.add(item);
        order.setItemList(itemList);

        // Serialize to JSON
        String transactionPayload = gson.toJson(order);

        Message msg = new Message(
                config.getTopic(),
                config.getTag(),
                "00103071750916428885",
                transactionPayload.getBytes("UTF-8")
        );
        msg.putUserProperty("OrderSource", "POS");

        try {
            // Send transactional message
            TransactionSendResult sendResult = producer.sendMessageInTransaction(msg, null);
            System.out.printf("Message sent: ID=%s, Status=%s%n",
                    sendResult.getMsgId(), sendResult.getLocalTransactionState());
        } catch (Exception e) {
            System.err.println("Send failed: " + e.getMessage());
            e.printStackTrace();
        }

        Thread.sleep(10000);
        producer.shutdown();
    }

    static class TransactionListenerImpl implements TransactionListener {
        @Override
        public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
            System.out.println("Executing local transaction for msg: " + new String(msg.getBody()));
            try {
                // Simulate business logic (e.g., validate payload)
                return LocalTransactionState.COMMIT_MESSAGE;
            } catch (Exception e) {
                System.err.println("Transaction error: " + e.getMessage());
                e.printStackTrace();
                return LocalTransactionState.UNKNOW;
            }
        }

        @Override
        public LocalTransactionState checkLocalTransaction(MessageExt msg) {
            System.out.println("Checking transaction for msg: " + msg.getMsgId());
            // Simulate check (e.g., verify business logic completion)
            return LocalTransactionState.COMMIT_MESSAGE;
        }
    }
}