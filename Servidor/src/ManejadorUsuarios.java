import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ManejadorUsuarios {
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";

    //Método para registrar un usuario
    public static boolean registrarUsuario(String nombreUsuario, String contrasena) {
        try {
            String contrasenaEncriptada = Encriptador.encriptar(contrasena);

            List<String> usuariosExistentes = leerUsuarios();

            for (String linea : usuariosExistentes) {
                String[] partes = linea.split("\\|");
                if (partes.length == 2) {
                    String usuarioGuardado = partes[0];
                    if (usuarioGuardado.equals(nombreUsuario)) {
                        String contrasenaGuardada = partes[1];
                        if (!contrasenaGuardada.equals(contrasenaEncriptada)) {
                            return false; // Usuario ya existe con otra contraseña
                        } else {
                            return true; // Usuario ya registrado correctamente
                        }
                    }
                }
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true));
            writer.write(nombreUsuario + "|" + contrasenaEncriptada);
            writer.newLine();
            writer.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Verifica si un usuario y contraseña existen
    public static boolean verificarUsuario(String nombreUsuario, String contrasena) {
        try {
            List<String> usuariosExistentes = leerUsuarios();
            String contrasenaEncriptada = Encriptador.encriptar(contrasena);

            for (String linea : usuariosExistentes) {
                String[] partes = linea.split("\\|");
                if (partes.length == 2) {
                    String usuarioGuardado = partes[0];
                    String contrasenaGuardada = partes[1];

                    if (usuarioGuardado.equals(nombreUsuario) && contrasenaGuardada.equals(contrasenaEncriptada)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //Lee los usuarios registrados en el archivo
    private static List<String> leerUsuarios() {
        List<String> usuarios = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                usuarios.add(linea);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return usuarios;
    }
}