package task1.factory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Abstract Factory가 생성하는 이미지 로딩 제품입니다. */
public interface ImageRepository {
    BufferedImage read(File file) throws IOException;
    String name();
}
