package org.aguzman.hilos.ejemplotimer.Proyecto;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Scanner scanner;

    public Cliente(String host, int port) {
        try {
            socket = new Socket(host, port);
            scanner = new Scanner(System.in);

            // Crear ObjectOutputStream antes de ObjectInputStream para evitar bloqueo
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            // Autenticación o registro
            authenticateOrRegister();

            // Iniciar un hilo para escuchar mensajes entrantes
            new Thread(this::listenForMessages).start();

            // Enviar mensajes o documentos al servidor
            sendMessageLoop();

        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void authenticateOrRegister() {
        try {
            while (true) {
                System.out.print("¿Desea registrarse o iniciar sesión? (escriba 'register' o 'login'): ");
                String choice = scanner.nextLine();
                output.writeObject(choice);
                output.flush();

                if ("register".equalsIgnoreCase(choice)) {
                    System.out.print("Ingrese el tipo de usuario (doctor/paciente): ");
                    String userType = scanner.nextLine();
                    System.out.print("Ingrese su nombre de usuario: ");
                    String username = scanner.nextLine();
                    System.out.print("Ingrese su contraseña: ");
                    String password = scanner.nextLine();

                    output.writeObject(userType);
                    output.writeObject(username);
                    output.writeObject(password);
                    output.flush();

                    String response = (String) input.readObject();
                    System.out.println(response);
                    if (response.equals("Registro exitoso. Ahora inicie sesión.")) {
                        continue; // Volver a solicitar inicio de sesión después de registrar
                    }
                } else if ("login".equalsIgnoreCase(choice)) {
                    System.out.print("Ingrese su nombre de usuario: ");
                    String username = scanner.nextLine();
                    System.out.print("Ingrese su contraseña: ");
                    String password = scanner.nextLine();

                    output.writeObject(username);
                    output.writeObject(password);
                    output.flush();

                    String response = (String) input.readObject();
                    System.out.println(response);
                    if (response.equals("Autenticación exitosa.")) {
                        break; // Salir del bucle si la autenticación es exitosa
                    }
                } else {
                    System.out.println("Opción no válida. Intente de nuevo.");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error durante la autenticación: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        try {
            while (true) {
                Object message = input.readObject();
                if (message instanceof String) {
                    System.out.println("Respuesta: " + message);
                } else if (message instanceof byte[]) {
                    saveDocument((byte[]) message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Conexión cerrada.");
            closeConnection();
        }
    }

    private void sendMessageLoop() {
        try {
            System.out.println("Escriba un mensaje para enviar o escriba 'upload' para cargar un documento:");

            while (true) {
                String text = scanner.nextLine();

                if ("upload".equalsIgnoreCase(text)) {
                    System.out.print("Ingrese la ruta del archivo: ");
                    String filePath = scanner.nextLine();
                    byte[] fileData = loadFile(filePath);
                    if (fileData != null) {
                        output.writeObject(fileData);
                        output.flush();
                        System.out.println("Documento enviado.");
                    } else {
                        System.out.println("Error al cargar el archivo. Verifique la ruta.");
                    }
                } else {
                    output.writeObject(text);
                    output.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Error al enviar el mensaje.");
            closeConnection();
        }
    }

    private byte[] loadFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return fis.readAllBytes();
        } catch (IOException e) {
            System.err.println("Error al cargar el archivo: " + e.getMessage());
            return null;
        }
    }

    private void saveDocument(byte[] fileData) {
        try (FileOutputStream fos = new FileOutputStream("document_received.dat")) {
            fos.write(fileData);
            System.out.println("Documento guardado como 'document_received.dat'.");
        } catch (IOException e) {
            System.err.println("Error al guardar el documento: " + e.getMessage());
        }
    }

    // Cerrar conexión y liberar recursos
    public void closeConnection() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Cliente("localhost", 12346);
    }
}
