import java.awt.Dimension
import java.awt.geom._
import javax.swing._
import javax.swing.text._
import java.awt.Color
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException

import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem
import javax.sound.midi.Receiver
import javax.sound.midi.SysexMessage
import javax.sound.midi.MidiMessage

import scala.collection.mutable.ArrayBuffer

import java.awt.GraphicsEnvironment
import java.io.File


object SysExListener {
  // Tenori-On LCD Display MIDI Protocol from pika.blue
  val LCD_ROW_NUMBER = 5
  val INV_NUM_CHARS = 6
  val INV_START_CHAR = 7
  val START_OF_STR = 8
  val PREAMBLE = Array[Byte](67, 115, 1, 51, 2)
  val DEBUG = false

  def bit(value:Int, which:Int):Boolean={
    return ((value >> which) & 1) == 1
  }
  def getMask(extension_bytes:Array[Byte]):List[Boolean]= {
    //<ex0-6> = 7 bits, one for each of the first 7 characters, indicating whether the character should be OR'd with 0x80.
    //<ex7-13> = ditto, for the next 7 characters
    //<ex14-19> = ditto, for the next 7 characters.

    List(
      bit(extension_bytes(0), 0),  // whether to add 0x80 to character 0
      bit(extension_bytes(0), 1),  // whether to add 0x80 to character 1
      bit(extension_bytes(0), 2),  // whether to add 0x80 to character 2
      bit(extension_bytes(0), 3),  // whether to add 0x80 to character 3
      bit(extension_bytes(0), 4),  // whether to add 0x80 to character 4
      bit(extension_bytes(0), 5),  // whether to add 0x80 to character 5
      bit(extension_bytes(0), 6),  // whether to add 0x80 to character 6

      bit(extension_bytes(1), 0),  // whether to add 0x80 to character 7
      bit(extension_bytes(1), 1),  // whether to add 0x80 to character 8
      bit(extension_bytes(1), 2),  // whether to add 0x80 to character 9
      bit(extension_bytes(1), 3),  // whether to add 0x80 to character 10
      bit(extension_bytes(1), 4),  // whether to add 0x80 to character 11
      bit(extension_bytes(1), 5),  // whether to add 0x80 to character 12
      bit(extension_bytes(1), 6),  // whether to add 0x80 to character 13

      bit(extension_bytes(2), 0),  // whether to add 0x80 to character 14
      bit(extension_bytes(2), 1),  // whether to add 0x80 to character 15
      bit(extension_bytes(2), 2),  // whether to add 0x80 to character 16
      bit(extension_bytes(2), 3),  // whether to add 0x80 to character 17
      bit(extension_bytes(2), 4),  // whether to add 0x80 to character 18
      bit(extension_bytes(2), 5),  // whether to add 0x80 to character 19
    )
  }

  def launch= {
    //val infos = MidiSystem.getMidiDeviceInfo()  // Standard MIDI SysEx doesn't work on OS X so we use a library.
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

              if (data.length > START_OF_STR && data.slice(0,5).sameElements(PREAMBLE))
              {

                val baseBytes = data slice (START_OF_STR, START_OF_STR+20)
                val byteMask = getMask(data.slice(START_OF_STR+20, START_OF_STR+20+3))
                val reconstructed = baseBytes zip byteMask map (pair => pair._1 + (if (pair._2) 0x80 else 0))
                val reconstructedBytes = reconstructed map (x => x.toChar)

                val str = new String(reconstructedBytes) // , StandardCharsets.UTF_8)

                if (data(LCD_ROW_NUMBER) > 3  ) {
                  val dbg = reconstructed map (byte => (if (byte>15) "" else "0") + byte.toHexString.toUpperCase) mkString ""
                  Console.println(data(LCD_ROW_NUMBER) + ": " + dbg.grouped(4).toList.mkString(" "))
                }

                val invStart = data(INV_START_CHAR)
                val invLength = data(INV_NUM_CHARS)

                TenoriOnLCD.displayText(data(LCD_ROW_NUMBER), str, invStart, invLength)
              }

              if (DEBUG) {
                for (byte <- data) {
                  print(byte + " ")
                }
                println()
              }
            }
          }
          def close() = {}
        }
      }
    }
  }
}

object TenoriOnLCD extends JFrame {
    val sc = new StyleContext();
    var currentSize:Dimension = new Dimension(0,0)

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

    val invertedStyle = sc.addStyle("MYINVERTEDSTYLE", null);
    invertedStyle.addAttribute(StyleConstants.Foreground, Color.white);
    invertedStyle.addAttribute(StyleConstants.Background, Color.black);
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

