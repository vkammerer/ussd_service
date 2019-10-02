import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:ussd_service/ussd_service.dart';

import 'package:permission_handler/permission_handler.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

enum RequestState {
  Undefined,
  Ongoing,
  Success,
  Error,
}

class _MyAppState extends State<MyApp> {
  RequestState _requestState = RequestState.Undefined;
  String _ussdResponseCode = '';
  String _ussdResponseMessage = '';
  String _ussdRequestCode = "";

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> sendUssdRequest() async {
    if (_requestState == RequestState.Ongoing) return;
    setState(() {
      _requestState = RequestState.Ongoing;
    });
    String ussdResponse;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      await PermissionHandler().requestPermissions([PermissionGroup.phone]);
      ussdResponse = await UssdService.makeRequest(1, _ussdRequestCode);
      if (!mounted) return;
      setState(() {
        _requestState = RequestState.Success;
        _ussdResponseMessage = ussdResponse;
      });
    } on PlatformException catch (e) {
      if (!mounted) return;
      setState(() {
        _requestState = RequestState.Error;
        _ussdResponseCode = e.code;
        _ussdResponseMessage = e.message;
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
                      _ussdRequestCode = newValue;
                    });
                  },
                ),
                SizedBox(
                  height: 20,
                ),
                MaterialButton(
                  color: Colors.blue,
                  textColor: Colors.white,
                  onPressed: () {
                    sendUssdRequest();
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
                    _ussdResponseMessage,
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ],
                if (_requestState == RequestState.Error) ...[
                  Text('Last request was not successful'),
                  SizedBox(height: 10),
                  Text('Error code was:'),
                  Text(
                    _ussdResponseCode,
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 10),
                  Text('Error message was:'),
                  Text(
                    _ussdResponseMessage,
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ]
              ]),
        ),
      ),
    );
  }
}
