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
import java.nio.file.*;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class UDPServer {
    private static SocketAddress routerAddress;
    private static InetSocketAddress serverAddress;
    private static InetSocketAddress clientAddress;
    private boolean connection_established = false;
    private StringBuilder entity_body = new StringBuilder();
    private Path root = Paths.get("").toAbsolutePath();  //default system current dir
    private boolean post_request = false;

    public void listenAndServe(String[] args) throws IOException {

        httpfsLibrary httpfsLib = new httpfsLibrary(args);

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(serverAddress);
            System.out.println("Server is listening at " + channel.getLocalAddress());
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            for (; ; ) {
                Packet packet = receivePacket(buf, channel);

                if(packet.getType() == PacketType.SYN.getValue()) {
                    handShake(packet ,channel);
                }
                else if(connection_established){
                    String client_request = new String(packet.getPayload(), UTF_8);
                    if (client_request.startsWith("GET")) {
                        String response = httpfsLib.parseUDP_request(client_request);
                        processGetResponse(packet, response, channel, buf);
                    }

                    else if(client_request.startsWith("POST")){
                        post_request = true;
                        processPostResponse(client_request, packet, channel, httpfsLib);
                    }

                    else if(post_request){
                        if(packet.getType() == PacketType.FIN.getValue()){
                            String response = httpfsLib.parseUDP_request(entity_body.toString());
                            sendPostResponse(packet, response, channel, buf);
                            post_request = false;
                        }

                        else
                            processPostResponse(client_request, packet, channel, httpfsLib);
                    }


                }

            }
        }
    }

    public void processGetResponse(Packet packet, String response, DatagramChannel channel, ByteBuffer buf) throws IOException {
        byte[] responseBytes = response.getBytes(UTF_8);
        System.out.println("Length of doc returned " + responseBytes.length);

        if(response.getBytes().length <= (Packet.MAX_LEN-11)) {
            Packet resp = createPacket(packet, response, packet.getSequenceNumber()+1, PacketType.DATA.getValue());
            System.out.println("\nSending Packet with Seq # :" + resp.getSequenceNumber());
            channel.send(resp.toBuffer(), routerAddress);
            Packet new_packet = receivePacket(buf, channel);
            if(new_packet.getType() == PacketType.ACK.getValue())
                ack_packet(new_packet);

            Packet fin_packet = createPacket(packet, "", new_packet.getSequenceNumber()+1, PacketType.FIN.getValue());
            channel.send(fin_packet.toBuffer(), routerAddress);
        }

        else{
            List<byte[]> payloads = getPayloads(responseBytes, (Packet.MAX_LEN-11));
            long seq_no = packet.getSequenceNumber()+1;

            for (byte[] payload : payloads) {
                String payload_data = new String(payload, UTF_8);
                Packet resp = createPacket(packet, payload_data, seq_no, PacketType.DATA.getValue());
                channel.send(resp.toBuffer(), routerAddress);
                Packet new_packet = receivePacket(buf, channel);
                if(new_packet.getType() == PacketType.ACK.getValue())
                    ack_packet(new_packet);
                seq_no = new_packet.getSequenceNumber()+1;
            }

            Packet fin_packet = createPacket(packet, "", seq_no, PacketType.FIN.getValue());
            channel.send(fin_packet.toBuffer(), routerAddress);
        }
    }

    public void processPostResponse(String client_request, Packet packet, DatagramChannel channel, httpfsLibrary httpfsLib) throws IOException {
        entity_body.append(client_request);
        Packet ack_packet = createPacket(packet, "", packet.getSequenceNumber()+1, PacketType.ACK.getValue());
        System.out.println("\nSending Packet with Seq # :" + ack_packet.getSequenceNumber());
        channel.send(ack_packet.toBuffer(), routerAddress);
    }

    private void sendPostResponse(Packet packet, String response, DatagramChannel channel, ByteBuffer buf) throws IOException {
        Packet resp = createPacket(packet, response, packet.getSequenceNumber()+1, PacketType.DATA.getValue());
        System.out.println("\nSending Packet with Seq # :" + resp.getSequenceNumber());
        channel.send(resp.toBuffer(), routerAddress);
        Packet new_packet = receivePacket(buf, channel);
        if(new_packet.getType() == PacketType.ACK.getValue())
            ack_packet(new_packet);
    }

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

    private void handShake(Packet packet, DatagramChannel channel) throws IOException {

        //Handshake step 2
        System.out.println("\n---------Establishing connection with client through 3-way handshake---------\n");
        String payload = new String(packet.getPayload(), UTF_8);
        System.out.println("Client message : " + payload);

        String responseMessage = ("Hi");
        long seq_no = packet.getSequenceNumber() + 1;
        Packet syn_ack_response = createPacket(packet, responseMessage, seq_no, PacketType.SYN_ACK.getValue());
        System.out.println("Sending SYN_ACK message: Hi and Seq # :" + seq_no);
        channel.send(syn_ack_response.toBuffer(), routerAddress);
        ByteBuffer buf = ByteBuffer
                .allocate(Packet.MAX_LEN)
                .order(ByteOrder.BIG_ENDIAN);

        // Send SYN_ACK packet to the client.
        sends(channel, syn_ack_response);
        Packet ack_packet = receivePacket(buf, channel);

        if(ack_packet.getType() == PacketType.ACK.getValue()) {
            ack_packet(ack_packet);
            connection_established = true;
            System.out.println("\n---------Connection with client established---------\n\n");
        }
        else if(ack_packet.getType() == PacketType.DATA.getValue()){
            System.out.println("\n---------Connection failed---------\n\n");
            System.exit(0);
        }

        /*
        Packet ack_packet = receivePacket(buf, channel);
        if(ack_packet.getType() == PacketType.ACK.getValue()) {
            ack_packet(ack_packet);
            connection_established = true;
            System.out.println("\n---------Connection with client established---------\n\n");
        }
        */
    }

    private void ack_packet(Packet client_packet) throws IOException {
        String ack_payload = new String(client_packet.getPayload(), UTF_8);
        System.out.println("Client ACK # : " + ack_payload);
    }

    private Packet createPacket(Packet packet, String message, long sequence_number, int type){
        Packet p = packet.toBuilder()
                .setType(type)
                .setSequenceNumber(sequence_number)
                .setPayload(message.getBytes())
                .create();
        return p;
    }

    private void sends(DatagramChannel channel, Packet packet) throws IOException{
        channel.send(packet.toBuffer(), routerAddress);
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        selector.select(100);

        Set<SelectionKey> keys = selector.selectedKeys();
        if (keys.isEmpty()) {
            sends(channel,packet);
        }

        selector.close();
        keys.clear();
        return;
    }

    private Packet receivePacket(ByteBuffer buf, DatagramChannel channel) throws IOException{

        channel.configureBlocking(true);
        buf.clear();
        routerAddress = channel.receive(buf);

        // Parse a packet from the received raw data.
        buf.flip();
        Packet packet = Packet.fromBuffer(buf);
        buf.flip();

        System.out.println("Receiving client packet with Seq # : " + packet.getSequenceNumber());

        return packet;
    }

    public void setServer(int serverPort){
        serverAddress = new InetSocketAddress(serverPort);
    }
}