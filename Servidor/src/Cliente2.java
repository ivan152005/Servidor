import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente2 {
    private static final String SERVIDOR_IP = "localhost";
    private static final int PUERTO = 6000;

    private JFrame ventana;
    private JTextField txtUsuario;
    private JPasswordField txtContrasena;
    private JTextArea areaMensajes;
    private JButton btnConectar;
    private JButton btnDesconectar;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean conectado = false;

    public Cliente2() {
        crearInterfaz();
    }

    // Creación de la interfaz gráfica del cliente
    private void crearInterfaz() {
        ventana = new JFrame("Cliente - Iniciar Sesión");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setSize(400, 300);
        ventana.setLayout(new BorderLayout());

        JPanel panelSuperior = new JPanel(new GridLayout(3, 2, 5, 5));
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelSuperior.add(new JLabel("Usuario:"));
        txtUsuario = new JTextField();
        panelSuperior.add(txtUsuario);

        panelSuperior.add(new JLabel("Contraseña:"));
        txtContrasena = new JPasswordField();
        panelSuperior.add(txtContrasena);

        btnConectar = new JButton("Conectar");
        btnDesconectar = new JButton("Desconectar");
        btnDesconectar.setEnabled(false);

        panelSuperior.add(btnConectar);
        panelSuperior.add(btnDesconectar);

        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(areaMensajes);

        ventana.add(panelSuperior, BorderLayout.NORTH);
        ventana.add(scrollPane, BorderLayout.CENTER);

        // Acción al presionar el botón "Conectar"
        btnConectar.addActionListener(e -> conectarAlServidor());

        // Acción al presionar el botón "Desconectar"
        btnDesconectar.addActionListener(e -> desconectar());

        ventana.setVisible(true);
    }

    // Método para conectar al servidor
    private void conectarAlServidor() {
        if (conectado) {
            areaMensajes.append("Ya estás conectado.\n");
            return;
        }

        String usuario = txtUsuario.getText().trim();
        String contrasena = new String(txtContrasena.getPassword()).trim();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            areaMensajes.append("Error: Usuario y contraseña no pueden estar vacíos.\n");
            return;
        }

        try {
            socket = new Socket(SERVIDOR_IP, PUERTO);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            areaMensajes.append("Conectado al servidor...\n");

            // Enviar usuario y contraseña
            out.println(usuario);
            out.println(contrasena);

            // Deshabilitar los campos de usuario y contraseña mientras se está conectado
            SwingUtilities.invokeLater(() -> {
                txtUsuario.setEnabled(false);
                txtContrasena.setEnabled(false);
                btnConectar.setEnabled(false);
                btnDesconectar.setEnabled(true);
            });

            conectado = true;

            // Hilo para recibir mensajes del servidor
            new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = in.readLine()) != null) {
                        final String mensajeRecibido = mensaje;
                        SwingUtilities.invokeLater(() -> areaMensajes.append(mensajeRecibido + "\n"));

                        // Si la contraseña es incorrecta, permitir reintentar sin desconectar
                        if (mensajeRecibido.contains("Error: Nombre de usuario o contraseña incorrectos.") ||
                                mensajeRecibido.contains("Error: El usuario ya existe con una contraseña diferente.")) {
                            SwingUtilities.invokeLater(() -> {
                                txtUsuario.setEnabled(true);
                                txtContrasena.setEnabled(true);
                                btnConectar.setEnabled(true);
                                btnDesconectar.setEnabled(false);
                            });
                            socket.close();
                            conectado = false;
                            return;
                        }

                        // Si el servidor avisa que se apagará en 3 segundos, iniciar la cuenta regresiva
                        if (mensajeRecibido.contains("Servidor apagándose en 3 segundos...")) {
                            iniciarCuentaRegresiva();
                        }

                        // Si el cliente es expulsado o el servidor se apaga
                        if (mensajeRecibido.contains("expulsado") || mensajeRecibido.contains("desconectado") ||
                                mensajeRecibido.contains("El servidor se ha cerrado")) {
                            desconectar();
                            break;
                        }
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> areaMensajes.append("Desconectado del servidor.\n"));
                }
            }).start();

        } catch (IOException e) {
            areaMensajes.append("Error al conectar con el servidor: " + e.getMessage() + "\n");
            btnConectar.setEnabled(true);
            btnDesconectar.setEnabled(false);
        }
    }

    // Método para iniciar una cuenta regresiva en la interfaz del cliente
    private void iniciarCuentaRegresiva() {
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    final int tiempo = i;
                    SwingUtilities.invokeLater(() -> areaMensajes.append("El servidor se apagará en " + tiempo + " segundos...\n"));
                    Thread.sleep(1000);
                }
                SwingUtilities.invokeLater(() -> {
                    areaMensajes.append("Servidor apagado. Desconectando...\n");
                    desconectar();
                });
            } catch (InterruptedException e) {
                areaMensajes.append("Error en la cuenta regresiva.\n");
            }
        }).start();
    }

    // Método para desconectarse del servidor
    // Método para desconectarse del servidor
    private void desconectar() {
        try {
            if (out != null) {
                out.println("SALIR"); // Enviar un mensaje especial al servidor
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            conectado = false;

            // Habilitar los campos de usuario y contraseña nuevamente
            SwingUtilities.invokeLater(() -> {
                txtUsuario.setEnabled(true);
                txtContrasena.setEnabled(true);
                btnConectar.setEnabled(true);
                btnDesconectar.setEnabled(false);
            });

            areaMensajes.append("Desconectado del servidor.\n");
        } catch (IOException e) {
            areaMensajes.append("Error al desconectar: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        new Cliente();
    }
}