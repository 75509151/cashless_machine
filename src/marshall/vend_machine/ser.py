import threading
import serial
import binascii
import traceback
import struct
import Queue


class MachineSer(object):
    """docstring for MarshallSer"""
    SERIAL_PARAMS = {"port": "",
                     "boudrate": 115200,   # according to the marshall protocol v1.11
                     "bytesize": serial.EIGHTBITS,
                     "stopbits": serial.STOPBITS_ONE,
                     "parity": serial.PARITY_NONE
                     }

    def __init__(self, que):
        self.ser = None
        self.rx_thread = None
        self.tx_thread = None
        self.machine_que = que

    def get_available_port(self):
        pass

    def open(self, port):
        try:
            self.ser = serial.Serial(port, MachineSer.SERIAL_PARAMS["boudrate"],
                                     bytesize=MachineSer.SERIAL_PARAMS["bytesize"], stopbits=MachineSer.SERIAL_PARAMS["stopbits"],
                                     parity=MachineSer.SERIAL_PARAMS["parity"], timeout=0.2)
            self.ser.flushInput()
            self.ser.flushOutput()
            return True
        except Exception:
            print str(traceback.format_exc())
            return False

    def is_open(self):
        if self.ser:
            return self.ser.isOpen()
        else:
            return False

    def set_port_params(self, port):
        try:
            pass
        except Exception, e:
            raise e

    def init_threads(self):
        self.tx_thread = SerialWrite(self)
        self.rx_thread = SerialRead(self)
        self.rx_thread.setDaemon(True)
        self.tx_thread.setDaemon(True)
        self.rx_thread.start()
        self.tx_thread.start()

    def close(self):
        try:
            if self.ser:
                self.ser.close()
                return True
        except Exception:
            print str(traceback.format_exc())
        return False


class SerialRead(threading.Thread):

    def __init__(self, ma):
        super(SerialRead, self).__init__()
        self.ma = ma
        self.r_b = ""

    def run(self):
        ma = self.ma
        while True:
            try:
                if ma.is_open():
                    self.r_b = ma.ser.read(1000)
                    if self.r_b and (len(self.r_b) >= 11):
                        print "RX:%s, %s" % (binascii.hexlify(self.r_b), binascii.hexlify(self.r_b[8]))
                        total_len = len(self.r_b)
                        offset = 0
                        enc_len = 0

                        while offset < total_len:
                            enc_len = self.get_packet_len(offset)
                            # print enc_len
                            if enc_len == 0:
                                break
                            if total_len - offset < 2:
                                break
                            if total_len < enc_len + 2:
                                print "Encoded Length %02d\n" % enc_len
                                print "Total Length  %02d\n" % total_len
                                break
                            raw_msg = self.r_b[offset: offset + enc_len + 2]

                            offset += enc_len + 2
                            msg = MarshallProtocolMsg.factory(raw_msg)
                            if msg:
                                print "msg type:%s" % type(msg)
                                ma.marshall_que.put(msg)
                            else:
                                print "MarshallProtocolMessage None \n"

                # else:
                #     print ma.is_open()
            except SerialException:
                print "serial error: %s" % str(traceback.format_exc())
            except KeyboardInterrupt, e:
                print str(traceback.format_exc())
                raise e

            except Exception:
                print traceback.format_exc()
                # raise e

    def get_packet_len(self, offset):
        length = 0
        length, = struct.unpack("<H", self.r_b[offset:offset + 2])
        # print "len: %s" % length
        return length


class SerialWrite(threading.Thread):
    def __init__(self, ma):
        super(SerialWrite, self).__init__()
        self.ma = ma
        self.tx_que = Queue.Queue(2)

    def run(self):
        while True:
            try:
                msg = self.tx_que.get()
                if msg:
                    buffer = msg.build_tx_buffer()
                    self.ma.ser.write(buffer)
                    print "TX:%s" % binascii.hexlify(buffer)
            except Exception:
                print str(traceback.format_exc())
