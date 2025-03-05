import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encriptador {

    public static String encriptar(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] resumen = md.digest(texto.getBytes());
            return convertirAHexadecimal(resumen);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Convierte los bytes del hash a formato hexadecimal
    private static String convertirAHexadecimal(byte[] resumen) {
        StringBuilder hex = new StringBuilder();
        for (byte b : resumen) {
            String h = Integer.toHexString(0xFF & b);
            if (h.length() == 1) hex.append("0"); // Asegurar dos caracteres por byte
            hex.append(h);
        }
        return hex.toString().toUpperCase();
    }
}