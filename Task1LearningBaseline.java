import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;


public class Task1LearningBaseline {
    // 한 행의 왼쪽/오른쪽 끝에서 각각 5%를 조사해 예상 배경색을 구한다.
    private static final double EDGE_SAMPLE_RATIO = 0.05;

    // 두 RGB 색 사이의 거리가 18 이하면 JPEG 노이즈가 섞인 같은 색으로 본다.
    private static final double BACKGROUND_COLOR_DISTANCE = 18.0;

    // 좌우 끝의 대표색이 너무 다르면 그 행은 배경이 아니라 그림일 가능성이 크다.
    private static final double MAX_EDGE_COLOR_DISTANCE = 18.0;

    // 좌우 끝부분 자체의 색 변화가 크면 그림이나 글자가 걸친 것으로 판단한다.
    private static final double MAX_EDGE_STANDARD_DEVIATION = 8.0;

    // R/G/B 최댓값과 최솟값의 차이가 18 이하면 흰색, 검정, 회색 계열로 본다.
    // 이보다 차이가 크면 핑크, 파랑, 빨강 같은 유채색으로 분류한다.
    private static final int MAX_NEUTRAL_CHANNEL_RANGE = 18;

    // 흰색, 검정, 회색 여백은 JPEG 노이즈를 고려해 전경 픽셀을 2%까지 허용한다.
    private static final double NEUTRAL_FOREGROUND_RATIO = 0.02;

    // 핑크 같은 유채색은 노을이나 하늘의 오인식을 막기 위해 훨씬 엄격하게 본다.
    // 720px 이미지에서 0.5%는 약 3픽셀이다.
    private static final double COLORED_FOREGROUND_RATIO = 0.005;

    // 일반 무채색 여백은 50px 이상 연속될 때 컷 사이 여백으로 인정한다.
    private static final int MIN_NEUTRAL_BLANK_HEIGHT = 50;

    // 유채색은 그림일 가능성이 더 높으므로 100px 이상이어야 여백으로 인정한다.
    private static final int MIN_COLORED_BLANK_HEIGHT = 100;

    // 유채색 여백의 시작부터 끝까지 대표색 거리가 10 이하여야 한다.
    // 일정한 핑크 배경은 통과하지만 계속 색이 바뀌는 노을은 최대한 제외한다.
    private static final double MAX_COLORED_BACKGROUND_DRIFT = 10.0;

    // classifyBlankRow()가 반환하는 행 종류를 숫자로 구분한다.
    private static final int CONTENT_ROW = 0;
    private static final int NEUTRAL_BLANK_ROW = 1;
    private static final int COLORED_BLANK_ROW = 2;

    public static void main(String[] args) throws IOException{
            File imagesDirectory = new File("images");

            File[] episodeDirectories = imagesDirectory.listFiles();

            if(episodeDirectories == null) {
                System.out.println("images 폴더를 읽을 수 없습니다.");
                return;
            }

            Arrays.sort(episodeDirectories);

            for (File episodeDirectory : episodeDirectories) {
                if (!episodeDirectory.isDirectory()) {
                    continue;
                }

                File[] imageFiles = episodeDirectory.listFiles();

                if(imageFiles == null){
                    continue;
                }

                Arrays.sort(imageFiles);

                for (File imageFile : imageFiles){
                    if(!imageFile.isFile()){
                        continue;
                    }

                    String name = imageFile.getName().toLowerCase();

                    boolean isImage = name.endsWith(".jpg");

                    if (isImage){
                        System.out.println("처리시작: " + imageFile.getPath());
                        splitImage(imageFile);
                    }
                }
            }
        }
        
