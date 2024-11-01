package org.aguzman.hilos.ejemplotimer.Proyecto;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Servidor {
    private static final int PORT = 12346;
    private static Map<String, User> userDatabase = new ConcurrentHashMap<>();
    private static Map<Integer, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private static AtomicInteger clientIdGenerator = new AtomicInteger(1);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor de chat médico iniciado y esperando conexiones en el puerto " + PORT);

            // Usuarios de prueba
            userDatabase.put("doctor1", new User("doctor1", "password123", true));
            userDatabase.put("patient1", new User("patient1", "password456", false));

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream output;
        private ObjectInputStream input;
        private User authenticatedUser;
        private int clientId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());
                clientId = clientIdGenerator.getAndIncrement();
                connectedClients.put(clientId, this);

                String action = (String) input.readObject();
                System.out.println("Acción recibida: " + action);

                if ("register".equalsIgnoreCase(action)) {
                    handleRegistration();
                } else if ("login".equalsIgnoreCase(action)) {
                    handleLogin();
                }

                // Verificar autenticación antes de continuar
                if (authenticatedUser != null) {
                    System.out.println("Usuario autenticado: " + authenticatedUser.getUsername());
                    listenForMessages();
                } else {
                    System.out.println("Autenticación fallida para el cliente " + clientId);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error con el cliente " + clientId + ": " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void handleRegistration() throws IOException, ClassNotFoundException {
            String userType = (String) input.readObject();
            String username = (String) input.readObject();
            String password = (String) input.readObject();
            System.out.println("Registrando nuevo usuario: " + username);

            if (userDatabase.containsKey(username)) {
                output.writeObject("El nombre de usuario ya está en uso. Intente con otro.");
            } else {
                boolean isDoctor = "doctor".equalsIgnoreCase(userType);
                userDatabase.put(username, new User(username, password, isDoctor));
                output.writeObject("Registro exitoso. Ahora inicie sesión.");
            }
            output.flush();
        }

        private void handleLogin() throws IOException, ClassNotFoundException {
            String username = (String) input.readObject();
            String password = (String) input.readObject();
            System.out.println("Intento de inicio de sesión: " + username);

            User user = userDatabase.get(username);
            if (user != null && user.getPassword().equals(password)) {
                authenticatedUser = user;
                output.writeObject("Autenticación exitosa.");
            } else {
                output.writeObject("Autenticación fallida. Intente nuevamente.");
            }
            output.flush();
        }

        private void listenForMessages() throws IOException, ClassNotFoundException {
            while (true) {
                Object receivedObject = input.readObject();
                if (receivedObject instanceof String) {
                    System.out.println("Mensaje recibido de " + authenticatedUser.getUsername() + ": " + receivedObject);
                    broadcastMessage(authenticatedUser.getUsername() + ": " + receivedObject);
                }
            }
        }

        private void broadcastMessage(String message) {
            for (ClientHandler client : connectedClients.values()) {
                try {
                    client.output.writeObject(message);
                    client.output.flush();
                } catch (IOException e) {
                    System.err.println("Error al enviar mensaje a " + client.clientId + ": " + e.getMessage());
                }
            }
        }

        private void closeConnection() {
            try {
                if (socket != null) socket.close();
                connectedClients.remove(clientId);
                System.out.println("Conexión cerrada para el cliente " + clientId);
            } catch (IOException e) {
                System.err.println("Error al cerrar la conexión del cliente " + clientId + ": " + e.getMessage());
            }
        }
    }
}

// Clase User
class User implements Serializable {
    private final String username;
    private final String password;
    private final boolean isDoctor;

    public User(String username, String password, boolean isDoctor) {
        this.username = username;
        this.password = password;
        this.isDoctor = isDoctor;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isDoctor() {
        return isDoctor;
    }
}
