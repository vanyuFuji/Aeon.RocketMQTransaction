package org.example;

import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class TransactionProducer {
    public static void main(String[] args) throws Exception {
        // Configure producer
        TransactionMQProducer producer = new TransactionMQProducer("cloudpos_order_salesorder_group");
        producer.setNamesrvAddr("10.10.232.138:9876;10.10.232.139:9876");
        producer.setRetryTimesWhenSendFailed(5);
        producer.setSendMsgTimeout(15000);
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
        order.setHkSaleType("1");
        order.setSaleType(1);
        order.setOrderNo("00103071750916428885");
        order.setRegionCode("002");
        order.setCorporationCode("601");
        order.setStoreCode("010");
        order.setMemberCard("");
        order.setTotalProductPrice(15.9);
        order.setTotalPayAmount(11.9);
        order.setTotalInvoiceAmount(0);
        order.setBonusPointAmount(0);
        order.setRealPayAmount(0);
        order.setTotalGivePoint(0);
        order.setGivePointFlag(0);
        order.setReturnPointFlag(0);
        order.setReturnCouponFlag(0);
        order.setOrderTime("2025062613:42:41");
        order.setAccountDate("20250626");
        order.setCashierNo("6210181");
        order.setPosNo("307");
        order.setReceiptNo("2506263075635");
        order.setOriginalOrderNo("");
        order.setOriginalPosNo("");
        order.setOriginalReceiptNo("");
        order.setReasonCode("");
        order.setStaffNo("");
        order.setQuicklyFlag(0);
        order.setHkSourceId("90");
        order.setOriginalHkSaleType("");
        order.setHistoricalOrders(0);
        order.setRefundMemoStatus(null);
        order.setDepositPenalty("");
        order.setMemberFeesFlag(0);
        order.setBalancePaymentStatus(0);
        order.setChangeAmt(0.0);
        order.setOriginalHkSourceId("");
        order.setExtensionData("");
        order.setDeliveryInfo(null);

        // Payment list
        List<OrderModels.Payment> paymentList = new ArrayList<>();
        OrderModels.Payment payment = new OrderModels.Payment();
        payment.setPayCode("CSH");
        payment.setPayName("CASH");
        payment.setPayAmount(11.9);
        payment.setOriginalValue(0);
        payment.setPayNo(null);
        payment.setSerialNo("0");
        payment.setCardNo("");
        payment.setDeviceCode("");
        payment.setOrgNo("");
        payment.setAuthCode("");
        payment.setTemplateId(0);
        payment.setInvoiceFlag(0);
        payment.setGivePoint(0);
        payment.setPayType(0);
        paymentList.add(payment);
        order.setPaymentList(paymentList);

        // Item list
        List<OrderModels.Item> itemList = new ArrayList<>();
        OrderModels.Item item = new OrderModels.Item();
        item.setOrderItemNo(1);
        item.setOriginalOrderItemNo(0);
        item.setSkuName("TVHC塑膠圓形水樽500毫升灰色(20/80)");
        item.setSkuProperty("");
        item.setItemCode("030271787");
        item.setScanCode("4549741938327");
        item.setBarcode("4549741938327");
        item.setSaleQty(1.0);
        item.setSalePrice(15.9);
        item.setPayAmount(11.9);
        item.setBonusPointAmount(0);
        item.setPromoDiscount(4);
        item.setCouponDiscount(0);
        item.setCouponAmount(0);
        item.setPointAmount(0);
        item.setGivePoint(0);
        item.setSaleUnit("");
        item.setSellerNo("");
        item.setItemType(0);
        item.setSaleType(0);
        item.setErpCategory("");
        item.setPromoList(null);
        item.setCouponList(null);
        item.setPointList(null);
        itemList.add(item);
        order.setItemList(itemList);

        // Serialize to JSON
        Gson gson = new Gson();
        String transactionPayload = gson.toJson(order);

        Message msg = new Message(
                "cloudpos_order_salesorder_topic",
                "salesOrderTag",
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

        Thread.sleep(60000);
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