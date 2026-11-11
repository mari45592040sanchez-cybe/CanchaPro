package canchapro.util;

import canchapro.util.excepciones.CanchaProException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// POO - Responsabilidad / Encapsulamiento: concentra cómo se protegen las contraseñas (hash + salt).
public final class SeguridadPassword {

    private static final int ITERACIONES = 10_000;
    private static final String CARACTERES_TEMPORAL = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SeguridadPassword() {
    }

    public static String generarSalt() {
        byte[] salt = new byte[16];
        ALEATORIO.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resultado = (salt + password).getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < ITERACIONES; i++) {
                resultado = digest.digest(resultado);
            }
            return Base64.getEncoder().encodeToString(resultado);
        } catch (NoSuchAlgorithmException e) {
            throw new CanchaProException("SHA-256 no está disponible en esta JVM.", e);
        }
    }

    public static boolean verificar(String password, String salt, String hashEsperado) {
        byte[] calculado = hash(password, salt).getBytes(StandardCharsets.UTF_8);
        byte[] esperado = hashEsperado.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(calculado, esperado);
    }

    public static String generarTemporal(int largo) {
        StringBuilder sb = new StringBuilder(largo);
        for (int i = 0; i < largo; i++) {
            sb.append(CARACTERES_TEMPORAL.charAt(ALEATORIO.nextInt(CARACTERES_TEMPORAL.length())));
        }
        return sb.toString();
    }
}
