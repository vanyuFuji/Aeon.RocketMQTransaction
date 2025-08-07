package org.example;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.example.mapper.VBeTransactionMapper;
import org.example.model.OrderModels;
import org.example.model.VBeTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class TransactionProducer {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProducer.class);

    public static class RocketMQConfig {
        private String namesrvAddr;
        private String producerGroup;
        private String topic;
        private String tag;
        private int retryTimes;
        private int sendTimeout;

        // Getters
        public String getNamesrvAddr() {
            return namesrvAddr;
        }

        public String getProducerGroup() {
            return producerGroup;
        }

        public String getTopic() {
            return topic;
        }

        public String getTag() {
            return tag;
        }

        public int getRetryTimes() {
            return retryTimes;
        }

        public int getSendTimeout() {
            return sendTimeout;
        }
    }

    public static class AppConfig {
        private long sendInterval;

        // Getter
        public long getSendInterval() {
            return sendInterval;
        }
    }

    private static int calculateEAN13CheckDigit(String barcode) {
        if (barcode == null || barcode.length() != 12) {
            throw new IllegalArgumentException("Barcode must be 12 digits");
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(barcode.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int mod = sum % 10;
        return (mod == 0) ? 0 : 10 - mod;
    }

    public static void main(String[] args) throws Exception {
        // Load configuration from config.json in the same directory
        Gson gson = new Gson();

        RocketMQConfig rocketMQConfig;
        AppConfig appConfig;

        java.nio.file.Path configPath = java.nio.file.Paths.get("config.json");
        if (!Files.exists(configPath)) {
            logger.error("config.json not found in the same directory: {}", configPath.toAbsolutePath());
            throw new java.io.FileNotFoundException("Error: config.json not found in the same directory as the application");
        }
        try (InputStream inputStream = Files.newInputStream(configPath);
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            rocketMQConfig = gson.fromJson(jsonObject.getAsJsonObject("rocketmq"), RocketMQConfig.class);
            appConfig = gson.fromJson(jsonObject.getAsJsonObject("app"), AppConfig.class);
            logger.info("Loaded RocketMQ Config from: {}", configPath.toAbsolutePath());
            logger.debug("namesrvAddr: {}", rocketMQConfig.getNamesrvAddr());
            logger.debug("producerGroup: {}", rocketMQConfig.getProducerGroup());
            logger.debug("topic: {}", rocketMQConfig.getTopic());
            logger.debug("tag: {}", rocketMQConfig.getTag());
            logger.debug("retryTimes: {}", rocketMQConfig.getRetryTimes());
            logger.debug("sendTimeout: {}", rocketMQConfig.getSendTimeout());
            logger.info("Loaded App Config: sendInterval={}ms", appConfig.getSendInterval());
        } catch (Exception e) {
            logger.error("Failed to load config.json: {}", e.getMessage(), e);
            throw e;
        }

//        // Configure RocketMQ producer
//        TransactionMQProducer producer = new TransactionMQProducer(rocketMQConfig.getProducerGroup());
//        producer.setNamesrvAddr(rocketMQConfig.getNamesrvAddr());
//        producer.setRetryTimesWhenSendFailed(rocketMQConfig.getRetryTimes());
//        producer.setSendMsgTimeout(rocketMQConfig.getSendTimeout());
//        producer.setVipChannelEnabled(false);
//        producer.setTransactionListener(new TransactionListenerImpl());
//        try {
//            producer.start();
//            logger.info("Producer started successfully");
//        } catch (Exception e) {
//            logger.error("Failed to start producer: {}", e.getMessage(), e);
//            throw e;
//        }

        // Initialize MyBatis
        SqlSessionFactory sqlSessionFactory;
        try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            logger.info("Initialized MyBatis SqlSessionFactory");
        }

        // Configure RocketMQ producer
        TransactionMQProducer producer = new TransactionMQProducer(rocketMQConfig.getProducerGroup());
        producer.setNamesrvAddr(rocketMQConfig.getNamesrvAddr());
        producer.setRetryTimesWhenSendFailed(rocketMQConfig.getRetryTimes());
        producer.setSendMsgTimeout(rocketMQConfig.getSendTimeout());
        producer.setVipChannelEnabled(false);
        producer.setTransactionListener(new TransactionListenerImpl());
        try {
            producer.start();
            logger.info("Producer started successfully");
        } catch (Exception e) {
            logger.error("Failed to start producer: {}", e.getMessage(), e);
            throw e;
        }

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            producer.shutdown();
            logger.info("Producer shut down via shutdown hook");
        }));

        // Worker loop: poll database at intervals
        while (true) {
            try (SqlSession session = sqlSessionFactory.openSession()) {
                VBeTransactionMapper mapper = session.getMapper(VBeTransactionMapper.class);
                List<VBeTransaction> transactions = null;
                try {
                    logger.debug("Polling V_BE_Transaction");
                    transactions = mapper.selectAll();
                    logger.debug("Queried V_BE_Transaction, found {} rows", transactions != null ? transactions.size() : 0);
                    if (transactions == null || transactions.isEmpty()) {
                        logger.info("No unprocessed data found in V_BE_Transaction");
                    } else {
                        for (VBeTransaction transaction : transactions) {
                            // Log raw row data
                            logger.info("Raw row data: Time={}, region={}, locationName={}, machineName={}, machineNumber={}, " +
                                            "Devid={}, slot={}, productName={}, barCode={}, ProductTypeName={}, Amount={}, PayType={}, " +
                                            "PayTransactionId={}, PayUserID={}, PayOutTradeNo={}",
                                    transaction.getTime(), transaction.getRegion(), transaction.getLocationName(),
                                    transaction.getMachineName(), transaction.getMachineNumber(), transaction.getDevid(),
                                    transaction.getSlot(), transaction.getProductName(), transaction.getBarCode(),
                                    transaction.getProductTypeName(), transaction.getAmount(), transaction.getPayType(),
                                    transaction.getPayTransactionId(), transaction.getPayUserId(), transaction.getPayOutTradeNo());

                            // Map to Order
//                            OrderModels.Order order = new OrderModels.Order();
//                            String orderNo = transaction.getPayTransactionId() != null ? transaction.getPayTransactionId() :
//                                    String.format("%032d", System.currentTimeMillis());
//                            double price = transaction.getAmount() != null ? transaction.getAmount() : 0.0;
//                            String cashierNo = transaction.getMachineName();
//                            String cardNo = transaction.getPayUserId();
//                            String devid = transaction.getDevid();
//                            String orgNo = "19266";
//                            String authcode = transaction.getPayOutTradeNo();
//                            String storecode = transaction.getLocationName() != null ?
//                                    transaction.getLocationName().replaceAll("\\s+", "") : "UnknownStore";
//                            String itemcode = transaction.getBarCode();
//                            String skuname = transaction.getProductName() != null ?
//                                    transaction.getProductName() : "Unknown Product";
//                            String orderTime = transaction.getTime() != null ?
//                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(transaction.getTime()) : "";
//                            String accountDate = transaction.getTime() != null ?
//                                    new SimpleDateFormat("yyyyMMdd").format(transaction.getTime()) : "";
//
//                            String barcode = itemcode != null ? itemcode : storecode + "000000000";
//                            if (barcode.length() >= 12) {
//                                barcode = barcode.substring(0, 12);
//                            } else {
//                                barcode = String.format("%12s", barcode).replace(' ', '0');
//                            }
//                            int checkDigit = calculateEAN13CheckDigit(barcode);
//                            barcode = barcode + checkDigit;
//
//                            order.setHkSaleType("1");
//                            order.setSaleType(1);
//                            order.setOrderNo(orderNo);
//                            order.setRegionCode(transaction.getRegion() != null ?
//                                    transaction.getRegion().replaceAll("\\s+", "") : "UnknownRegion");
//                            order.setCorporationCode("601");
//                            order.setStoreCode(storecode);
//                            order.setTotalProductPrice(price);
//                            order.setTotalPayAmount(price);
//                            order.setTotalInvoiceAmount(0);
//                            order.setBonusPointAmount(0);
//                            order.setRealPayAmount(price);
//                            order.setTotalGivePoint(0);
//                            order.setGivePointFlag(0);
//                            order.setReturnPointFlag(0);
//                            order.setReturnCouponFlag(0);
//                            order.setOrderTime(orderTime);
//                            order.setAccountDate(accountDate);
//                            order.setCashierNo(cashierNo);
//                            order.setPosNo(cashierNo);
//                            order.setReceiptNo(cashierNo);
//                            order.setQuicklyFlag(0);
//                            order.setHkSourceId("90");
//                            order.setHistoricalOrders(0);
//                            order.setDeliveryInfo(null);
//
//                            // Payment list
//                            List<OrderModels.Payment> paymentList = new ArrayList<>();
//                            OrderModels.Payment payment = new OrderModels.Payment();
//                            String payType = transaction.getPayType();
//                            if (price < 2.0) {
//                                payment.setPayCode("VM1");
//                                payment.setPayName("VM OCTOPUS 1 SALES");
//                            } else if (price >= 2.0 && price <= 10.0) {
//                                payment.setPayCode("VM2");
//                                payment.setPayName("VM OCTOPUS 2 SALES");
//                            } else {
//                                payment.setPayCode("VM3");
//                                payment.setPayName("VM OCTOPUS 3 SALES");
//                            }
//                            payment.setPayAmount(price);
//                            payment.setOriginalValue(0);
//                            payment.setPayNo(null);
//                            payment.setCardNo(cardNo);
//                            payment.setDeviceCode(devid);
//                            payment.setOrgNo(orgNo);
//                            payment.setAuthCode(authcode);
//                            payment.setGivePoint(0);
//                            payment.setPayType(payType != null && payType.equals("Octopus") ? 0 : 1);
//                            paymentList.add(payment);
//                            order.setPaymentList(paymentList);
//
//                            // Item list
//                            List<OrderModels.Item> itemList = new ArrayList<>();
//                            OrderModels.Item item = new OrderModels.Item();
//                            item.setOrderItemNo(1);
//                            item.setSkuName(skuname);
//                            item.setItemCode(itemcode != null ? itemcode : "000000000");
//                            item.setBarcode(barcode);
//                            item.setSaleQty(1);
//                            item.setSalePrice(price);
//                            item.setPayAmount(price);
//                            item.setBonusPointAmount(0);
//                            item.setPromoDiscount(0);
//                            item.setCouponDiscount(0);
//                            item.setCouponAmount(0);
//                            item.setPointAmount(0);
//                            item.setGivePoint(0);
//                            item.setItemType(0);
//                            item.setSaleType(0);
//                            item.setPromoList(null);
//                            item.setCouponList(null);
//                            item.setPointList(null);
//                            itemList.add(item);
//                            order.setItemList(itemList);

                            // Serialize to JSON
//                            String transactionPayload = gson.toJson(order);
//                            logger.debug("Serialized order payload: {}", transactionPayload);
//                            logger.info("Serialized order payload: {}", transactionPayload);

                            // Send message
//                            Message msg = new Message(
//                                    rocketMQConfig.getTopic(),
//                                    rocketMQConfig.getTag(),
//                                    orderNo,
//                                    transactionPayload.getBytes("UTF-8")
//                            );
//                            msg.putUserProperty("OrderSource", "POS");

//                            try {
//                                TransactionSendResult sendResult = producer.sendMessageInTransaction(msg, null);
//                                logger.info("Message sent: ID={}, Status={}", sendResult.getMsgId(), sendResult.getLocalTransactionState());
//                                // Mark as processed
////                                try (SqlSession updateSession = sqlSessionFactory.openSession()) {
////                                    VBeTransactionMapper updateMapper = updateSession.getMapper(VBeTransactionMapper.class);
////                                    updateMapper.markAsProcessed(transaction.getPayTransactionId());
////                                    updateSession.commit();
////                                    logger.debug("Marked transaction as processed: PayTransactionId={}", transaction.getPayTransactionId());
//                                }
//                            } catch (Exception e) {
//                                logger.error("Send failed for orderNo={}: {}", orderNo, e.getMessage(), e);
//                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error querying V_BE_Transaction: {}", e.getMessage(), e);
                }
            }

            // Wait for the next polling interval
            try {
                logger.debug("Sleeping for {}ms before next poll", appConfig.getSendInterval());
                Thread.sleep(appConfig.getSendInterval());
            } catch (InterruptedException e) {
                logger.error("Sleep interrupted: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static OrderModels.Order BuildModel() {
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
        String regionCode = "002";
        String corpCode = "601";
        String cashierNo = "7777"; //posNo 7778
        String cardNo = "12345678"; //octopus no
        String devid = "586ADF"; //octopus devid
        String orgNo = "19266"; //octopus spid
        String authcode = "888";
        String storecode = "210";
        String itemcode = "090000035"; //itemcode left pad 0s, 9 digits
        String skuname = "TVHC塑膠圓形水樽500毫升灰色(20/80)";
        String barcode = storecode + itemcode; // 210 + 090000035 = 210090000035
        int checkDigit = calculateEAN13CheckDigit(barcode);
        barcode = barcode + checkDigit; // 2100900000358

        order.setHkSaleType("1");
        order.setSaleType(1);
        order.setOrderNo(orderNo);
        order.setRegionCode(regionCode);
        order.setCorporationCode(corpCode);
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
        payment.setOrgNo(orgNo);
        payment.setAuthCode(authcode);
        payment.setGivePoint(0);
        payment.setPayType(0);
        paymentList.add(payment);
        order.setPaymentList(paymentList);

        // Item list
        List<OrderModels.Item> itemList = new ArrayList<>();
        OrderModels.Item item = new OrderModels.Item();
        item.setOrderItemNo(1);
        item.setSkuName(skuname);
        item.setItemCode(itemcode); //9 digits
        item.setBarcode(barcode);
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
        return order;
    }


    static class TransactionListenerImpl implements TransactionListener {
        private static final Logger logger = LoggerFactory.getLogger(TransactionListenerImpl.class);

        @Override
        public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
            logger.info("Executing local transaction for msg: {}", new String(msg.getBody()));
            try {
                // Simulate business logic
                return LocalTransactionState.COMMIT_MESSAGE;
            } catch (Exception e) {
                logger.error("Transaction error: {}", e.getMessage(), e);
                return LocalTransactionState.UNKNOW;
            }
        }

        @Override
        public LocalTransactionState checkLocalTransaction(MessageExt msg) {
            logger.info("Checking transaction for msg: {}", msg.getMsgId());
            return LocalTransactionState.COMMIT_MESSAGE;
        }
    }
}