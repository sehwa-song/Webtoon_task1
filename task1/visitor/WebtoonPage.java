package task1.visitor;

import java.io.File;

/** 실제 이미지 파일 하나를 나타내며 자식이 없는 Leaf입니다. */
public final class WebtoonPage implements WebtoonElement {
    private final File imageFile;

    public WebtoonPage(File imageFile) {
        this.imageFile = imageFile;
    }

    public File getImageFile() {
        return imageFile;
    }

    @Override
    public void accept(WebtoonVisitor visitor) {
        visitor.visit(this);
    }
}
