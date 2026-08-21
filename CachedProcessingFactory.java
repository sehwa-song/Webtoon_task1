/** Singleton 이미지 캐시를 사용하는 객체군을 만드는 Concrete Factory. */
final class CachedProcessingFactory implements ProcessingComponentFactory {
    @Override
    public ImageRepository createImageRepository() {
        return SingletonImageRepository.getInstance();
    }

    @Override
    public CutPointStrategy createCutPointStrategy() {
        return MarginCutPointStrategy.getInstance();
    }

    @Override
    public String name() {
        return "ABSTRACT_FACTORY_SINGLETON_CACHE";
    }
}