        static void splitImage(File imageFile) throws IOException {
            BufferedImage image = ImageIO.read(imageFile);

            if (image == null) {
                System.out.println("읽을 수 없어" + imageFile.getPath());
                return;
            }

            String originalName = imageFile.getName();
            int dotPosition = originalName.lastIndexOf('.');

            String baseName;

            if (dotPosition > 0) {
                baseName = originalName.substring(0, dotPosition);
            } else {
                baseName = originalName;
            }

            // 현재 연속 여백이 시작된 y 좌표이다. -1이면 여백을 찾는 중이 아니다.
            int blankStart = -1;

            List<Integer> cutPoints = new ArrayList<>(); //  자를 위치 저장할 리스트
            cutPoints.add(0); // 저장할 범위라서 0(처음)과 getHeight(끝)은 추가.


            for (int y = 0; y < image.getHeight(); y++) {
                // 현재 행이 그림인지, 무채색 여백인지, 유채색 여백인지 판정한다.
                int rowType = classifyBlankRow(image, y);
                boolean blankRow = rowType != CONTENT_ROW;

                if (blankRow && blankStart == -1) {
                    // 그림 행을 보다가 처음 여백 행을 만난 위치를 저장한다.
                    blankStart = y;
                }

                if (!blankRow && blankStart != -1) {
                    // 유채색에서 무채색으로 바뀌더라도 중간에 그림 행이 없었다면
                    // 같은 여백으로 계속 묶는다. 실제 그림 행을 만났을 때만 종료한다.
                    addCutPointForBlankZone(
                            image,
                            cutPoints,
                            blankStart,
                            y - 1);

                    blankStart = -1;
                }
            }

            // 이미지의 마지막 행까지 여백이 이어진 경우도 구간을 닫아 준다.
            // addCutPointForBlankZone()에서 맨 아래 여백은 컷 경계가 아니므로 제외한다.
            if (blankStart != -1) {
                addCutPointForBlankZone(
                        image,
                        cutPoints,
                        blankStart,
                        image.getHeight() - 1);
            }

            cutPoints.add(image.getHeight());

            // 에피소드별 출력 폴더 만들기
            String episodeName = imageFile.getParentFile().getName();

            File outputRoot = new File("output");
            File outputDirectory = new File(outputRoot, episodeName);

            if(!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            for(int i = 0; i <cutPoints.size() - 1; i++){
                int top = cutPoints.get(i);
                int bottom = cutPoints.get(i+1);
                int pieceHeight = bottom - top;
                BufferedImage piece = image.getSubimage(
                    0,
                    top,
                    image.getWidth(),
                    pieceHeight
                );
                
                String cutNumber = String.format("%03d", i);

                File outputFile = new File(outputDirectory, baseName + "_cut_" + cutNumber + ".jpg");

                ImageIO.write(piece, "jpg", outputFile); // 파일에 기록하는 역할

                System.out.println("저장 완료:" + outputFile.getPath());
            }

            // 이전 실행보다 컷 개수가 줄면 과거의 큰 번호 파일이 남을 수 있다.
            // 이번에 만든 조각 수보다 번호가 큰 같은 원본의 파일만 정리한다.
            removeStaleOutputPieces(
                    outputDirectory,
                    baseName,
                    cutPoints.size() - 1);

        }

        static void removeStaleOutputPieces(
                File outputDirectory,
                String baseName,
                int currentPieceCount) {

            File[] outputFiles = outputDirectory.listFiles();

            if (outputFiles == null) {
                return;
            }

            String prefix = baseName + "_cut_";
            String suffix = ".jpg";

            for (File outputFile : outputFiles) {
                String fileName = outputFile.getName();

                // 현재 원본에서 생성한 조각 파일이 아니면 건드리지 않는다.
                if (!fileName.startsWith(prefix)
                        || !fileName.endsWith(suffix)) {
                    continue;
                }

                // 예: 001_cut_015.jpg에서 "015" 부분만 꺼낸다.
                String numberText = fileName.substring(
                        prefix.length(),
                        fileName.length() - suffix.length());

                try {
                    int pieceNumber = Integer.parseInt(numberText);

                    // 현재 조각이 0~14라면 15 이상의 예전 파일만 삭제한다.
                    if (pieceNumber >= currentPieceCount
                            && !outputFile.delete()) {
                        System.out.println(
                                "오래된 출력 파일 삭제 실패: "
                                + outputFile.getPath());
                    }
                } catch (NumberFormatException exception) {
                    // 번호 형식이 아닌 파일은 사용자가 만든 파일일 수 있으므로 무시한다.
                }
            }
        }
                      
        // 현재 행을 그림, 무채색 여백, 유채색 여백 중 하나로 분류한다.
        static int classifyBlankRow(BufferedImage image, int y) {
            int width = image.getWidth();

            int neutralMaximumForeground =
                    (int) (width * NEUTRAL_FOREGROUND_RATIO);
            int coloredMaximumForeground =
                    (int) (width * COLORED_FOREGROUND_RATIO);

            // 기존 흰색/검정 방식은 그대로 유지한다.
            // 거의 흰색 또는 거의 검정이면 무채색 여백 행이다.
            int fixedColorForeground =
                    countWhiteOrBlackForegroundPixels(image, y);

            if (fixedColorForeground <= neutralMaximumForeground) {
                return NEUTRAL_BLANK_ROW;
            }

            // 흰색이나 검정이 아니라면 좌우 끝에서 이 행의 예상 배경색을 찾는다.
            Color expectedBackground =
                    findExpectedEdgeBackgroundColor(image, y);

            // 좌우 끝 색이 다르거나 글자/그림이 걸쳐 있으면 배경색을 정할 수 없다.
            if (expectedBackground == null) {
                return CONTENT_ROW;
            }

            int edgeForeground = countPixelsDifferentFromBackground(
                    image, y, expectedBackground);

            if (isNeutralColor(expectedBackground)) {
                // 회색 그라데이션은 행마다 대표색이 달라도 2% 기준을 사용한다.
                return edgeForeground <= neutralMaximumForeground
                        ? NEUTRAL_BLANK_ROW
                        : CONTENT_ROW;
            }

            // 핑크 같은 유채색은 0.5% 이하만 다른 경우에만 여백 후보로 둔다.
            // 실제 여백 확정은 높이와 구간 전체 색 안정성 검사까지 통과해야 한다.
            return edgeForeground <= coloredMaximumForeground
                    ? COLORED_BLANK_ROW
                    : CONTENT_ROW;
        }

        // 기존 흰색/검정 방식과 새 회색 그라데이션 방식을 모두 실행한다.
        // 둘 중 전경 픽셀이 더 적게 나온 값을 사용하면 기존 기능을 유지하면서
        // 회색 그라데이션 여백도 추가로 찾을 수 있다.
        static int countForegroundPixels(BufferedImage image, int y) {
            int fixedColorForeground =
                    countWhiteOrBlackForegroundPixels(image, y);
            int edgeColorForeground =
                    countEdgeBasedForegroundPixels(image, y);

            return Math.min(fixedColorForeground, edgeColorForeground);
        }

        // 기존 방식: 한 행이 거의 흰색이거나 거의 검정인지 확인한다.
        static int countWhiteOrBlackForegroundPixels(
                BufferedImage image,
                int y) {

            int width = image.getWidth();
            int whitePixelCount = 0;
            int blackPixelCount = 0;

            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));

