package UDP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class UDPClient {
    private static SocketAddress routerAddress;
    private static InetSocketAddress serverAddress;
    private static InetSocketAddress clientAddress;
    private boolean connection_established = false;
    private long sequence_number;

    private void runClient() throws IOException {
        try(DatagramChannel channel = DatagramChannel.open()){
            channel.bind(clientAddress);

            if(!connection_established)
                handShake(channel);

            if(connection_established){
                //This section is to test GET, to test POST comment this section and uncomment Post section
                //Get contents of /docs

                String msg = "/docs/jsonFile.json";
                sequence_number++;
                Packet p = createPacket(msg, PacketType.DATA.getValue());
                channel.send(p.toBuffer(), routerAddress);
                Packet resp = receivePacket(channel);
                StringBuilder response = new StringBuilder();

                while(resp.getType() != PacketType.FIN.getValue()){
                    String server_response = new String(resp.getPayload(), UTF_8);
                    //System.out.println("\n\nPacket payload is :\n" + server_response);

                    response.append(server_response);
                    ack_packet(resp.getSequenceNumber(), channel);
                    resp = receivePacket(channel);
                }

                System.out.println("\n---------Server response for Get file contents-----------\n" + response);


                //This section is to test POST, to test GET comment this section and uncomment GET section
                /**
                String msg = "Post new content to the file";
                sequence_number++;

                byte[] responseBytes = msg.getBytes(UTF_8);
                System.out.println("Length of Post message " + responseBytes.length);

                if(msg.getBytes().length <= (Packet.MAX_LEN-11)) {
                    Packet resp = createPacket(msg, PacketType.DATA.getValue());
                    System.out.println("\nSending Packet with Seq # :" + resp.getSequenceNumber());
                    channel.send(resp.toBuffer(), routerAddress);
                    Packet new_packet = receivePacket(channel);
                    if(new_packet.getType() == PacketType.ACK.getValue())
                        System.out.println(" \nSeq # of received packet : " + new_packet.getSequenceNumber());

                    sequence_number++;
                    Packet fin_packet = createPacket("", PacketType.FIN.getValue());
                    channel.send(fin_packet.toBuffer(), routerAddress);
                }

                else{
                    byte[][] payloads = getPayloads(responseBytes, (Packet.MAX_LEN-11));

                    for(int i = 0; i < payloads.length; i++) {
                        String payload = new String(payloads[i], UTF_8);
                        Packet resp = createPacket(payload, PacketType.DATA.getValue());
                        System.out.println("\nSending Packet with Seq # :" + resp.getSequenceNumber());
                        channel.send(resp.toBuffer(), routerAddress);

                        Packet new_packet = receivePacket(channel);
                        if(new_packet.getType() == PacketType.ACK.getValue())
                            System.out.println(" \nSeq # of received packet : " + new_packet.getSequenceNumber());

                        sequence_number++;
                    }

                    Packet fin_packet = createPacket("", PacketType.FIN.getValue());
                    channel.send(fin_packet.toBuffer(), routerAddress);
                }
                 */
            }
        }
    }

    public static void setRouter(String routerHost, int routerPort){
        routerAddress = new InetSocketAddress(routerHost, routerPort);
    }

    public static void setServer(String serverHost, int serverPort){
        serverAddress = new InetSocketAddress(serverHost, serverPort);
    }

    public static void setClientAddress(int clientPort){
        clientAddress = new InetSocketAddress(clientPort);
    }

    private Packet createPacket(String message, int type){
        Packet p = new Packet.Builder()
                .setType(type)
                .setSequenceNumber(sequence_number)
                .setPortNumber(serverAddress.getPort())
                .setPeerAddress(serverAddress.getAddress())
                .setPayload(message.getBytes())
                .create();
        return p;
    }

    private Packet receivePacket(DatagramChannel channel) throws IOException{

        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        buf.clear();
        channel.receive(buf);

        // Parse a packet from the received raw data.
        buf.flip();
        Packet packet = Packet.fromBuffer(buf);
        buf.flip();

        sequence_number++;
        return packet;
    }

    public static byte[][] getPayloads(byte[] response, int payload_size) {

        byte[][] payloads = new byte[(int)Math.ceil(response.length / (double)payload_size)][payload_size];

        int offset = 0;

        for(int i = 0; i < payloads.length; i++) {
            payloads[i] = Arrays.copyOfRange(response,offset, offset + payload_size);
            offset += payload_size ;
        }

        return payloads;
    }

    private void handShake(DatagramChannel channel) throws IOException {

        System.out.println("\n---------Establishing connection with server through 3-way handshake---------\n");

        //Handshake step 1

        String helloMessage = "Hi S";
        sequence_number = 1L;

        Packet packet1 = createPacket(helloMessage, PacketType.SYN.getValue());
        System.out.println("Sending SYN message with sequence number " + sequence_number);

        // Sending SYN and get SYN_ACK from the server.
        sends(channel, packet1);
        Packet server_packet = receivePacket(channel);

        //Handshake step 3
        if(server_packet.getType() == PacketType.SYN_ACK.getValue()) {
            String payload = new String(server_packet.getPayload(), UTF_8);
            System.out.println("Server SYN_ACK response : " + payload);
            sequence_number = server_packet.getSequenceNumber() + 1;

            // Din't put the timeout, cuz the server doesn't send anything to back to the client.
            ack_packet(server_packet.getSequenceNumber(), channel);
            connection_established = true;
        }
    }

    private void ack_packet(long seq_no, DatagramChannel channel) throws IOException {
        System.out.println(" \nSeq # of received packet : " + seq_no);
        Packet ack_packet = createPacket(String.valueOf(seq_no), PacketType.ACK.getValue());
        channel.send(ack_packet.toBuffer(), routerAddress);
    }

    private void sends(DatagramChannel channel, Packet packet) throws IOException{
        while(true){
            channel.send(packet.toBuffer(), routerAddress);

            // When you are using debug, you should switch to the server after send.
            // Otherwise, it keeps thinking the packet is dropped.
            // cuz server doesn't send anything.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            selector.select(100);

            Set<SelectionKey> keys = selector.selectedKeys();
            // Packet Not Dropped
            if(!keys.isEmpty()){
                break;
            }
        }
    }

    public static void main(String[] args) throws IOException {

        setRouter("localhost", 3000);
        setServer("localhost", 8007);
        setClientAddress(41830);
        new UDPClient().runClient();
    }
}

