import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** 요청할 때마다 파일에서 이미지를 읽는 비교 기준 구현. */
final class DiskImageRepository implements ImageRepository {
    @Override
    public BufferedImage load(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("지원하지 않는 이미지입니다: " + path);
        }
        return image;
    }
}
