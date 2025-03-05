import java.io.*;
import java.net.Socket;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ManejadorCliente implements Runnable {
    private Socket socket;
    private Servidor servidor;
    private PrintWriter out;
    private BufferedReader in;
    private String nombreUsuario;

    private static final Logger logger = Logger.getLogger(ManejadorCliente.class.getName());

    static {
        try {
            FileHandler fileHandler = new FileHandler("usuarios.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("No se pudo configurar el logger de usuarios: " + e.getMessage());
        }
    }

    public ManejadorCliente(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Ingrese su nombre de usuario:");
            nombreUsuario = in.readLine();

            out.println("Ingrese su contraseña:");
            String contrasena = in.readLine();

            if (nombreUsuario == null || contrasena == null || nombreUsuario.trim().isEmpty() || contrasena.trim().isEmpty()) {
                out.println("Error: Nombre de usuario o contraseña inválidos.");
                cerrarConexion();
                return;
            }

            // Encriptar la contraseña
            String contrasenaEncriptada = Encriptador.encriptar(contrasena);

            // Verificar si el usuario ya existe
            boolean usuarioNuevo = ManejadorUsuarios.registrarUsuario(nombreUsuario, contrasena);

            if (!usuarioNuevo) {
                out.println("Error: El usuario ya existe con una contraseña diferente. Elige otro nombre.");
                cerrarConexion();
                return;
            }

            // Si es un nuevo usuario, lo registramos en el log
            logger.info("Usuario registrado: " + nombreUsuario + " | Contraseña: " + contrasenaEncriptada);

            // Verificar credenciales
            boolean loginExitoso = ManejadorUsuarios.verificarUsuario(nombreUsuario, contrasena);

            if (loginExitoso) {
                servidor.agregarCliente(nombreUsuario, socket.getInetAddress().toString(), socket);
                out.println("¡Inicio de sesión exitoso!");

                while (!socket.isClosed()) {
                    String mensaje = in.readLine();
                    if (mensaje == null || mensaje.equalsIgnoreCase("SALIR")) {
                        break;
                    }
                }
            } else {
                out.println("Error: Nombre de usuario o contraseña incorrectos.");
                cerrarConexion();
            }

        } catch (IOException e) {
            System.err.println("Error con el cliente " + nombreUsuario + ": " + e.getMessage());
        } finally {
            cerrarConexion();
        }
    }

    //Método para cerrar la conexión y eliminar el cliente de la GUI
    private void cerrarConexion() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión del cliente " + nombreUsuario + ": " + e.getMessage());
        } finally {
            servidor.expulsarCliente(nombreUsuario);
        }
    }
}