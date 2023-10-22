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
import javax.swing.text._
import java.awt.Color


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
    val sc = new StyleContext();

    def createDocument(text:String, invertStart:Int, invertLength:Int)={
      val result = new DefaultStyledDocument(sc)
      if (invertLength == 0) {
        result insertString (0, text, null)
      } else {
        result insertString (0, text substring (0, invertStart), null)
        result insertString (invertStart, text substring (invertStart, invertStart+invertLength), invertedStyle)
        result insertString (invertStart+invertLength, text substring (invertStart+invertLength, text.length), null)
      }
      result
    }

    val invertedStyle = sc.addStyle("BLUE", null);
    invertedStyle.addAttribute(StyleConstants.Foreground, Color.white);
    invertedStyle.addAttribute(StyleConstants.Background, Color.black);
    invertedStyle.addAttribute(StyleConstants.FontSize, new Integer(14));
    invertedStyle.addAttribute(StyleConstants.Bold, true);

    val doc0 = createDocument("Row 0", 0, 0)
    val doc1 = createDocument("Row 1", 0, 0)
    val doc2 = createDocument("Row 2", 0, 0)
    val doc3 = createDocument("Row 3", 0, 0)


  val row0 = new JTextPane(doc0)
  val row1 = new JTextPane(doc1)
  val row2 = new JTextPane(doc2)
  val row3 = new JTextPane(doc3)
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
  val LCD_ROW_NUMBER = 5
  val INV_NUM_CHARS = 6
  val INV_START_CHAR = 7
  val START_OF_STR = 8

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

              val PREAMBLE = Array[Byte](67, 115, 1, 51, 2)
              if (data.length > START_OF_STR && data.slice(0,5).sameElements(PREAMBLE)) 
              {
                val str = new String(data) // , StandardCharsets.UTF_8)
                val invStart = data(INV_START_CHAR)
                val invLength = data(INV_NUM_CHARS)
                data(LCD_ROW_NUMBER) match {
                  //case 0 => TenoriOnLCD.row0.setText(str)
                  case 0 => TenoriOnLCD.row0.setDocument(TenoriOnLCD.createDocument(str substring(START_OF_STR, START_OF_STR+20), invStart, invLength))
                  case 1 => TenoriOnLCD.row1.setDocument(TenoriOnLCD.createDocument(str substring(START_OF_STR, START_OF_STR+20), invStart, invLength))
                  case 2 => TenoriOnLCD.row2.setDocument(TenoriOnLCD.createDocument(str substring(START_OF_STR, START_OF_STR+20), invStart, invLength))
                  case 3 => TenoriOnLCD.row3.setDocument(TenoriOnLCD.createDocument(str substring(START_OF_STR, START_OF_STR+20), invStart, invLength))
                  case _ => System.out.println("Row > 3 " + str)
                }
              }
              
              for (byte <- data) {
                print(byte + " ")
              }
              //print(str)
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
  SysExListener.launch
}
