package task2.verification;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import task2.baseline.Task2Baseline;
import task2.variants.chain.ChainExtractorApp;
import task2.variants.decorator.DecoratorExtractorApp;
import task2.variants.factory.StandardProcessingFactory;
import task2.variants.factory.WebtoonExtractionService;
import task2.variants.proxy.ProxyExtractorApp;
import task2.variants.strategy.StrategyExtractorApp;
import task2.variants.template.TemplateExtractorApp;
import task2.variants.visitor.VisitorExtractorApp;

/**
 * 성능 측정 전에 모든 독립 구현이 같은 콘텐츠 범위를 반환하는지 확인한다.
 *
 * <p>이 클래스만 검증 목적으로 모든 패키지에 의존한다. baseline 및 각 variants
 * 폴더는 이 클래스를 빼고도 각각 독립 컴파일할 수 있다.</p>
 */
public final class Task2EquivalenceCheck {
    private Task2EquivalenceCheck() {
    }

    public static void main(String[] args) throws IOException {
        List<File> inputs = new ArrayList<File>();
        if (args.length == 0) {
            inputs.add(new File("images"));
        } else {
            for (String arg : args) {
                inputs.add(new File(arg));
            }
        }

        List<File> imageFiles = new ArrayList<File>();
        for (File input : inputs) {
            collectImageFiles(input, imageFiles);
        }
        Collections.sort(imageFiles, Comparator.comparing(File::getPath));
        if (imageFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "검증할 이미지가 없습니다: " + inputs);
        }

        ImageIO.setUseCache(false);
        WebtoonExtractionService factoryService =
                new WebtoonExtractionService(new StandardProcessingFactory());

        int checked = 0;
        for (File imageFile : imageFiles) {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                throw new IOException(
                        "지원하지 않거나 손상된 이미지: " + imageFile);
            }

            String baseline = baselineSignature(image);
            String strategy = strategySignature(image);
            String template = templateSignature(image);
            String chain = chainSignature(image);
            String decorator = decoratorSignature(image);
            String proxy = proxySignature(image);
            String visitor = visitorSignature(image);
            String factory = factorySignature(factoryService, image);

