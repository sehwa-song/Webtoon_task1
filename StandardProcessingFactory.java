/** 매번 디스크에서 읽는 객체군을 만드는 Concrete Factory. */
final class StandardProcessingFactory implements ProcessingComponentFactory {
    @Override
    public ImageRepository createImageRepository() {
        return new DiskImageRepository();
    }

    @Override
    public CutPointStrategy createCutPointStrategy() {
        return MarginCutPointStrategy.getInstance();
    }

    @Override
    public String name() {
        return "ABSTRACT_FACTORY_NO_CACHE";
    }
}
