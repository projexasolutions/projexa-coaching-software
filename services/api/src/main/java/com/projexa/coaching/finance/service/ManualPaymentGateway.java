package com.projexa.coaching.finance.service;
import org.springframework.stereotype.Component;
@Component public class ManualPaymentGateway implements PaymentGateway { public String createPayment(String invoiceId,long amountInMinorUnits,String currency){return "MANUAL-"+invoiceId;} public boolean verifyWebhook(String payload,String signature){return true;} }