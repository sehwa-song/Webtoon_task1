package task1.proxy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/** 디스크에서 실제로 이미지를 읽는 원본 대상 객체(Real Subject)입니다. */
public final class DiskImageRepository implements ImageRepository {
    @Override
    public BufferedImage read(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("지원하지 않거나 손상된 이미지입니다: " + imageFile);
        }
        return image;
    }
}
