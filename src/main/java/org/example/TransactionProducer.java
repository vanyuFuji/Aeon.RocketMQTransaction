package org.example;

import com.google.gson.reflect.TypeToken;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TransactionProducer {
    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(TransactionProducer.class);
    private static TransactionMQProducer producer = null; // Track producer state
    private static SqlSessionFactory sqlSessionFactory;

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

    public static class TestProductMapping {
        private String productName;
        private String itemCode;

        public String getProductName() {
            return productName;
        }

        public String getItemCode() {
            return itemCode;
        }
    }

    public static class AppConfig {
        private long sendInterval;
        private List<StoreCodeMapping> storeCode;
        private boolean testProductmode;

        public boolean getTestProudctMode() {
            return testProductmode;
        }

        public long getSendInterval() {
            return sendInterval;
        }

        public List<StoreCodeMapping> getStorecode() {
            return storeCode;
        }
    }

    public static class StoreCodeMapping {
        private String code;
        private String machine;
        private String VYdevId;
        private String posNo;


        public String getCode() {
            return code;
        }

        public String getMachine() {
            return machine;
        }

        public String getVYdevId() {
            return VYdevId;
        }

        public String getPosNo() {
            return posNo;
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

        Map<String, StoreCodeMapping> storeCodeMap = new HashMap<>();
        List<TestProductMapping> productList = new ArrayList<>();

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
            logger.info("Test Product Mode: {}", appConfig.getTestProudctMode());
            // Build storecode mapping
            if (appConfig.getStorecode() != null) {
                for (StoreCodeMapping mapping : appConfig.getStorecode()) {
                    storeCodeMap.put(mapping.getMachine(), mapping);
                    logger.debug("Storecode mapping: machine={} -> code={}, getVYdevId={}, posNo={}",
                            mapping.getMachine(), mapping.getCode(), mapping.getVYdevId(), mapping.getPosNo());
                }
            } else {
                logger.warn("No storecode mappings found in config.json");
            }
        } catch (Exception e) {
            logger.error("Failed to load config.json: {}", e.getMessage(), e);
            throw e;
        }

        // Load testproduct.json for product list
        if (appConfig.getTestProudctMode()) {
            java.nio.file.Path productPath = Paths.get("testproduct.json");
            if (!Files.exists(productPath)) {
                logger.error("testproduct.json not found in the same directory: {}", productPath.toAbsolutePath());
                throw new java.io.FileNotFoundException("Error: testproduct.json not found");
            }
            try (InputStream inputStream = Files.newInputStream(productPath);
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                productList = gson.fromJson(jsonObject.getAsJsonArray("products"),
                        new TypeToken<List<TestProductMapping>>() {
                        }.getType());
                logger.info("Loaded {} product mappings from testproduct.json", productList.size());
                for (TestProductMapping product : productList) {
                    logger.debug("Product mapping: itemCode={} -> productName={}", product.getItemCode(), product.getProductName());
                }
            } catch (Exception e) {
                logger.error("Failed to load testproduct.json: {}", e.getMessage(), e);
                throw e;
            }
        }

        // Initialize MyBatis
        try (InputStream inputStream = new FileInputStream("mybatis-config.xml")) {
            logger.debug("Loading mybatis-config.xml from: {}", new File("mybatis-config.xml").getAbsolutePath());
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            logger.info("Initialized MyBatis SqlSessionFactory");
        } catch (Exception e) {
            logger.error("Failed to initialize MyBatis SqlSessionFactory: {}", e.getMessage(), e);
            throw new RuntimeException("MyBatis initialization failed", e);
        }
        if (sqlSessionFactory == null) {
            throw new RuntimeException("SqlSessionFactory is null after initialization attempt");
        }


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
                        // Initialize producer only when transactions are found
                        if (producer == null) {
                            logger.info("Found {} unprocessed transactions. Initializing producer.", transactions.size());
                            producer = new TransactionMQProducer(rocketMQConfig.getProducerGroup());
                            producer.setNamesrvAddr(rocketMQConfig.getNamesrvAddr());
                            producer.setRetryTimesWhenSendFailed(rocketMQConfig.getRetryTimes());
                            producer.setSendMsgTimeout(rocketMQConfig.getSendTimeout());
                            producer.setVipChannelEnabled(false);
                            producer.setTransactionListener(new TransactionListenerImpl());
                            try {
                                producer.start();
                                logger.info("Producer started successfully");
                                // Add shutdown hook
                                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                    if (producer != null) {
                                        producer.shutdown();
                                        logger.info("Producer shut down via shutdown hook");
                                    }
                                }));
                            } catch (Exception e) {
                                logger.error("Failed to start producer: {}", e.getMessage(), e);
                                throw e;
                            }
                        }
                        for (VBeTransaction transaction : transactions) {
                            // Log raw row data
//                            logger.info("Raw row data: Time={}, region={}, locationName={}, machineName={}, machineNumber={}, " +
//                                            "Devid={}, slot={}, productName={}, barCode={}, ProductTypeName={}, Amount={}, PayType={}, " +
//                                            "PayTransactionId={}, PayUserID={}, PayOutTradeNo={}",
//                                    transaction.getTime(), transaction.getRegion(), transaction.getLocationName(),
//                                    transaction.getMachineName(), transaction.getMachineNumber(), transaction.getDevid(),
//                                    transaction.getSlot(), transaction.getProductName(), transaction.getBarCode(),
//                                    transaction.getProductTypeName(), transaction.getAmount(), transaction.getPayType(),
//                                    transaction.getPayTransactionId(), transaction.getPayUserId(), transaction.getPayOutTradeNo());

                            // Process transaction using BuildModel
                            OrderModels.Order order = BuildModel(transaction, gson, storeCodeMap, productList, appConfig.getTestProudctMode());

                            // Serialize to JSON
                            String transactionPayload = gson.toJson(order);
                            //logger.info("Serialized order payload: {}", transactionPayload);

                            // Send message
                            Message msg = new Message(
                                    rocketMQConfig.getTopic(),
                                    rocketMQConfig.getTag(),
                                    order.getOrderNo(),
                                    transactionPayload.getBytes("UTF-8")
                            );
                            msg.putUserProperty("OrderSource", "POS");

                            try {
                                TransactionSendResult sendResult = producer.sendMessageInTransaction(msg, null);
                                logger.info("Message sent: ID={}, Status={}", sendResult.getMsgId(), sendResult.getLocalTransactionState());
                                // Mark as processed
                                try (SqlSession updateSession = sqlSessionFactory.openSession()) {
                                    VBeTransactionMapper updateMapper = updateSession.getMapper(VBeTransactionMapper.class);
                                    updateMapper.markAsProcessed(transaction.getId(), sendResult.getMsgId());
                                    updateSession.commit();
                                    logger.debug("Marked transaction as processed: id={}, ProcessedId={}", transaction.getId(), sendResult.getMsgId());
                                }
                            } catch (Exception e) {
                                logger.error("Send failed for transaction={}: {}", transaction.getId(), e.getMessage(), e);
                            }

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

    private static OrderModels.Order BuildModel(VBeTransaction transaction, Gson gson, Map<String, StoreCodeMapping> storeCodeMap
            , List<TestProductMapping> productList, boolean testProductMode) {
        OrderModels.Order order = new OrderModels.Order();
        String orderNo;
        StoreCodeMapping defaultMapping = new StoreCodeMapping();
        defaultMapping.posNo = "777";
        defaultMapping.code = "071";
        defaultMapping.VYdevId = "DEV000";
        StoreCodeMapping mapping = storeCodeMap.getOrDefault(transaction.getMachineName(), defaultMapping);
        double price = transaction.getAmount() != null ? transaction.getAmount() : 0.1;
        String cashierNo = "0000000";
        String posNo = storeCodeMap.getOrDefault(transaction.getMachineName(), defaultMapping).getPosNo();//first 777, second 778 same shop
        String cardNo = transaction.getPayUserId();
        String devid = transaction.getDevid();
        String orgNo = "19266";
        String authcode = "888";
        String storecode = storeCodeMap.getOrDefault(transaction.getMachineName(), defaultMapping).getCode() + "";
        String transactionDevId = transaction.getDevid() != null ? transaction.getDevid() : "";
        String transactionMachineNumber = transaction.getMachineNumber() != null ? transaction.getMachineNumber() : "";
        boolean useCashSales = transactionMachineNumber.equals(mapping.getMachine()) && transactionDevId.equals(mapping.getVYdevId());
        String regioncode = "002";
        String corpcode = "601";
        String barcodeprefix = "210";
        String itemcode = transaction.getBarCode();
        String skuname;

        if (transaction.getId() != null && transaction.getMachineName() != null) {
            String machineDigits = transaction.getMachineName().replaceAll("[^0-9]", "");
            if (posNo.length() != 3) {
                logger.warn("posNo must be 3 digits, using default '777' for machine={}", posNo);
                posNo="777";
            }
            int idPaddingWidth = 32 - machineDigits.length();
            String paddedId = String.format("%0" + idPaddingWidth + "d", transaction.getId());
            String concatString = machineDigits + paddedId;
            // Replace positions 5-7 (characters 4-6, 0-based) with posno
            StringBuilder orderNoBuilder = new StringBuilder(concatString);
            if (concatString.length() >= 8) {
                orderNoBuilder.replace(5, 8, posNo);
            } else {
                logger.warn("concatString too short ({}) to replace positions 5-7, using original", concatString);
            }
            orderNo = orderNoBuilder.toString();
            if (orderNo.length() > 32) {
                orderNo = orderNo.substring(0, 32);

                logger.debug("Generated orderNo: {} (machineDigits: {})", orderNo, machineDigits);
            }

        } else {
            orderNo = String.format("%032d", System.currentTimeMillis());
            logger.debug("Generated orderNo (fallback): {}", orderNo);
        }


        if (testProductMode && !productList.isEmpty()) {
            TestProductMapping product = productList.get(random.nextInt(productList.size()));
            itemcode = product.getItemCode();
            skuname = product.getProductName();
            logger.debug("Test mode: Selected random product - itemCode={}, productName={}", itemcode, skuname);
        } else {
            itemcode = transaction.getBarCode();
            skuname = transaction.getProductName() != null ? transaction.getProductName() : "Unknown Product";
        }

        if (itemcode == null || itemcode.trim().isEmpty()) {
            itemcode = "000000000"; // Default for null/empty
        } else {
            // Keep only digits
            itemcode = itemcode.replaceAll("[^0-9]", "");
            if (itemcode.length() == 0) {
                itemcode = "000000000"; // Default for non-numeric
            } else if (itemcode.length() > 9) {
                itemcode = itemcode.substring(0, 9); // Truncate to 9 digits
            } else {
                itemcode = String.format("%09d", Long.parseLong(itemcode)); // Pad to 9 digits
            }
        }
        String orderTime = transaction.getTime() != null ?
                new SimpleDateFormat("yyyyMMddHH:mm:ss").format(transaction.getTime()) : "";
        String accountDate = transaction.getTime() != null ?
                new SimpleDateFormat("yyyyMMdd").format(transaction.getTime()) : "";

        String barcode = barcodeprefix + itemcode;

        int checkDigit = calculateEAN13CheckDigit(barcode);
        barcode = barcode + checkDigit;

        // Set ignoreAccountDate based on transaction time
        boolean ignoreAccountDate = true;
        if (transaction.getTime() != null) {
            LocalDate transactionDate = transaction.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate today = LocalDate.now(ZoneId.systemDefault()); // Today is 2025-08-13
            ignoreAccountDate = !transactionDate.isEqual(today);
            logger.debug("Transaction date: {}, Today: {}, ignoreAccountDate: {}", transactionDate, today, ignoreAccountDate);
        }
        order.setHkSaleType("1");
        order.setSaleType(1);
        order.setRegionCode(regioncode);
        order.setOrderNo(orderNo);
        order.setCorporationCode(corpcode);
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
        order.setPosNo(posNo);
        order.setReceiptNo(transaction.getId().toString());
        order.setQuicklyFlag(0);
        order.setHkSourceId("90");
        order.setHistoricalOrders(0);
        order.setDeliveryInfo(null);
        order.setIgnoreAccountDate(ignoreAccountDate);

        // Payment list
        List<OrderModels.Payment> paymentList = new ArrayList<>();
        OrderModels.Payment payment = new OrderModels.Payment();
        String payType = transaction.getPayType();
        if (useCashSales) {
            payment.setPayCode("CSH");
            payment.setPayName("CASH SALES");
            logger.debug("machineNumber={} and VYdevId={} match config: Set payCode=CSH, payName=CASH SALES",
                    transaction.getMachineNumber(), transaction.getDevid());
        } else {
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
            logger.debug("machineNumber={} or VYdevId={} do not match config: Set payCode={}, payName={}",
                    transaction.getMachineNumber(), transaction.getDevid(), payment.getPayCode(), payment.getPayName());
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




