package com.sms.razorpay;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class RazorpayController {

    private final RazorpayPaymentService paymentService;

    public RazorpayController(RazorpayPaymentService paymentService) {
        this.paymentService = paymentService;
    }
//
//    @GetMapping("/payment")
//    public String payment(Model model) {
//        model.addAttribute("razorpayKeyId", paymentService.getKeyId());
//        model.addAttribute("amountRupees", paymentService.getFixedAmountRupees());
//        return "payment";
//    }
//
//    @PostMapping("/api/razorpay/create-order")
//    @ResponseBody
//    public ResponseEntity<?> createOrder() {
//        try {
//            return ResponseEntity.ok(paymentService.createOrder());
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(
//                    Map.of(
//                            "success", false,
//                            "message", "Unable to create payment order."
//                    )
//            );
//        }
//    }
//
    @PostMapping("/student/verify-payment")
    @ResponseBody
    public Map<String, Object> verify(@RequestBody VerifyPaymentRequest request) {
    	Map<String, Object> response = new HashMap<>();

        try {

            boolean verified = paymentService.verifyPayment(request);

            if (verified) {
                response.put("success", true);
                response.put("message", "Payment verified successfully");
                System.out.println("payment successful...");
            } else {
                response.put("success", false);
                response.put("message", "Payment verification failed");
            }

        } catch (Exception e) {

            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }
//
//    /**
//     * Configure this URL in Razorpay Dashboard:
//     * POST /api/razorpay/webhook
//     *
//     * IMPORTANT: expose it through HTTPS in production.
//     */
//    @PostMapping("/api/razorpay/webhook")
//    @ResponseBody
//    public ResponseEntity<String> webhook(
//            @RequestBody String payload,
//            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
//
//        try {
//            if (signature == null || signature.isBlank()) {
//                return ResponseEntity.badRequest().body("Missing signature");
//            }
//
//            paymentService.handleWebhook(payload, signature);
//            return ResponseEntity.ok("OK");
//
//        } catch (Exception e) {
//            // A non-2xx response tells Razorpay the webhook was not processed.
//            return ResponseEntity.badRequest().body("Invalid webhook");
//        }
//    }
}
