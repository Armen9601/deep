import java.lang.reflect.*;
import java.util.*;
import sun.misc.Unsafe;

public class CopyUtils {

    private static final Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance", e);
        }
    }

    public static <T> T deepCopy(T original) {
        return deepCopy(original, new IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static <T> T deepCopy(T original, Map<Object, Object> visited) {
        if (original == null) {
            return null;
        }

        if (visited.containsKey(original)) {
            return (T) visited.get(original);
        }

        Class<?> clazz = original.getClass();

        if (clazz.isPrimitive() || clazz == String.class || Number.class.isAssignableFrom(clazz) || clazz == Boolean.class || clazz == Character.class) {
            return original;
        }

        if (clazz.isArray()) {
            int length = Array.getLength(original);
            Object copy = Array.newInstance(clazz.getComponentType(), length);
            visited.put(original, copy);
            for (int i = 0; i < length; i++) {
                Array.set(copy, i, deepCopy(Array.get(original, i), visited));
            }
            return (T) copy;
        }

        if (original instanceof Collection<?> originalCollection) {
            Collection<Object> copyCollection = original instanceof List ? new ArrayList<>() : new HashSet<>();
            visited.put(original, copyCollection);
            for (Object item : originalCollection) {
                copyCollection.add(deepCopy(item, visited));
            }
            return (T) copyCollection;
        }

        if (original instanceof Map<?, ?> originalMap) {
            Map<Object, Object> copyMap = new HashMap<>();
            visited.put(original, copyMap);
            for (Map.Entry<?, ?> entry : originalMap.entrySet()) {
                copyMap.put(deepCopy(entry.getKey(), visited), deepCopy(entry.getValue(), visited));
            }
            return (T) copyMap;
        }

        try {
            T copy = (T) unsafe.allocateInstance(clazz);
            visited.put(original, copy);
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object fieldValue = field.get(original);
                field.set(copy, deepCopy(fieldValue, visited));
            }
            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy object", e);
        }
    }

    public static void main(String[] args) {
        List<String> books = new ArrayList<>(Arrays.asList("Book1", "Book2"));
        Man original = new Man("John", 30, books);
        Man copy = CopyUtils.deepCopy(original);

        System.out.println("Original: " + original.getName() + ", " + original.getAge() + ", " + original.getFavoriteBooks());
        System.out.println("Copy: " + copy.getName() + ", " + copy.getAge() + ", " + copy.getFavoriteBooks());

        original.setName("Doe");
        original.getFavoriteBooks().add("Book3");

        System.out.println("Modified Original: " + original.getName() + ", " + original.getAge() + ", " + original.getFavoriteBooks());
        System.out.println("Unmodified Copy: " + copy.getName() + ", " + copy.getAge() + ", " + copy.getFavoriteBooks());
    }
}