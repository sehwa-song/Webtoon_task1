import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        File dir = new File("images");

        for(File f : dir.listFiles()) {
            for (File img : f.listFiles()){
                splitImage(img);
            }
        }
    }


    static void splitImage(File img){
        try {
        BufferedImage image = ImageIO.read(img); // 예외처리를 해야만 컴파일 가능.
            
        int width = image.getWidth();
        int height = image.getHeight();

        List<Integer> cutYs = new ArrayList<>();

        boolean prevBlank = false;
        int start = 0;

        cutYs.add(0);

        for(int y=0; y<height;y++){
            boolean isBlank = false;


            for (int x =0; x<width; x++){ // 한줄이 전부 흰색인가? 안쪽 x까지 반복해야 알 수 있음.
                int rgb = image.getRGB(x,y); // rgb를 각각 분해해서 비교해봐야 앎.

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int threshold = 240;
                if (r >= threshold && g >= threshold && b >= threshold) { //분해된 세개의 값이 통과해야 함.
                    isBlank = true;
                } else {
                    isBlank = false;
                    break;
                }

            }

            if (isBlank && !prevBlank) { // 여백 아니었다가 여백 시작
                start = y;
            }

            if (!isBlank && prevBlank) { // 여백이었다가 여백 끝
                int thickness = y - start;
                if(thickness >= 30 && start > 0){
                    System.out.println("여백구간: " + start + " ~ " + (y-1));
                    int cutY = start + (thickness / 2);
                    cutYs.add(cutY);
                }
            }
    
            prevBlank = isBlank; // 이번 줄 상태를 "직전"으로 바꾸기
            
        }

        cutYs.add(height);


        String episode = img.getParentFile().getName(); // 에피소드 번호
        String name = img.getName().replace(".jpg", ""); // 확장자 뗀 원본 이름

        new File("output/"+episode).mkdirs();

        for (int i =0 ; i <cutYs.size()-1 ; i++) {
            int top = cutYs.get(i);
            int end = cutYs.get(i + 1);
            int cutHeight = end - top; 

            BufferedImage piece = image.getSubimage(0, top, width, cutHeight);
            ImageIO.write(piece, "jpg", new File("output/" + episode + "/" + name + "_cut_" +i + ".jpg"));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
                       
    }
}

