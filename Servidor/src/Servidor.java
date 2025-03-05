import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Servidor {
    private static final int PUERTO = 6000;
    private static final int MAX_CLIENTES = 10;
    private static final Logger logger = Logger.getLogger(Servidor.class.getName());

    private DefaultListModel<String> modeloListaClientes;
    private JList<String> listaClientes;
    private ExecutorService hilosClientes;
    private ServerSocket servidor;
    private boolean ejecutando = true;
    private Map<String, Socket> clientesConectados;
    private JFrame ventana;

    public Servidor() {
        configurarLogger();
        clientesConectados = new HashMap<>();
        SwingUtilities.invokeLater(this::crearInterfaz);
    }

    private void configurarLogger() {
        try {
            FileHandler fileHandler = new FileHandler("servidor.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("No se pudo configurar el logger: " + e.getMessage());
        }
    }

    private void crearInterfaz() {
        ventana = new JFrame("Servidor - Clientes Conectados");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setSize(500, 400);
        ventana.setLayout(new BorderLayout());

        modeloListaClientes = new DefaultListModel<>();
        listaClientes = new JList<>(modeloListaClientes);
        JScrollPane scrollPane = new JScrollPane(listaClientes);
        ventana.add(scrollPane, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel();
        JButton btnExpulsar = new JButton("Expulsar Cliente");
        JButton btnDetener = new JButton("Detener Servidor");

        //Botón para expulsar clientes
        btnExpulsar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expulsarClienteSeleccionado();
            }
        });

        //Botón para detener el servidor
        btnDetener.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                detenerServidor();
            }
        });

        panelBotones.add(btnExpulsar);
        panelBotones.add(btnDetener);
        ventana.add(panelBotones, BorderLayout.SOUTH);

        ventana.setVisible(true);
    }

    private void iniciarServidor() {
        hilosClientes = Executors.newFixedThreadPool(MAX_CLIENTES);

        try {
            servidor = new ServerSocket(PUERTO);
            logger.info("Servidor iniciado en el puerto " + PUERTO);

            while (ejecutando) {
                try {
                    Socket cliente = servidor.accept();
                    logger.info("Cliente conectado desde " + cliente.getInetAddress());

                    ManejadorCliente manejador = new ManejadorCliente(cliente, this);
                    hilosClientes.execute(manejador);
                } catch (IOException e) {
                    if (!ejecutando) {
                        logger.info("Servidor detenido.");
                        break;
                    }
                    logger.warning("Error al aceptar cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe("Error en el servidor: " + e.getMessage());
        } finally {
            detenerServidor();
        }
    }

    public synchronized void agregarCliente(String nombreUsuario, String ip, Socket socket) {
        String clienteInfo = nombreUsuario + " - " + ip;
        SwingUtilities.invokeLater(() -> modeloListaClientes.addElement(clienteInfo));
        clientesConectados.put(clienteInfo, socket);
    }

    public synchronized void expulsarCliente(String nombreUsuario) {
        String clienteInfo = clientesConectados.keySet().stream()
                .filter(info -> info.startsWith(nombreUsuario + " -"))
                .findFirst().orElse(null);

        if (clienteInfo != null) {
            clientesConectados.remove(clienteInfo);

            SwingUtilities.invokeLater(() -> {
                modeloListaClientes.removeElement(clienteInfo);
                logger.info("Cliente desconectado: " + clienteInfo);
            });
        } else {
            logger.warning("Intento de expulsar cliente no encontrado: " + nombreUsuario);
        }
    }


    private void expulsarClienteSeleccionado() {
        String clienteSeleccionado = listaClientes.getSelectedValue();
        if (clienteSeleccionado != null && clientesConectados.containsKey(clienteSeleccionado)) {
            try {
                Socket socket = clientesConectados.get(clienteSeleccionado);
                if (socket != null && !socket.isClosed()) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("Has sido expulsado del servidor.");
                    socket.close();
                }
                expulsarCliente(clienteSeleccionado.split(" - ")[0]);
            } catch (IOException e) {
                logger.severe("Error al expulsar cliente: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(null, "Seleccione un cliente para expulsar.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    //Método para detener el servidor y expulsar a todos los clientes
    private void detenerServidor() {
        ejecutando = false;
        logger.info("Deteniendo servidor...");

        //Notificar a los clientes que el servidor se cerrará en 3 segundos
        for (String cliente : clientesConectados.keySet()) {
            try {
                Socket socket = clientesConectados.get(cliente);
                if (socket != null && !socket.isClosed()) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("Servidor apagándose en 3 segundos...");
                }
            } catch (IOException e) {
                logger.severe("Error al notificar a los clientes: " + e.getMessage());
            }
        }

        //Esperar 3 segundos antes de cerrar conexiones
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.severe("Error en la cuenta regresiva: " + e.getMessage());
        }

        //Expulsar a todos los clientes
        for (Socket socket : clientesConectados.values()) {
            try {
                if (socket != null && !socket.isClosed()) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("El servidor se ha cerrado.");
                    socket.close();
                }
            } catch (IOException e) {
                logger.severe("Error al expulsar cliente: " + e.getMessage());
            }
        }

        clientesConectados.clear();
        SwingUtilities.invokeLater(() -> modeloListaClientes.clear());

        //Apagar el servidor y cerrar recursos
        try {
            if (servidor != null && !servidor.isClosed()) {
                servidor.close();
            }
            if (hilosClientes != null) {
                hilosClientes.shutdown();
            }
        } catch (IOException e) {
            logger.severe("Error al cerrar el servidor: " + e.getMessage());
        }

        // Cerrar la ventana después de detener el servidor
        SwingUtilities.invokeLater(() -> ventana.dispose());
    }

    public static void main(String[] args) {
        Servidor servidor = new Servidor();
        servidor.iniciarServidor();
    }
}