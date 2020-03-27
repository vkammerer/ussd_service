# ussd_service example

Demonstrates how to use the Flutter plugin ussd_service.

### Test the example project

If you want to test the plugin in its own project:

1. Clone the plugin repository:
```
git clone https://github.com/vkammerer/ussd_service.git
```
2. Open the "example" folder with your IDE or editor.   
3. Install the project dependencies.   
4. Run the application.   

### Or test in your project

If you prefer to test it directly in your project:

1. Make sure that your `AndroidManifext.xml` file includes the following permission:
```xml
<uses-permission android:name="android.permission.CALL_PHONE" />
```
2. Add `ussd_service`, `permission_handler` and `sim_service` as a dependencies in your pubspec.yaml.   

3. Then include and adapt the following code to your project.   
```dart
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:sim_service/models/sim_data.dart';
import 'package:sim_service/sim_service.dart';
import 'package:ussd_service/ussd_service.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

enum RequestState {
  Ongoing,
  Success,
  Error,
}

class _MyAppState extends State<MyApp> {
  RequestState _requestState;
  String _requestCode = "";
  String _responseCode = "";
  String _responseMessage = "";

  Future<void> _sendUssdRequest() async {
    setState(() {
      _requestState = RequestState.Ongoing;
    });
    try {
      String responseMessage;
      await PermissionHandler().requestPermissions([PermissionGroup.phone]);
      SimData simData = await SimService.getSimData;
      if (simData == null) {
        debugPrint("simData cant be null");
        return;
      }
      responseMessage = await UssdService.makeRequest(
          simData.cards.first.subscriptionId, _requestCode);
      setState(() {
        _requestState = RequestState.Success;
        _responseMessage = responseMessage;
      });
    } catch (e) {
      setState(() {
        _requestState = RequestState.Error;
        _responseCode = e.code;
        _responseMessage = e.message;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Ussd Plugin demo'),
        ),
        body: Container(
          padding: EdgeInsets.symmetric(vertical: 10, horizontal: 20),
          child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                TextFormField(
                  decoration: InputDecoration(
                    labelText: 'Enter Code',
                  ),
                  onChanged: (newValue) {
                    setState(() {
                      _requestCode = newValue;
                    });
                  },
                ),
                SizedBox(
                  height: 20,
                ),
                MaterialButton(
                  color: Colors.blue,
                  textColor: Colors.white,
                  onPressed: _requestState == RequestState.Ongoing
                      ? null
                      : () {
                          _sendUssdRequest();
                        },
                  child: Text('Send Ussd request'),
                ),
                SizedBox(
                  height: 20,
                ),
                if (_requestState == RequestState.Ongoing)
                  Text('Ongoing request...'),
                if (_requestState == RequestState.Success) ...[
                  Text('Last request was successful.'),
                  SizedBox(height: 10),
                  Text('Response was:'),
                  Text(
                    _responseMessage,
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ],
                if (_requestState == RequestState.Error) ...[
                  Text('Last request was not successful'),
                  SizedBox(height: 10),
                  Text('Error code was:'),
                  Text(
                    _responseCode,
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 10),
                  Text('Error message was:'),
                  Text(
                    _responseMessage,
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ]
              ]),
        ),
      ),
    );
  }
}
```