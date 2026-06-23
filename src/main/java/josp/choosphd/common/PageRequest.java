package josp.choosphd.common;

public record PageRequest(int pageNo, int pageSize, String sortBy, String sortDir) {
    public PageRequest {
        if (sortDir == null) sortDir = "desc";
        if (sortBy == null) sortBy = "score";
    }
    public static PageRequest of(int pageNo, int pageSize) {
        return new PageRequest(Math.max(1, pageNo), Math.min(100, Math.max(1, pageSize)), "score", "desc");
    }
}
