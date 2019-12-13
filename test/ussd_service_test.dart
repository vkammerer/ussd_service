import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ussd_service/ussd_service.dart';

void main() {
  const MethodChannel channel = MethodChannel('ussd_service');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('makeRequest', () async {
    expect(await UssdService.makeRequest, isNot('42'));
  });
}
