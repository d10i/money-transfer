package com.moneytransfer.utils

import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport

trait JsonSupport extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats.lossless
}