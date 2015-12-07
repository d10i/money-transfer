package com.moneytransfer.utils

import spray.util.LoggingContext

trait LoggingComponent {

  implicit def log: LoggingContext
}
