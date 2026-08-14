package com.sms.razorpay;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.sms.model.StudentFee;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RazorpayPaymentService {

//    private final RazorpayClient razorpayClient;
//    private final PaymentRepository paymentRepository;
//    private final String keyId;

//	@Value("${razorpay.key.secret}")
//    private String razorpayKeySecret;

//    private final long fixedAmountPaise;
//
//    public RazorpayPaymentService(
//            RazorpayClient razorpayClient,
//            PaymentRepository paymentRepository,
//            @Value("${razorpay.key-id}") String keyId,
//            @Value("${razorpay.key-secret}") String keySecret,
//            @Value("${razorpay.fixed-amount-rupees}") long fixedAmountRupees) {
//
//        this.razorpayClient = razorpayClient;
//        this.paymentRepository = paymentRepository;
//        this.keyId = keyId;
//        this.keySecret = keySecret;
//        this.fixedAmountPaise = fixedAmountRupees * 100;
//    }
//
//    public String getKeyId() {
//        return keyId;
//    }
//
//    public long getFixedAmountRupees() {
//        return fixedAmountPaise / 100;
//    }
//
//    @Transactional
//    public Map<String, Object> createOrder() throws Exception {
//
//        String internalOrderId = "SMS-" +
//                UUID.randomUUID().toString().replace("-", "").substring(0, 20);
//
//        JSONObject request = new JSONObject();
//        request.put("amount", fixedAmountPaise);
//        request.put("currency", "INR");
//        request.put("receipt", internalOrderId);
//
//        Order razorpayOrder = razorpayClient.orders.create(request);
//
//        Payment payment = new Payment();
//        payment.setInternalOrderId(internalOrderId);
//        payment.setRazorpayOrderId(razorpayOrder.get("id"));
//        payment.setAmountPaise(fixedAmountPaise);
//        payment.setCurrency("INR");
//        payment.setStatus(PaymentStatus.PENDING);
//
//        paymentRepository.save(payment);
//
//        Map<String, Object> response = new LinkedHashMap<>();
//        response.put("success", true);
//        response.put("internalOrderId", internalOrderId);
//        response.put("orderId", razorpayOrder.get("id"));
//        response.put("amount", razorpayOrder.get("amount"));
//        response.put("currency", razorpayOrder.get("currency"));
//
//        return response;
//    }
//
    @Transactional
    public boolean verifyPayment(VerifyPaymentRequest request) throws Exception {

    	String payload =
                request.getRazorpayOrderId()
                + "|"
                + request.getRazorpayPaymentId();

        boolean verified = Utils.verifySignature(
                payload,
                request.getRazorpaySignature(),
                "hwhVzaph532E9cy2hZABWqKR"
        );

        if (!verified) {
            return false;
        }

        // Yahan database update hoga
        
        StudentFee sFee = new StudentFee();
        
        sFee.setAmount(null);
        sFee.setMonth(null);
        sFee.setPaymentDate(LocalDateTime.now());
        sFee.setSchool(null);
        sFee.setSession(null);
        sFee.setStatus(null);
        sFee.setStudent(null);
        
        sFee.setCurrency(null);
        sFee.setRazorpayPaymentId(null);
        sFee.setRazorpayOrderId(null);
        sFee.setInternalOrderId(null);

        return true;
    }
//
//    @Transactional
//    public void handleWebhook(String payload, String signature) throws Exception {
//
//        // Razorpay signs the raw webhook body. Verify BEFORE parsing/trusting it.
//        Utils.verifyWebhookSignature(payload, signature, keySecret);
//
//        JSONObject event = new JSONObject(payload);
//        String eventName = event.optString("event");
//
//        JSONObject payloadObject = event.optJSONObject("payload");
//        if (payloadObject == null) {
//            return;
//        }
//
//        JSONObject paymentEntity = null;
//        JSONObject orderEntity = null;
//
//        JSONObject paymentWrapper = payloadObject.optJSONObject("payment");
//        if (paymentWrapper != null) {
//            paymentEntity = paymentWrapper.optJSONObject("entity");
//        }
//
//        JSONObject orderWrapper = payloadObject.optJSONObject("order");
//        if (orderWrapper != null) {
//            orderEntity = orderWrapper.optJSONObject("entity");
//        }
//
//        String razorpayOrderId = paymentEntity != null
//                ? paymentEntity.optString("order_id", null)
//                : orderEntity != null
//                ? orderEntity.optString("id", null)
//                : null;
//
//        if (razorpayOrderId == null) {
//            return;
//        }
//
//        Payment payment = paymentRepository
//                .findByRazorpayOrderId(razorpayOrderId)
//                .orElse(null);
//
//        if (payment == null) {
//            return;
//        }
//
//        switch (eventName) {
//            case "payment.captured", "order.paid" -> {
//                if (paymentEntity != null) {
//                    payment.setRazorpayPaymentId(
//                            paymentEntity.optString("id", payment.getRazorpayPaymentId())
//                    );
//                }
//                payment.setStatus(PaymentStatus.PAID);
//                paymentRepository.save(payment);
//            }
//
//            case "payment.failed" -> {
//                payment.setStatus(PaymentStatus.FAILED);
//                paymentRepository.save(payment);
//            }
//
//            default -> {
//                // Ignore events that are not needed by this application.
//            }
//        }
//    }
}
