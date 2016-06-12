package it.simonesalvo.hangman.common.utilis;

import com.google.gson.Gson;
import lombok.NonNull;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by Simone Salvo on 22/06/2015.
 * www.simonesalvo.it
 */

public class JsonUtils {

    public static String encodeJSON(@NonNull Object obj) throws IOException {
        try {
            return new Gson().toJson(obj);
        } catch (Exception e) {
            throw new IOException("Unable to build JSON string. " + e.getMessage());
        }
    }

    public static <T> T decodeJSON(@NonNull String json, @NonNull Class<T> destinationClass) {
        return new Gson().fromJson(json, destinationClass);
    }
    
    public static <T> T decodeJSON(@NonNull String json, @NonNull Type destinationType) {
        return new Gson().fromJson(json, destinationType);
    }
}
