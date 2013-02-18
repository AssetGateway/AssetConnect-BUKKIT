package asset.connect.bukkit.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


public class ReflectionUtils {

	public static boolean setFinalField(Class<?> objectClass, Object object, String fieldName, Object value) {
		try {
			Field field = objectClass.getDeclaredField(fieldName);
			field.setAccessible(true);

			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

			field.set(object, value);
			return true;
		} catch (Exception exception) {
			exception.printStackTrace();
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField(Object object, Class<T> fieldClass, String fieldName) {
		try {
			Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(object);
		} catch(Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}
	
}