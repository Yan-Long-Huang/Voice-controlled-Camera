import 'package:flutter/material.dart';

void main() {
  runApp(MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'Voice Camera',
    home: HomeScreen(),
  ));
}

class HomeScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Voice Camera"),
      ),
      drawer: Drawer(
        child: ListView(
          children: <Widget>[
            DrawerHeader(
                decoration: BoxDecoration(
                    gradient: LinearGradient(
                        colors: <Color>[Colors.pinkAccent, Colors.yellow])),
                child: Container(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: <Widget>[
                      Material(
                        borderRadius: BorderRadius.all(Radius.circular(26)),
                        elevation: 10,
                        child: Image.asset(
                          'images/camera.png',
                          width: 100,
                          height: 100,
                        ),
                      ),
                      Text(
                        'Voice Camera',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 26,
                          fontWeight:
                              FontWeight.bold, // fontStyle: FontStyle.italic
                        ),
                      ),
                    ],
                  ),
                )),
            CustomListTitle(Icons.camera, "Camera", () => {}),
            CustomListTitle(Icons.photo, "Gallery", () => {}),
            CustomListTitle(Icons.info, "About", () => {}),
          ],
        ),
      ),
    );
  }
}

class CustomListTitle extends StatelessWidget {
  IconData icon;
  String text;
  Function onTap;

  CustomListTitle(this.icon, this.text, this.onTap);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8.0, 0, 8.0, 0),
      child: Container(
        decoration: BoxDecoration(
            border:
                Border(bottom: BorderSide(color: Colors.blueGrey.shade100))),
        child: InkWell(
          splashColor: Colors.cyanAccent[100],
          onTap: () => {},
          child: Container(
            height: 40,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                Row(
                  children: <Widget>[
                    Icon(icon),
                    Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Text(text, style: TextStyle(fontSize: 16.0)))
                  ],
                ),
                Icon(Icons.arrow_right)
              ],
            ),
          ),
        ),
      ),
    );
  }
}
