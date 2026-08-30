package task1.factory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/** Abstract Factory가 생성하는 결과 저장 제품입니다. */
public interface PieceWriter {
    List<File> write(
            File sourceFile,
            BufferedImage image,
            List<Integer> cutPoints,
            File outputRoot) throws IOException;

    String name();
}
