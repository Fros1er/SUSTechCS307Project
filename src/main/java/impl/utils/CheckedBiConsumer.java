package impl.utils;

import java.sql.SQLException;

@FunctionalInterface
public interface CheckedBiConsumer<T, U> {
    void accept(T t, U u) throws SQLException;
}
