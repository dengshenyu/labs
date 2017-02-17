package util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by shenyuan on 17/2/16.
 */
public class JsonUtils {

    private static ObjectMapper mapper = new ObjectMapper();

    public static String toJson(Object object) {
        try {
            String json = mapper.writeValueAsString(object);
            return json;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }
}
