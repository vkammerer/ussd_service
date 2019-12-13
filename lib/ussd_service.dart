import 'dart:async';

import 'package:flutter/services.dart';

/// Flutter plugin to access to access Android's sendUssdRequest method from a Flutter application.
/// See the section for sendUssdRequest in https://developer.android.com/reference/android/telephony/TelephonyManager.html

class UssdService {
  static const MethodChannel _channel =
  MethodChannel('vincentkammerer.com/ussd_service');

  /// Performs the USSD request and returns the response
  static Future<String> makeRequest(int subscriptionId, String code) async {
    final String response = await _channel.invokeMethod(
      'makeRequest',
      {'subscriptionId': subscriptionId, 'code': code},
    );
    return response;
  }
}