                boolean isWhite = color.getRed() >= 245
                        && color.getGreen() >= 245
                        && color.getBlue() >= 245;

                boolean isBlack = color.getRed() <= 20
                        && color.getGreen() <= 20
                        && color.getBlue() <= 20;

                if (isWhite) {
                    whitePixelCount++;
                }

                if (isBlack) {
                    blackPixelCount++;
                }
            }

            // 흰색과 검정 중 더 많은 쪽을 이 행의 고정 배경색으로 본다.
            int backgroundPixelCount =
                    Math.max(whitePixelCount, blackPixelCount);

            return width - backgroundPixelCount;
        }

        // 추가 방식: 좌우 끝에서 예상 배경색을 찾은 후
        // 그 색과 다른 픽셀 수를 반환한다.
        static int countEdgeBasedForegroundPixels(BufferedImage image, int y) {
            Color expectedBackground =
                    findExpectedEdgeBackgroundColor(image, y);

            if (expectedBackground == null) {
                return image.getWidth();
            }

            return countPixelsDifferentFromBackground(
                    image, y, expectedBackground);
        }

        // 한 행의 좌우 끝을 조사해 예상 배경색을 반환한다.
        // 좌우 끝이 서로 다르거나 끝부분에 그림/글자가 섞이면 null을 반환한다.
        static Color findExpectedEdgeBackgroundColor(
                BufferedImage image,
                int y) {

            int width = image.getWidth();

            // 720px 이미지라면 좌우에서 각각 36px을 조사한다.
            // 아주 작은 이미지에서도 최소 1px은 조사한다.
            int edgeWidth = Math.max(
                    1,
                    (int) Math.round(width * EDGE_SAMPLE_RATIO));

            // 조사 영역이 이미지의 1/3보다 커지지 않도록 제한한다.
            edgeWidth = Math.min(edgeWidth, Math.max(1, width / 3));

            // 왼쪽 끝과 오른쪽 끝의 평균 RGB 색을 각각 구한다.
            Color leftBackground = averageColor(
                    image, y, 0, edgeWidth);
            Color rightBackground = averageColor(
                    image, y, width - edgeWidth, width);

            // 좌우 끝 색상이 서로 너무 다르면 한 가지 배경색이라고 보기 어렵다.
            if (colorDistance(leftBackground, rightBackground)
                    > MAX_EDGE_COLOR_DISTANCE) {
                return null;
            }

            // 좌우 끝에 그림, 말풍선, 글자가 걸려 색 변화가 크면
            // 안전한 여백 행으로 인정하지 않는다.
            double leftDeviation = colorStandardDeviation(
                    image, y, 0, edgeWidth, leftBackground);
            double rightDeviation = colorStandardDeviation(
                    image, y, width - edgeWidth, width, rightBackground);

            if (leftDeviation > MAX_EDGE_STANDARD_DEVIATION
                    || rightDeviation > MAX_EDGE_STANDARD_DEVIATION) {
                return null;
            }

            // 좌우 대표색의 평균을 이 행의 예상 배경색으로 사용한다.
            // 행마다 새로 계산하므로 세로 방향 그라데이션도 따라갈 수 있다.
            return new Color(
                    (leftBackground.getRed() + rightBackground.getRed()) / 2,
                    (leftBackground.getGreen() + rightBackground.getGreen()) / 2,
                    (leftBackground.getBlue() + rightBackground.getBlue()) / 2);
        }

        // 대표 배경색이 흰색/검정/회색 계열인지 확인한다.
        static boolean isNeutralColor(Color color) {
            int maximumChannel = Math.max(
                    color.getRed(),
                    Math.max(color.getGreen(), color.getBlue()));
            int minimumChannel = Math.min(
                    color.getRed(),
                    Math.min(color.getGreen(), color.getBlue()));

            return maximumChannel - minimumChannel
                    <= MAX_NEUTRAL_CHANNEL_RANGE;
        }

        // 한 행에서 예상 배경색과 RGB 거리가 18보다 먼 픽셀을 센다.
        // 이런 픽셀은 그림, 말풍선, 글자일 가능성이 있다.
        static int countPixelsDifferentFromBackground(
                BufferedImage image,
                int y,
                Color expectedBackground) {

            int width = image.getWidth();
            int foregroundPixelCount = 0;
            double maximumDistanceSquared =
                    BACKGROUND_COLOR_DISTANCE * BACKGROUND_COLOR_DISTANCE;

            for (int x = 0; x < width; x++) {
                Color current = new Color(image.getRGB(x, y));

                // 예상 배경색에서 멀리 떨어진 픽셀은
                // 그림, 말풍선 또는 글자에 속한 전경 픽셀로 센다.
                if (colorDistanceSquared(current, expectedBackground)
                        > maximumDistanceSquared) {
                    foregroundPixelCount++;
                }
            }

            return foregroundPixelCount;
        }

        // 한 행에서 startX 이상 endX 미만 범위의 평균 RGB 색을 구한다.
        static Color averageColor(
                BufferedImage image,
                int y,
                int startX,
                int endX) {

            long redSum = 0;
            long greenSum = 0;
            long blueSum = 0;

            for (int x = startX; x < endX; x++) {
                Color color = new Color(image.getRGB(x, y));
                redSum += color.getRed();
                greenSum += color.getGreen();
                blueSum += color.getBlue();
            }

            int pixelCount = endX - startX;

            return new Color(
                    (int) (redSum / pixelCount),
                    (int) (greenSum / pixelCount),
                    (int) (blueSum / pixelCount));
        }

        // 조사 범위의 픽셀들이 대표색 주변에 얼마나 모여 있는지 계산한다.
        // 값이 작으면 단색 배경에 가깝고, 크면 그림이나 글자가 섞였다는 뜻이다.
        static double colorStandardDeviation(
                BufferedImage image,
                int y,
                int startX,
                int endX,
                Color average) {

            long distanceSquaredSum = 0;

            for (int x = startX; x < endX; x++) {
                Color color = new Color(image.getRGB(x, y));
                distanceSquaredSum += colorDistanceSquared(color, average);
            }

            int pixelCount = endX - startX;
            return Math.sqrt(distanceSquaredSum / (double) pixelCount);
        }

        // 두 RGB 색의 실제 거리를 반환한다.
        static double colorDistance(Color first, Color second) {
            return Math.sqrt(colorDistanceSquared(first, second));
        }

        // 제곱근 계산을 반복하지 않기 위해 색 거리의 제곱도 별도 함수로 둔다.
        static int colorDistanceSquared(Color first, Color second) {
            int redDifference = first.getRed() - second.getRed();
            int greenDifference = first.getGreen() - second.getGreen();
            int blueDifference = first.getBlue() - second.getBlue();

            return redDifference * redDifference
                    + greenDifference * greenDifference
                    + blueDifference * blueDifference;
        }

        // 연속된 여백 구간이 실제 컷 경계 조건을 만족하면 절단 위치를 추가한다.
        static void addCutPointForBlankZone(
                BufferedImage image,
                List<Integer> cutPoints,
                int start,
                int end) {

            if (start < 0 || end < start) {
                return;
            }

            // 이미지 맨 위와 맨 아래의 배경은 두 그림 사이의 여백이 아니다.
            // 여기서 자르면 배경만 들어 있는 별도 파일이 생길 수 있으므로 제외한다.
            if (start == 0 || end == image.getHeight() - 1) {
                return;
            }

            boolean neutralPartQualified = false;
            boolean coloredPartQualified = false;

            int neutralRunLength = 0;
            int coloredRunStart = -1;

            // 하나의 연속 여백 안에 핑크와 밝은 회색이 함께 있을 수 있다.
            // 색 종류가 바뀌었다는 이유로 두 번 자르지 않고,
            // 각 종류 안에 기준을 만족하는 구간이 하나라도 있는지 확인한다.
            for (int y = start; y <= end; y++) {
                int rowType = classifyBlankRow(image, y);

                if (rowType == NEUTRAL_BLANK_ROW) {
                    neutralRunLength++;

                    if (neutralRunLength >= MIN_NEUTRAL_BLANK_HEIGHT) {
                        neutralPartQualified = true;
                    }

                    // 직전까지 이어진 유채색 구간이 끝났으므로 검사한다.
                    if (coloredRunStart != -1) {
                        int coloredRunEnd = y - 1;
                        int coloredHeight = coloredRunEnd - coloredRunStart + 1;

                        if (coloredHeight >= MIN_COLORED_BLANK_HEIGHT
                                && isStableColoredBackground(
                                        image, coloredRunStart, coloredRunEnd)) {
                            coloredPartQualified = true;
                        }

                        coloredRunStart = -1;
                    }
                } else if (rowType == COLORED_BLANK_ROW) {
                    neutralRunLength = 0;

                    if (coloredRunStart == -1) {
                        coloredRunStart = y;
                    }
                }
            }

            // 여백의 마지막 행까지 유채색이 이어졌다면 위 반복문 안에서
            // 종료 검사가 되지 않았으므로 여기서 한 번 더 검사한다.
            if (coloredRunStart != -1) {
                int coloredHeight = end - coloredRunStart + 1;

                if (coloredHeight >= MIN_COLORED_BLANK_HEIGHT
                        && isStableColoredBackground(
                                image, coloredRunStart, end)) {
                    coloredPartQualified = true;
                }
            }

            // 무채색도 유채색도 각자의 기준을 만족하지 못하면 자르지 않는다.
            if (!neutralPartQualified && !coloredPartQualified) {
                return;
            }

            // 정확한 중간을 무조건 자르지 않고,
            // 주변에 글자나 말풍선 픽셀이 가장 적은 안전한 행을 찾는다.
            int cutY = findSafestCutY(image, start, end);

            if (cutY != -1) {
                String typeName;

                if (neutralPartQualified && coloredPartQualified) {
                    typeName = "유채색+무채색 혼합 여백";
                } else if (coloredPartQualified) {
                    typeName = "유채색 여백";
                } else {
                    typeName = "무채색 여백";
                }

                System.out.println(
                        typeName + " " + start + "~" + end
                        + ", 안전한 절단 위치: " + cutY);
                cutPoints.add(cutY);
            }
        }

        // 유채색 여백 전체의 대표색이 일정하게 유지되는지 검사한다.
        // 핑크 단색 배경은 통과하지만, 세로로 색이 계속 변하는 노을은 제외하기 위한 조건이다.
        static boolean isStableColoredBackground(
                BufferedImage image,
                int start,
                int end) {

            int middle = start + (end - start) / 2;
            Color referenceColor =
                    findExpectedEdgeBackgroundColor(image, middle);

            if (referenceColor == null || isNeutralColor(referenceColor)) {
                return false;
            }

            for (int y = start; y <= end; y++) {
                Color currentColor =
                        findExpectedEdgeBackgroundColor(image, y);

                if (currentColor == null || isNeutralColor(currentColor)) {
                    return false;
                }

                // 구간 중 한 행이라도 기준색에서 10보다 멀어지면
                // 일정한 유채색 배경이 아니라 색이 변하는 그림으로 판단한다.
                if (colorDistance(currentColor, referenceColor)
                        > MAX_COLORED_BACKGROUND_DRIFT) {
                    return false;
                }
            }

            return true;
        }

        // 위아래 5줄까지 함께 확인한 뒤 전경 픽셀이 가장 적은 위치를 고른다.
        // 점수가 같으면 원래 여백의 중간과 가까운 위치를 선택한다.
        static int findSafestCutY(BufferedImage image, int start, int end) {
            int radius = 5;
            int middle = start + (end - start) / 2;

            int maximumForegroundPixels = Math.max(
                    1,
                    (int) (image.getWidth() * COLORED_FOREGROUND_RATIO));

            int bestY = -1;
            long bestScore = Long.MAX_VALUE;

            for (int y = start + radius; y <= end - radius; y++) {
                long score = 0;
                boolean safe = true;

                // 현재 행의 위아래 5줄까지 확인한다.
                for (int checkY = y - radius;
                        checkY <= y + radius;
                        checkY++) {

                    int foregroundPixels =
                            countForegroundPixels(image, checkY);

                    score += foregroundPixels;

                    if (foregroundPixels > maximumForegroundPixels) {
                        safe = false;
                        break;
                    }
                }

                if (!safe) {
                    continue;
                }

                boolean lowerScore = score < bestScore;

                boolean sameScoreButCloser =
                        score == bestScore
                        && Math.abs(y - middle)
                           < Math.abs(bestY - middle);

                if (lowerScore || sameScoreButCloser) {
                    bestY = y;
                    bestScore = score;
                }
            }

            return bestY;
        }

}
    
