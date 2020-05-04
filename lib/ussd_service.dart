import 'dart:async';

import 'package:flutter/services.dart';

/// Flutter plugin to access to access Android's sendUssdRequest method from a Flutter application.
/// See the section for sendUssdRequest in https://developer.android.com/reference/android/telephony/TelephonyManager.html

class UssdService {
  static const MethodChannel _channel =
      MethodChannel("com.vincentkammerer.ussd_service/plugin_channel");

  /// Performs the USSD request and returns the response
  static Future<String> makeRequest(
    int subscriptionId,
    String code, [
    Duration timeout = const Duration(seconds: 10),
  ]) async {
    final String response = await _channel
        .invokeMethod(
          "makeRequest",
          {"subscriptionId": subscriptionId, "code": code},
        )
        .timeout(timeout)
        .catchError((e) {
          // TODO: Move timeout handling to Android side once Java 9 is used
          // and `CompletableFuture.timeout` is available
          if (e is TimeoutException) {
            throw PlatformException(
                code: "ussd_plugin_ussd_execution_timeout", message: e.message);
          }
          throw e;
        });
    return response;
  }
}
