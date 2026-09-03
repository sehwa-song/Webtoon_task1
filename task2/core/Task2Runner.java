package task2.core;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import webtoon.io.ImageFiles;

public final class Task2Runner {
    private Task2Runner() {}

    public static void run(String pattern, ContentAlgorithm algorithm, String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("사용법: <입력.jpg> <출력 폴더>");
            return;
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        BufferedImage image = ImageFiles.read(input);
        List<ContentRegion> regions = algorithm.detectContent(image);
        for (int i = 0; i < regions.size(); i++) {
            ContentRegion region = regions.get(i);
            String name = ImageFiles.baseName(input) + "_content_" + String.format("%03d", i) + ".jpg";
            ImageFiles.savePart(image, region.top(), region.bottom(), output.resolve(name));
        }
        System.out.println(pattern + " 콘텐츠 구간 " + regions);
    }
}
