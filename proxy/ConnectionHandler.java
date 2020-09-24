package proxy;

import java.io.*;
import java.net.*;

class ConnectionHandler extends Thread {
    Socket clientSocket;
    InputStream streamFromClient;
    OutputStream streamToClient;
    String node;
    Socket nodeSocket;
    byte[] request;
    byte[] reply;
    InputStream streamFromNode;
    OutputStream streamToNode;

    public ConnectionHandler(Socket clientSocket, String node) {
        this.clientSocket = clientSocket;
        this.node = node;
        this.request = new byte[1024];
        this.reply = new byte[4096];
    }

    @Override
    public void run() {
        try {
            // init streams from client
            streamFromClient = clientSocket.getInputStream();
            streamToClient = clientSocket.getOutputStream();

            // create a socket connection to the node.
            try {
                nodeSocket = new Socket(this.node, 8026);
            } catch (IOException e) {
                PrintWriter out = new PrintWriter(streamToClient);
                out.print("Proxy server cannot connect to " + node + ":" + 8026 + ":\n" + e + "\n");
                out.flush();
                try {
                    clientSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }

            // create the streams to the node.
            streamFromNode = nodeSocket.getInputStream();
            streamToNode = nodeSocket.getOutputStream();

            // a new thread to read from client and send to node.
            new Thread () {
                public void run() {
                    int bytesRead;
                    System.out.println("Reading from client.");
                    try {
                        while ((bytesRead = streamFromClient.read(request)) != -1) {
                            streamToNode.write(request, 0, bytesRead);
                            streamToNode.flush();
                        }
                        System.out.println("Reading from client complete, closing streamToNode.");
                    } catch (IOException e) {
                    }

                    try {
                        streamToNode.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

            // this current thread reads response from node and forwards to client.
            int bytesRead;
            try {
                
                System.out.println("Reading from node.");
                while ((bytesRead = streamFromNode.read(reply)) != -1) {
                    streamToClient.write(reply, 0, bytesRead);
                    streamToClient.flush();
                }
                System.out.println("Wrote to client.");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (nodeSocket != null)
                    nodeSocket.close();
                    System.out.println("Close nodesocket.");
                if (clientSocket != null)
                    clientSocket.close();
                    System.out.println("Close clientsocket.");
            }
            streamToClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 
}