    val MAX_FONT_SIZE = 70.0;
    val MIN_FONT_SIZE = 10.0;
    var fontSizeReal:Double = row0.getFont().getSize;
    var fontNameReal= "Tenori-On";  // row0.getFont().getName;

    addComponentListener(new ComponentAdapter() {
            override def componentResized(e:ComponentEvent) {
                val newSize:Double = getSize().width
                val oldSize:Double = currentSize.width
                val sizeChange:Double = newSize - oldSize
                val percentChange:Double = (sizeChange) / oldSize
                var fontSize = fontSizeReal
                var fontChange = fontSize + (fontSize*percentChange)
                fontChange = if (fontChange > MAX_FONT_SIZE) MAX_FONT_SIZE else fontChange
                fontChange = if (fontChange < MIN_FONT_SIZE) MIN_FONT_SIZE else fontChange
                row0.setFont(new Font(fontNameReal, Font.PLAIN, fontChange.intValue()))
                row1.setFont(new Font(fontNameReal, Font.PLAIN, fontChange.intValue()))
                row2.setFont(new Font(fontNameReal, Font.PLAIN, fontChange.intValue()))
                row3.setFont(new Font(fontNameReal, Font.PLAIN, fontChange.intValue()))
                fontSizeReal = fontChange

                currentSize = getSize()
            }
        });


  var allRows = ArrayBuffer[String]()
  def emitForMagicLeap(row:Int, text:String, invertStart:Int, invertLength:Int)={
      if (invertLength == 0) {
        allRows(row) = text
      } else {
        var s = text substring (0, invertStart)
        s += """<mark=#CCC$$C05 padding="10, 10, 0, 0">"""
        s += text substring (invertStart, invertStart+invertLength)
        s += """</mark>"""
        s += text substring (invertStart+invertLength, text.length)
        allRows(row) = s
      }
      println(allRows mkString "<BR>")
  }

  def displayText(row:Int, text:String, invStart:Int, invLength:Int)={
    row match {
      case 0 => { row0.setDocument(createDocument(text, invStart, invLength)) ; emitForMagicLeap(row,text,invStart,invLength) }
      case 1 => { row1.setDocument(createDocument(text, invStart, invLength)) ; emitForMagicLeap(row,text,invStart,invLength) }
      case 2 => { row2.setDocument(createDocument(text, invStart, invLength)) ; emitForMagicLeap(row,text,invStart,invLength) }
      case 3 => { row3.setDocument(createDocument(text, invStart, invLength)) ; emitForMagicLeap(row,text,invStart,invLength) }
      case _ => {  }
    }
  }


  def launch = {
    val thisFrame = TenoriOnLCD
    thisFrame.setLayout(new java.awt.GridLayout(4,1));
    val size = new Dimension(200, 250)
    thisFrame.setSize(size)
    thisFrame.currentSize = size
    thisFrame.add(row0)
    thisFrame.add(row1)
    thisFrame.add(row2)
    thisFrame.add(row3)

    allRows += ""
    allRows += ""
    allRows += ""
    allRows += ""


    thisFrame.setVisible(true);
    thisFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  }
}


trait hasPrintln {
      def println(s:String)
}

object Console extends JFrame with hasPrintln {
  var textArea:JTextArea = null
  def println(s:String){
    textArea.setText(textArea.getText() + s + "\n")
  }
  def launch= {
    val thisFrame = Console

    val version = {
      try {
        io.Source.fromResource("git-commit-hash.txt").getLines.next
      } catch {
        case e:Exception => {
          "unknown"
      }
      }
    }

    textArea = new JTextArea("Version "+version+"\nMessages for LCD row > 3 will go here.\n");
    textArea.setSize(300,300);

    textArea.setLineWrap(true);
    textArea.setEditable(true);
    textArea.setVisible(true);

    val caret = textArea.getCaret().asInstanceOf[DefaultCaret]
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);


    val scroll = new JScrollPane (textArea);
    thisFrame.add(scroll);
    thisFrame.setSize(new Dimension(400, 400))
    thisFrame.setVisible(true);
    thisFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  }
}


object main extends App {

  try {
    val customFont = Font.createFont(Font.TRUETYPE_FONT, (Thread.currentThread.getContextClassLoader.getResourceAsStream("Tenori-On.ttf"))).deriveFont(12f)
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    ge.registerFont(customFont)
  } catch {
    case e:Exception => {
      e.printStackTrace()
    }
    println("Could not load Tenori-On.ttf font from resource, trying to load from local file.")
    val customFont = Font.createFont(Font.TRUETYPE_FONT, (new File("Tenori-On.ttf"))).deriveFont(12f)
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    ge.registerFont(customFont)
  }




  Console.launch
  TenoriOnLCD.launch
  SysExListener.launch
}
