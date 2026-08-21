import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** 이미지 로딩 정책을 추상화한다. */
public interface ImageRepository {
    BufferedImage load(Path path) throws IOException;
}
