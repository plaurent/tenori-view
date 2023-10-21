import java.awt.AlphaComposite
import java.awt.Graphics
import java.awt.EventQueue
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.geom._
import javax.swing._
import javax.imageio.ImageIO

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException

import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver
import javax.sound.midi.SysexMessage
import javax.sound.midi.MidiMessage

object TenoriOnLCD extends JFrame {

  val row0 = new JTextField("Row 0")
  val row1 = new JTextField("Row 1")
  val row2 = new JTextField("Row 2")
  val row3 = new JTextField("Row 3")
  val rowX = new JPanel()

  def launch = {
    val thisFrame = TenoriOnLCD 
    thisFrame.setLayout(new java.awt.GridLayout(4,1));
    thisFrame.setSize(new Dimension(500, 400))
    thisFrame.add(row0)
    thisFrame.add(row1)
    thisFrame.add(row2)
    thisFrame.add(row3)

    thisFrame.setVisible(true);
    thisFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  }
}

object SysExListener {
  def launch= {
    //val infos = MidiSystem.getMidiDeviceInfo()
    val infos = CoreMidiDeviceProvider.getMidiDeviceInfo()
    for (info <- infos) {
      val device = MidiSystem getMidiDevice info;
      if (device.getMaxTransmitters != 0) {
        device.open
        device.getTransmitter setReceiver new Receiver() {
          def send(message: MidiMessage, timeStamp: Long) = {
            if (message.isInstanceOf[SysexMessage]) {
              val sysExMessage = message.asInstanceOf[SysexMessage];
              val data = sysExMessage.getData();

              val str = new String(data) // , StandardCharsets.UTF_8)
              data(5) match {
                case 0 => TenoriOnLCD.row0.setText(str)
                case 1 => TenoriOnLCD.row1.setText(str)
                case 2 => TenoriOnLCD.row2.setText(str)
                case 3 => TenoriOnLCD.row3.setText(str)
                case _ => System.out.println("Row > 3 " + str)
              }
              
              for (byte <- data) {
                print(byte + " ")
              }
              print(str)
              println()
            }
          }
          def close() = {}
        }
      }
    }
  }
}


object main extends App {
  TenoriOnLCD.launch
  TenoriOnLCD.row0.setText("Hi")
  SysExListener.launch
}
