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
    private StringBuilder response = new StringBuilder();

    /**
     * Constructor for the UDPClient class. Sets the router and server ports.
     * Sets the client port
     */
    public UDPClient(){
        setRouter("localhost", 3000);
        setServer("localhost", 8007);
        setClientAddress(41830);
    }

    /**
     * Creates the UDP Channel connection, handshake protocol and sends the requests on behalf of the client library
     * @param request String representing the HTTP request (post or get).
     * @param requestMethod String for the HTTP method
     */
    public void runClient(String request, String requestMethod) throws IOException {
        try(DatagramChannel channel = DatagramChannel.open()){
            channel.bind(clientAddress);

            if(!connection_established)
                handShake(channel);

            if(connection_established){
                if(requestMethod.equals("get"))
                    send_get_request(request, channel);

                else if(requestMethod.equals("post"))
                    send_post_request(request, channel);

            }
        }
    }

    /**
     * Sets the router port and address
     * @param routerHost String representing the router local address.
     * @param routerPort String for the router port
     */
    public static void setRouter(String routerHost, int routerPort){
        routerAddress = new InetSocketAddress(routerHost, routerPort);
    }

    /**
     * Sets the server port and address
     * @param serverHost String representing the server local address.
     * @param serverPort String for the server port
     */
    public static void setServer(String serverHost, int serverPort){
        serverAddress = new InetSocketAddress(serverHost, serverPort);
    }

    /**
     * Sets the client port
     * @param clientPort String representing the client port
     */
    public static void setClientAddress(int clientPort){
        clientAddress = new InetSocketAddress(clientPort);
    }

    /**
     * Creates a UDP Packet object
     * @param message String the message or payload of the packet
     * @param type type of packet
     * @return Packet object
     */
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

    /**
     * Receives a Datagram Bytebuffer and returns a new Packet
     * @param channel DatagramChannel object
     * @return Packet object
     */
    private Packet receivePacket(DatagramChannel channel) throws IOException{

        channel.configureBlocking(true);
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

    /**
     * Segments the payload into chunks of 1013 bytes each
     * @param response HTTP response message
     * @param payload_size size of the response
     * @return List<byte[]> a list of byte objects representing each Packet's payload
     */
    public static List<byte[]> getPayloads(byte[] response, int payload_size) {

        List<byte[]> payloads = new ArrayList<byte[]>();
        int offset = 0;

        while (offset < response.length) {
            int end = Math.min(response.length, offset + payload_size);
            payloads.add(Arrays.copyOfRange(response, offset, end));
            offset += payload_size;
        }

        return payloads;
    }

    /**
     * simulation of 3-way handshake similar to TCP protocol
     * @param channel DatagramChannel object
     */
    private void handShake(DatagramChannel channel) throws IOException {

        System.out.println("\n---------Establishing connection with server through 3-way handshake---------\n");

        //Handshake step 1

        String helloMessage = "Hi S";
        sequence_number = 1L;

        Packet packet1 = createPacket(helloMessage, PacketType.SYN.getValue());

        System.out.println("Sending SYN message with sequence number " + sequence_number);
        //channel.send(packet1.toBuffer(), routerAddress);
        sends(channel, packet1);

        Packet server_packet = receivePacket(channel);
        //Handshake step 3
        if(server_packet.getType() == PacketType.SYN_ACK.getValue()) {
            String payload = new String(server_packet.getPayload(), UTF_8);
            System.out.println("Server SYN_ACK response : " + payload);
            connection_established = true;
            sequence_number = server_packet.getSequenceNumber() + 1;
            ack_packet(server_packet.getSequenceNumber(), channel);
        }
    }

    /**
     * Creates an ACK packet for the received packet and sends it to UDP Server
     * @param seq_no sequence number of the received packet
     * @param channel DatagramChannel object
     */
    private void ack_packet(long seq_no, DatagramChannel channel) throws IOException {
        System.out.println(" \nSeq # of received packet : " + seq_no);
        Packet ack_packet = createPacket(String.valueOf(seq_no), PacketType.ACK.getValue());
        channel.send(ack_packet.toBuffer(), routerAddress);
    }

    /**
     * Sends a Packet with Selector timer, recursively sends after timeout
     * @param packet Packet to be sent
     * @param channel DatagramChannel object
     */
    private void sends(DatagramChannel channel, Packet packet) throws IOException{
        channel.send(packet.toBuffer(), routerAddress);
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        selector.select(1000);

        Set<SelectionKey> keys = selector.selectedKeys();
        if (keys.isEmpty()) {
            sends(channel,packet);
        }

        selector.close();
        keys.clear();
        return;
    }

    /**
     * Gets the HTTP response data from Server
     * @return HTTP response string
     */
    public String get_response(){
        return this.response.toString();
    }

    /**
     * Sends the get request Packets to Server
     * @param request string representing the HTTP Request
     * @param channel DatagramChannel object
     */
    private void send_get_request(String request, DatagramChannel channel) throws IOException {
        sequence_number++;
        Packet p = createPacket(request, PacketType.DATA.getValue());
        //channel.send(p.toBuffer(), routerAddress);
        sends(channel,p);
        Packet resp = receivePacket(channel);

        while(resp.getType() != PacketType.FIN.getValue()){
            String server_response = new String(resp.getPayload(), UTF_8);
            response.append(server_response);
            ack_packet(resp.getSequenceNumber(), channel);
            resp = receivePacket(channel);
        }
    }

    /**
     * Sends the post request Packets to Server
     * @param request string representing the HTTP Request
     * @param channel DatagramChannel object
     */
    private void send_post_request(String request, DatagramChannel channel) throws IOException {
        sequence_number++;
        byte[] responseBytes = request.getBytes(UTF_8);
        System.out.println("Length of Post message " + responseBytes.length);

        if(request.getBytes().length <= (Packet.MAX_LEN-11)) {
            Packet resp = createPacket(request, PacketType.DATA.getValue());
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
            List<byte[]> payloads = getPayloads(responseBytes, (Packet.MAX_LEN-11));

            for (byte[] payload : payloads) {
                String payload_data = new String(payload, UTF_8);
                Packet resp = createPacket(payload_data, PacketType.DATA.getValue());
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

        Packet response_packet = receivePacket(channel);
        String server_response = new String(response_packet.getPayload(), UTF_8);
        response.append(server_response);
        ack_packet(response_packet.getSequenceNumber(), channel);
    }
}

