package com.shopcore.factory;

import com.shopcore.enums.PaymentMethod;
import com.shopcore.exception.ShopCoreException;
import com.shopcore.payment.CardPayment;
import com.shopcore.payment.CashOnDeliveryPayment;
import com.shopcore.payment.NetBankingPayment;
import com.shopcore.payment.PaymentProcessor;
import com.shopcore.payment.UpiPayment;

/** Factory pattern: PaymentService asks for "a processor for UPI" without knowing the class. */
public final class PaymentFactory {
    private PaymentFactory() {}

    public static PaymentProcessor getProcessor(PaymentMethod method) {
        switch (method) {
            case UPI:         return new UpiPayment();
            case CARD:        return new CardPayment();
            case NET_BANKING: return new NetBankingPayment();
            case COD:         return new CashOnDeliveryPayment();
            default:          throw new ShopCoreException("Unsupported payment method: " + method);
        }
    }
}
