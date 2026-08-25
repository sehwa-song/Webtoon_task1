/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package webtoon.benchmark;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import variants.strategy.CutPointStrategy;
import variants.strategy.MarginCutPointStrategy;
import variants.strategy.StrategySplitter;

/**
 * Strategy 패턴의 호출 방식에 따른 시간과 메모리 할당량을 비교한다.
 *
 * <p>각 benchmark의 1 op는 images 폴더의 모든 이미지를 한 번씩
 * 분석하는 작업이다. 이미지 파일 읽기는 측정 전에 한 번만 수행한다.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(value = 1, jvmArgsAppend = {"-Xmx4g"})
@Threads(1)
public class MyBenchmark {

    /** 모든 benchmark가 함께 사용하는 이미지와 전략 객체이다. */
    @State(Scope.Benchmark)
    public static class StrategyState {
        List<BufferedImage> images;
        MarginCutPointStrategy concreteStrategy;
        CutPointStrategy interfaceStrategy;
        StrategySplitter strategyContext;

        /** 시간 측정을 시작하기 전에 전체 이미지를 메모리에 읽는다. */
        @Setup(Level.Trial)
        public void loadImagesAndCreateStrategies() throws IOException {
            File imageDirectory = findImageDirectory();
            List<File> imageFiles = new ArrayList<>();
            collectImageFiles(imageDirectory, imageFiles);
            imageFiles.sort(Comparator.comparing(File::getPath));

            if (imageFiles.isEmpty()) {
                throw new IllegalStateException(
                        "이미지를 찾지 못했습니다: " + imageDirectory.getAbsolutePath());
            }

            ImageIO.setUseCache(false);
            images = new ArrayList<>(imageFiles.size());
            for (File imageFile : imageFiles) {
                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) {
                    throw new IOException(
                            "읽을 수 없는 이미지입니다: " + imageFile.getAbsolutePath());
                }
                images.add(image);
            }

            // 세 benchmark 모두 같은 알고리즘 객체를 사용한다.
            concreteStrategy = new MarginCutPointStrategy();
            interfaceStrategy = concreteStrategy;
            strategyContext = new StrategySplitter(interfaceStrategy);

            System.out.println(
                    "JMH가 불러온 전체 이미지 수: " + images.size());
        }

        /** benchmark가 모두 끝나면 이미지가 잡고 있던 메모리를 해제한다. */
        @TearDown(Level.Trial)
        public void releaseImages() {
            if (images != null) {
                for (BufferedImage image : images) {
                    image.flush();
                }
                images.clear();
            }
        }

        private static File findImageDirectory() throws IOException {
            // pattern-jmh 폴더에서 실행하는 경우
            File parentImages = new File("../images");
            if (parentImages.isDirectory()) {
                return parentImages.getCanonicalFile();
            }

            // 프로젝트 최상위 폴더에서 실행하는 경우
            File currentImages = new File("images");
            if (currentImages.isDirectory()) {
                return currentImages.getCanonicalFile();
            }

            throw new IOException(
                    "images 폴더를 찾지 못했습니다. pattern-jmh 또는 프로젝트 최상위에서 실행하세요.");
        }

        private static void collectImageFiles(File input, List<File> result) {
            File[] children = input.listFiles();
            if (children == null) {
                return;
            }

            for (File child : children) {
                if (child.isDirectory()) {
                    collectImageFiles(child, result);
                } else if (isSupportedImage(child)) {
                    result.add(child);
                }
            }
        }

        private static boolean isSupportedImage(File file) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            return name.endsWith(".jpg")
                    || name.endsWith(".jpeg")
                    || name.endsWith(".png");
        }
    }

    /** 구체 클래스 타입으로 알고리즘을 바로 호출한다. */
    @Benchmark
    public void concreteDirect(StrategyState state, Blackhole blackhole) {
        for (BufferedImage image : state.images) {
            blackhole.consume(state.concreteStrategy.findCutPoints(image));
        }
    }

    /** Strategy 인터페이스 타입을 통해 알고리즘을 호출한다. */
    @Benchmark
    public void strategyInterface(StrategyState state, Blackhole blackhole) {
        for (BufferedImage image : state.images) {
            blackhole.consume(state.interfaceStrategy.findCutPoints(image));
        }
    }

    /** Context인 StrategySplitter를 통해 전략 알고리즘을 호출한다. */
    @Benchmark
    public void strategyContext(StrategyState state, Blackhole blackhole) {
        for (BufferedImage image : state.images) {
            blackhole.consume(state.strategyContext.findCutPoints(image));
        }
    }
}
