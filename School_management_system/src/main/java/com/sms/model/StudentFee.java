package com.sms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_fees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @Column(nullable = false)
    private String month;

    @Column(nullable = false)
    private String session;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    private LocalDateTime paymentDate;

    @Column(length = 100)
    private String transactionId;
    
    @Column(name = "internal_order_id", nullable = false, unique = true, length = 80)
	private String internalOrderId;

	@Column(name = "razorpay_order_id", unique = true, length = 100)
	private String razorpayOrderId;

	@Column(name = "razorpay_payment_id", unique = true, length = 100)
	private String razorpayPaymentId;

	@Column(nullable = false, length = 10)
	private String currency = "INR";
    
 
}