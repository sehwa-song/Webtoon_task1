/** 이미지 저장소와 컷 알고리즘이라는 관련 객체군을 만드는 Abstract Factory. */
public interface ProcessingComponentFactory {
    ImageRepository createImageRepository();

    CutPointStrategy createCutPointStrategy();

    String name();
}
