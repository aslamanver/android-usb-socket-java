package com.aslam.usbsocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {

    public interface SocketServerListener {

        void onConnected(String message);

        void onDisconnected(EOFException exception);

        void onFailed(Exception exception);

        void onDataReceived(String data);
    }

    public static final String TAG = "ECR_SOCKET";
    public static final int PORT = 46568;

    private Thread socketThread;
    private Thread communicationThread;
    private SocketServerRunnable socketServerRunnable;
    private CommunicationThreadRunnable communicationThreadRunnable;
    private ServerSocket serverSocket;
    private Socket socket;
    private SocketServerListener socketClientListener;

    private DataInputStream in;
    private DataOutputStream out;

    public SocketServer(SocketServerListener socketClientListener) {
        this.socketClientListener = socketClientListener;
        socketServerRunnable = new SocketServerRunnable();
    }

    public void start() {

        if (socketThread != null && socketThread.isAlive()) {
            socketThread.interrupt();
        }
        if (communicationThread != null && communicationThread.isAlive()) {
            communicationThread.interrupt();
        }

        socketThread = new Thread(socketServerRunnable);
        socketThread.start();
    }

    public void stop() {

        if (communicationThreadRunnable != null) {
            communicationThreadRunnable.stop();
        }

        socketServerRunnable.stop();
        socketThread.interrupt();

        if (communicationThread != null) {
            communicationThread.interrupt();
        }
    }

    public void sendData(String data) {
        socketServerRunnable.sendData(data);
    }

    class SocketServerRunnable implements Runnable {

        @Override
        public void run() {

            try {

                serverSocket = new ServerSocket(PORT);

                while (true) {

                    socket = serverSocket.accept();

                    socketClientListener.onConnected(String.format("Client connected from: %s", socket.getRemoteSocketAddress().toString()));

                    in = new DataInputStream(socket.getInputStream());
                    socketClientListener.onDataReceived(in.readUTF());

                    out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("Thank you for connecting to " + socket.getLocalSocketAddress());

                    if (communicationThread != null && communicationThread.isAlive()) {
                        communicationThread.interrupt();
                    }

                    communicationThreadRunnable = new CommunicationThreadRunnable(socket);
                    communicationThread = new Thread(communicationThreadRunnable);
                    communicationThread.start();
                }

            } catch (IOException e) {

                if (e != null && e.getMessage().contains("Socket closed")) {
                    return;
                }

                socketClientListener.onFailed(e);
            } finally {
                socketThread.interrupt();
            }

        }

        public void sendData(final String data) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        out.writeUTF(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

        public void stop() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class CommunicationThreadRunnable implements Runnable {

        private Socket clientSocket;

        public CommunicationThreadRunnable(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    in = new DataInputStream(this.clientSocket.getInputStream());
                    socketClientListener.onDataReceived(in.readUTF());
                }
            } catch (EOFException e) {
                socketClientListener.onDisconnected(e);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                communicationThread.interrupt();
            }
        }

        public void stop() {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
