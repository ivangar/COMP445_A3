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
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
                //TEST Get contents of /test.txt
                String msg = "/jsonFile.json";
                Packet p = createPacket(msg, PacketType.DATA.getValue());
                channel.send(p.toBuffer(), routerAddress);
                Packet resp = receivePacket(channel);
                StringBuilder response = new StringBuilder();
                int packet_number = 1;

                while(resp.getType() != PacketType.FIN.getValue()){
                    String server_response = new String(resp.getPayload(), UTF_8);
                    //System.out.println("\n\nPacket payload is :\n" + server_response);
                    System.out.println("\nPacket # is :" + packet_number);
                    response.append(server_response);
                    resp = receivePacket(channel);
                    packet_number++;
                }


                System.out.println("\n---------Server response for Get file contents-----------\n" + response);

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

        return packet;
    }

    private void handShake(DatagramChannel channel) throws IOException {

        System.out.println("\n---------Establishing connection with server through 3-way handshake---------\n");

        //Handshake step 1

        String helloMessage = "Hi S";
        sequence_number = 1L;

        Packet packet1 = createPacket(helloMessage, PacketType.SYN.getValue());

        System.out.println("Sending SYN message with sequence number " + sequence_number);
        channel.send(packet1.toBuffer(), routerAddress);

        Packet server_packet = receivePacket(channel);
        //Handshake step 3
        if(server_packet.getType() == PacketType.SYN_ACK.getValue()) {
            String payload = new String(server_packet.getPayload(), UTF_8);
            System.out.println("Server SYN_ACK response : " + payload);
            connection_established = true;
            sequence_number = server_packet.getSequenceNumber() + 1;
            Packet ack_packet = createPacket(String.valueOf(server_packet.getSequenceNumber()), PacketType.ACK.getValue());
            channel.send(ack_packet.toBuffer(), routerAddress);
        }
    }

    public static void main(String[] args) throws IOException {

        setRouter("localhost", 3000);
        setServer("localhost", 8007);
        setClientAddress(41830);
        new UDPClient().runClient();
    }
}

