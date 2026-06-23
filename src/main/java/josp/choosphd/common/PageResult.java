package josp.choosphd.common;

import java.util.List;

public record PageResult<T>(List<T> list, long total, long pageNo, long pageSize) {
    public static <T> PageResult<T> of(List<T> list, long total, long pageNo, long pageSize) {
        return new PageResult<>(list, total, pageNo, pageSize);
    }
}
