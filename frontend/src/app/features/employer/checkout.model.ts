export interface CheckoutSession {
  paymentId: string;
  transactionId: string;
  redirectUrl: string;
  amount: number;
}
