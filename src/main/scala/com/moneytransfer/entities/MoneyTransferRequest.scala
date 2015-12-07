package com.moneytransfer.entities

case class MoneyTransferRequest(fromAccountId: String, toAccountId: String, amount: Float)