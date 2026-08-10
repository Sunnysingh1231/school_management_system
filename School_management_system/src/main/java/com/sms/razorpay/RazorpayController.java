package com.sms.razorpay;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class RazorpayController {

//    private final RazorpayPaymentService paymentService;
//
//    public RazorpayController(RazorpayPaymentService paymentService) {
//        this.paymentService = paymentService;
//    }
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
//    @PostMapping("/api/razorpay/verify")
//    @ResponseBody
//    public ResponseEntity<?> verify(@RequestBody VerifyPaymentRequest request) {
//        try {
//            boolean verified = paymentService.verify(request);
//
//            if (!verified) {
//                return ResponseEntity.badRequest().body(
//                        Map.of(
//                                "success", false,
//                                "message", "Payment verification failed."
//                        )
//                );
//            }
//
//            return ResponseEntity.ok(
//                    Map.of(
//                            "success", true,
//                            "message", "Payment verified successfully.",
//                            "paymentId", request.getRazorpayPaymentId()
//                    )
//            );
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(
//                    Map.of(
//                            "success", false,
//                            "message", "Payment verification failed."
//                    )
//            );
//        }
//    }
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
