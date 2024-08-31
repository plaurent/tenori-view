# tenori-view

This is a cross-platform viewer which reproduces the Tenori-On LCD screen on your computer.

It works by monitoring specially-crafted SysEx messages emitted from Tenori-On's running pika.blue's custom A038 firmware when a particular setting is enabled.

Future versions of this code will provide an option for sending the data to external devices over TCP/IP.

## Usage

To enable these SysEx messages with the Tenori-On LCD content, go to `Main Menu > 10 MIDI menu > Out Screen->Sysex` and turn Screen via Sysex to ON.

You can then connect your Tenori-On to the computer and double-click `tenori-view.jar` to view the LCD screen.

## Demo Video

https://vimeo.com/pakl/tenori-view-demo01
[![Demo Video](https://pakl.net/serve/tenori-view-demo-video-icon.png)](https://vimeo.com/pakl/tenori-view-demo01  "Demo video - Click to Watch!")

