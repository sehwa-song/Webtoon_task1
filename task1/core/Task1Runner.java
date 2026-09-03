package task1.core;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import webtoon.io.ImageFiles;

public final class Task1Runner {
    private Task1Runner() {}

    public static void run(String pattern, CutAlgorithm algorithm, String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("사용법: <입력.jpg> <출력 폴더>");
            return;
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        BufferedImage image = ImageFiles.read(input);
        List<Integer> cuts = algorithm.detectCuts(image);
        for (int i = 0; i < cuts.size() - 1; i++) {
            String name = ImageFiles.baseName(input) + "_cut_" + String.format("%03d", i) + ".jpg";
            ImageFiles.savePart(image, cuts.get(i), cuts.get(i + 1), output.resolve(name));
        }
        System.out.println(pattern + " 절단점 " + cuts);
    }
}
