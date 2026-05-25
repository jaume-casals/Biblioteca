package domini;

import java.util.Map;

/** Common interface for value objects that can be serialized to a Map for JSON / display. */
public interface Mappable {
    Map<String, Object> toMap();
}
