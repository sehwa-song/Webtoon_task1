package task1.proxy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Real Subject와 Proxy가 함께 구현하는 이미지 읽기 인터페이스입니다. */
public interface ImageRepository {
    BufferedImage read(File imageFile) throws IOException;
}
