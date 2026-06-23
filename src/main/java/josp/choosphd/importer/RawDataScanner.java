package josp.choosphd.importer;

import josp.choosphd.security.ChoosePhdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Component
public class RawDataScanner {

    private static final Logger log = LoggerFactory.getLogger(RawDataScanner.class);

    private final ChoosePhdProperties props;

    public RawDataScanner(ChoosePhdProperties props) {
        this.props = props;
    }

    public List<RawFile> scan() {
        String rawDir = props.getData().getRawDir();
        File root = new File(rawDir);
        if (!root.isDirectory()) {
            log.error("raw dir not found: {}", rawDir);
            return List.of();
        }

        List<RawFile> out = new ArrayList<>();
        File[] children = root.listFiles();
        if (children == null) return out;

        for (File dir : children) {
            if (!dir.isDirectory()) continue;
            String source = matchSource(dir.getName());
            if (source == null) continue;

            try (Stream<Path> walk = Files.walk(dir.toPath())) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                        .forEach(p -> {
                            String fn = p.getFileName().toString();
                            Integer year = extractYear(fn);
                            String trendDir = extractTrendDir(p, dir);
                            if (year != null) {
                                out.add(new RawFile(p.toString(), fn, source, year, trendDir));
                            }
                        });
            } catch (IOException e) {
                log.error("scan failed: {}", dir, e);
            }
        }

        log.info("scanned {} raw files from {}", out.size(), rawDir);
        return out;
    }

    private String matchSource(String dirName) {
        String n = dirName.toLowerCase(Locale.ROOT);
        if (n.contains("qs")) return "QS";
        if (n.contains("the ") || n.contains("the排名") || n.equals("the") || n.contains("times higher")) return "THE";
        if (n.contains("软科") || n.contains("arwu") || n.contains("shanghai")) return "ARWU";
        if (n.contains("us news") || n.contains("usnews") || n.contains("us_news")) return "USNEWS";
        if (n.contains("grur") || n.contains("employability") || n.contains("雇佣")) return "GRUR";
        if (n.contains("edurank") || n.contains("edu rank")) return "EDURANK";
        if (n.contains("mosiur") || n.contains("mosi")) return "MOSIUR";
        if (n.contains("rur")) return "RUR";
        if (n.contains("cwur")) return "CWUR";
        if (n.contains("mengy") || n.contains("mengy 排名") || n.contains("mengy排名")) return "MENGY";
        return null;
    }

    private Integer extractYear(String name) {
        int idx = name.indexOf('-');
        if (idx > 0) {
            String head = name.substring(0, idx);
            if (head.length() == 4 && head.chars().allMatch(Character::isDigit)) {
                int y = Integer.parseInt(head);
                if (y > 1990 && y < 2100) return y;
            }
        }
        if (name.length() >= 4 && name.substring(0, 4).chars().allMatch(Character::isDigit)) {
            int y = Integer.parseInt(name.substring(0, 4));
            if (y > 1990 && y < 2100) return y;
        }
        return null;
    }

    private String extractTrendDir(Path file, File sourceDir) {
        // 优先: 文件名
        String fn = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fn.contains("growing-trend") || fn.contains("growing_trend") || fn.contains("growing trend")) {
            return "UP";
        }
        if (fn.contains("declining-trend") || fn.contains("declining_trend") || fn.contains("declining trend")) {
            return "DOWN";
        }
        // 父目录(顶级目录名)
        Path parent = file.getParent();
        if (parent != null) {
            String pn = parent.getFileName().toString().toLowerCase(Locale.ROOT);
            if (pn.contains("growing") || pn.contains("growth")) return "UP";
            if (pn.contains("declining")) return "DOWN";
        }
        return null;
    }

    public record RawFile(String path, String fileName, String source, int year, String trendDirection) {}
}
