import 'dart:async';

import 'package:flutter/services.dart';

class UssdService {
  static const MethodChannel _channel =
      const MethodChannel('vincentkammerer.com/ussd_service');

  static Future<String> makeRequest(subscriptionId, code) async {
    final String response = await _channel.invokeMethod(
        'makeRequest', {'subscriptionId': subscriptionId, 'code': code});
    return response;
  }
}
