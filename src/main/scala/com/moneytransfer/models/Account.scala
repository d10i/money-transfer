package com.moneytransfer.models

case class Account(id: String, firstName: String, lastName: String, balance: Float)

object Account {
  def apply(firstName: String, lastName: String, balance: Float): Account = Account(id = java.util.UUID.randomUUID.toString,
    firstName = firstName,
    lastName = lastName,
    balance = balance)
}