            requireSame(imageFile, "Strategy", baseline, strategy);
            requireSame(imageFile, "Template Method", baseline, template);
            requireSame(imageFile, "Chain of Responsibility", baseline, chain);
            requireSame(imageFile, "Decorator", baseline, decorator);
            requireSame(imageFile, "Proxy", baseline, proxy);
            requireSame(imageFile, "Visitor+Composite", baseline, visitor);
            requireSame(imageFile, "Abstract Factory", baseline, factory);
            checked++;
        }

        System.out.println(
                "PASS: " + checked
                        + "개 이미지에서 Baseline과 7개 디자인 패턴 구현의"
                        + " 콘텐츠 범위가 모두 같습니다.");
    }

    private static String baselineSignature(BufferedImage image) {
        List<task2.baseline.ContentRegion> regions =
                Task2Baseline.findContentRegions(image);
        validateBaseline(regions, image.getHeight());
        return regions.toString();
    }

    private static String strategySignature(BufferedImage image) {
        List<task2.variants.strategy.ContentRegion> regions =
                StrategyExtractorApp.findContentRegions(image);
        validateStrategy(regions, image.getHeight());
        return regions.toString();
    }

    private static String templateSignature(BufferedImage image) {
        List<task2.variants.template.ContentRegion> regions =
                TemplateExtractorApp.findContentRegions(image);
        int previousBottom = 0;
        for (task2.variants.template.ContentRegion region : regions) {
            previousBottom = validateRange(
                    region.top(), region.bottomExclusive(),
                    previousBottom, image.getHeight());
        }
        return regions.toString();
    }

    private static String chainSignature(BufferedImage image) {
        List<task2.variants.chain.ContentRegion> regions =
                ChainExtractorApp.findContentRegions(image);
        int previousBottom = 0;
        for (task2.variants.chain.ContentRegion region : regions) {
            previousBottom = validateRange(
                    region.top(), region.bottomExclusive(),
                    previousBottom, image.getHeight());
        }
        return regions.toString();
    }

    private static String decoratorSignature(BufferedImage image) {
        List<task2.variants.decorator.ContentRegion> regions =
                DecoratorExtractorApp.findContentRegions(image);
        int previousBottom = 0;
        for (task2.variants.decorator.ContentRegion region : regions) {
            previousBottom = validateRange(
                    region.top(), region.bottomExclusive(),
                    previousBottom, image.getHeight());
        }
        return regions.toString();
    }

    private static String proxySignature(BufferedImage image) {
        List<task2.variants.proxy.ContentRegion> regions =
                ProxyExtractorApp.findContentRegions(image);
        int previousBottom = 0;
        for (task2.variants.proxy.ContentRegion region : regions) {
            previousBottom = validateRange(
                    region.top(), region.bottomExclusive(),
                    previousBottom, image.getHeight());
        }
        return regions.toString();
    }

    private static String visitorSignature(BufferedImage image) {
        List<task2.variants.visitor.ContentRegion> regions =
                VisitorExtractorApp.findContentRegions(image);
        validateVisitor(regions, image.getHeight());
        return regions.toString();
    }

    private static String factorySignature(
            WebtoonExtractionService service, BufferedImage image) {
        List<task2.variants.factory.ContentRegion> regions =
                service.detect(image);
        validateFactory(regions, image.getHeight());
        return regions.toString();
    }

    private static void validateBaseline(
            List<task2.baseline.ContentRegion> regions, int imageHeight) {
        int previousBottom = 0;
        for (task2.baseline.ContentRegion region : regions) {
            previousBottom = validateRange(
                    region.top(), region.bottomExclusive(),
                    previousBottom, imageHeight);
        }
    }

    private static void validateStrategy(
            List<task2.variants.strategy.ContentRegion> regions,
            int imageHeight) {
        int previousBottom = 0;
        for (task2.variants.strategy.ContentRegion region : regions) {
            previousBottom = validateRange(
                    region.top(), region.bottomExclusive(),
                    previousBottom, imageHeight);
        }
    }

    private static void validateVisitor(
            List<task2.variants.visitor.ContentRegion> regions,
            int imageHeight) {
        int previousBottom = 0;
        for (task2.variants.visitor.ContentRegion region : regions) {
            previousBottom = validateRange(
                    region.top(), region.bottomExclusive(),
                    previousBottom, imageHeight);
        }
    }

    private static void validateFactory(
            List<task2.variants.factory.ContentRegion> regions,
            int imageHeight) {
        int previousBottom = 0;
        for (task2.variants.factory.ContentRegion region : regions) {
            previousBottom = validateRange(
                    region.top(), region.bottomExclusive(),
                    previousBottom, imageHeight);
        }
    }

    private static int validateRange(
            int top, int bottomExclusive, int previousBottom, int imageHeight) {
        if (top < previousBottom
                || bottomExclusive <= top
                || bottomExclusive > imageHeight) {
            throw new AssertionError(
                    "잘못되거나 겹친 콘텐츠 범위: ["
                            + top + "," + bottomExclusive + ")"
                            + ", 이전 끝=" + previousBottom
                            + ", 이미지 높이=" + imageHeight);
        }
        return bottomExclusive;
    }

    private static void requireSame(
            File imageFile,
            String implementation,
            String baseline,
            String actual) {
        if (!baseline.equals(actual)) {
            throw new AssertionError(
                    imageFile + "의 " + implementation + " 결과가 다릅니다."
                            + System.lineSeparator()
                            + "Baseline: " + baseline
                            + System.lineSeparator()
                            + implementation + ": " + actual);
        }
    }

    private static void collectImageFiles(File input, List<File> result) {
        if (input.isFile()) {
            if (isSupportedImage(input)) {
                result.add(input);
            }
            return;
        }
        File[] children = input.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            collectImageFiles(child, result);
        }
    }

    private static boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }
}
