package com.moneytransfer.exceptions

class BalanceTooLowException(message: String, code: Option[String]) extends ServiceException(message, code) {

  def this(accountId: String, amount: Float) = {
    this(s"Account $accountId can't afford to transfer $amount", Some("BalanceTooLow"))
  }
}
