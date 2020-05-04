/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package serialization.capnproto;

import domain.MetadataCreator;
import domain.capnproto.CapSampleFactory;
import domain.capnproto.CapTweetObject;
import org.capnproto.MessageReader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 5, time = 1)
public class CapnProtoSerializeDeserializeLatency {

    static MetadataCreator metadataCreator = new MetadataCreator();

    private static org.capnproto.MessageReader toObject(byte[] data) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        ReadableByteChannel readableByteChannel = Channels.newChannel(byteArrayInputStream);
//        return org.capnproto.SerializePacked.readFromUnbuffered(readableByteChannel);
        return org.capnproto.Serialize.read(readableByteChannel);
    }

    private static byte[] toData(org.capnproto.MessageBuilder message) throws IOException {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        WritableByteChannel wbc = Channels.newChannel(buff);

//        org.capnproto.SerializePacked.writeToUnbuffered(wbc, message);
        org.capnproto.Serialize.write(wbc, message);
        return buff.toByteArray();
    }

    byte[] data;

    @Setup
    public void prepare() throws IOException {
        data = toData(CapSampleFactory.create(metadataCreator));
    }

    @TearDown
    public void teardown() {
    }

    @Benchmark
    public byte[] testToData() throws IOException {
        org.capnproto.MessageBuilder tweetObject = CapSampleFactory.create(metadataCreator);
        return toData(tweetObject);
    }

    @Benchmark
    public org.capnproto.MessageReader testToObject(Blackhole blackhole) throws IOException {
        MessageReader messageReader = toObject(data);
        CapTweetObject.TweetObject.Reader tweetObject = messageReader.getRoot(CapTweetObject.TweetObject.factory);
        blackhole.consume(tweetObject.getCreatedAt());
        blackhole.consume(tweetObject.getId());
        blackhole.consume(tweetObject.getText());
        CapTweetObject.User.Reader user = tweetObject.getUser();
        blackhole.consume(user.getDescription());
        blackhole.consume(user.getId());
        blackhole.consume(user.getName());
        blackhole.consume(user.getScreenName());
        blackhole.consume(user.getUrl());

        CapTweetObject.Location.Reader location = user.getLocation();
        blackhole.consume(location.getCity());
        blackhole.consume(location.getCountry());
        return messageReader;
    }


    public static void main(String[] args) throws IOException {
        CapnProtoSerializeDeserializeLatency test = new CapnProtoSerializeDeserializeLatency();
        test.prepare();
        System.out.println("data length " + test.testToData().length);
        MessageReader messageReader = toObject(test.data);
        CapTweetObject.TweetObject.Reader tweetObject = messageReader.getRoot(CapTweetObject.TweetObject.factory);
        System.out.println(tweetObject.getCreatedAt());
        System.out.println(tweetObject.getId());
        System.out.println(tweetObject.getText());
        CapTweetObject.User.Reader user = tweetObject.getUser();
        System.out.println(user.getDescription());
        System.out.println(user.getId());
        System.out.println(user.getName());
        System.out.println(user.getScreenName());
        System.out.println(user.getUrl());

        CapTweetObject.Location.Reader location = user.getLocation();
        System.out.println(location.getCity());
        System.out.println(location.getCountry());
        test();
    }

    private static void test() {
        Options opt = new OptionsBuilder()
                .include(CapnProtoSerializeDeserializeLatency.class.getSimpleName())
                .forks(1)
                .build();

        try {
            new Runner(opt).run();
        } catch (RunnerException e) {
            e.printStackTrace();
        }
    }
}