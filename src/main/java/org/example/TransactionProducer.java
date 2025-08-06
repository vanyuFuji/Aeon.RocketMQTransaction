package org.example;

import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

public class TransactionProducer {
    public static void main(String[] args) throws Exception {
        // Configure producer
        TransactionMQProducer producer = new TransactionMQProducer("cloudpos_order_salesorder_group");
        producer.setNamesrvAddr("10.10.232.138:9876;10.10.232.139:9876");
        producer.setRetryTimesWhenSendFailed(5); // Increase retries
        producer.setSendMsgTimeout(15000); // Set timeout to 15s
        producer.setVipChannelEnabled(false); // Disable VIP channel for 4.x compatibility

        // Optional: If ACL is enabled (uncomment if provider confirms)
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

        // Prepare transaction payload
        String transactionPayload = "{\"hkSaleType\":\"1\",\"saleType\":1,\"orderNo\":\"00103071750916428885\",\"regionCode\":\"002\",\"corporationCode\":\"601\",\"storeCode\":\"010\",\"memberCard\":\"\",\"totalProductPrice\":15.9,\"totalPayAmount\":11.9,\"totalInvoiceAmount\":0,\"bonusPointAmount\":0,\"realPayAmount\":0,\"totalGivePoint\":0,\"givePointFlag\":0,\"returnPointFlag\":0,\"returnCouponFlag\":0,\"orderTime\":\"2025062613:42:41\",\"accountDate\":\"20250626\",\"cashierNo\":\"6210181\",\"posNo\":\"307\",\"receiptNo\":\"2506263075635\",\"originalOrderNo\":\"\",\"originalPosNo\":\"\",\"originalReceiptNo\":\"\",\"reasonCode\":\"\",\"staffNo\":\"\",\"itemList\":[{\"orderItemNo\":1,\"originalOrderItemNo\":0,\"skuName\":\"TVHC塑膠圓形水樽500毫升灰色(20/80)\",\"skuProperty\":\"\",\"itemCode\":\"030271787\",\"scanCode\":\"4549741938327\",\"barcode\":\"4549741938327\",\"saleQty\":1.0,\"salePrice\":15.9,\"payAmount\":11.9,\"bonusPointAmount\":0,\"promoDiscount\":4,\"couponDiscount\":0,\"couponAmount\":0,\"pointAmount\":0,\"givePoint\":0,\"saleUnit\":\"\",\"sellerNo\":\"\",\"itemType\":0,\"saleType\":0,\"erpCategory\":\"\",\"promoList\":null,\"couponList\":null,\"pointList\":null}],\"paymentList\":[{\"payCode\":\"CSH\",\"payName\":\"CASH\",\"payAmount\":11.9,\"originalValue\":0,\"payNo\":null,\"serialNo\":\"0\",\"cardNo\":\"\",\"deviceCode\":\"\",\"orgNo\":\"\",\"authCode\":\"\",\"templateId\":0,\"invoiceFlag\":0,\"givePoint\":0,\"payType\":0}],\"deliveryInfo\":null,\"quicklyFlag\":0,\"hkSourceId\":\"90\",\"originalHkSaleType\":\"\",\"historicalOrders\":0,\"refundMemoStatus\":null,\"depositPenalty\":\"\",\"memberFeesFlag\":0,\"balancePaymentStatus\":0,\"changeAmt\":0.0,\"originalHkSourceId\":\"\",\"extensionData\":\"\"}";
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

        // Keep running for async checks (adjust for production)
        Thread.sleep(60000);

        // Shutdown
        producer.shutdown();
    }
}

class TransactionListenerImpl implements TransactionListener {
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        System.out.println("Executing local transaction for msg: " + new String(msg.getBody()));
        try {
            // Simulate success (replace with actual DB logic)
            boolean success = true; // E.g., insert order into database
            return success ? LocalTransactionState.COMMIT_MESSAGE : LocalTransactionState.ROLLBACK_MESSAGE;
        } catch (Exception e) {
            System.err.println("Transaction error: " + e.getMessage());
            e.printStackTrace();
            return LocalTransactionState.UNKNOW;
        }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        System.out.println("Checking transaction for msg: " + msg.getMsgId());
        return LocalTransactionState.COMMIT_MESSAGE; // Adjust based on DB check
    }
}