package com.njupt.bidder.utils;

import com.njupt.bidder.pojo.FirstRoundInput;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.njupt.bidder.pojo.SecondRoundInput;
import com.njupt.bidder.pojo.ThirdRoundInput;

import java.io.*;
import java.util.Base64;

/**
 * 序列化工具，基于kryo实现对每轮输入的序列化，并将字节数组转化为base64编码，传输体积会增大，但更优雅一点；
 * 注意序列化的时候由于没有转成文件，因此output缓冲区一定要足够一次装下整个对象.
 * 第一轮大约有32个密文，即64个群元素，需要8192字节加一个身份字符串的空间；
 * 第二轮根据参加拍卖的人数，每一方都会有32个密文64个群元素，再加上每一方的身份字符串的空间；
 * 第三轮还需要额外的TOKEN的空间，每一方有32个密文32个TOKEN，共96个群元素，再加上身份字符串的空间；
 */
public class SerializeUtils {
    private static final Kryo kryo = new Kryo();

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    static {
        kryo.setRegistrationRequired(false);
    }
    public static String firstRoundInput2Bytes(FirstRoundInput firstRoundInput) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream, 10240)) {
            kryo.writeObject(output, firstRoundInput);
            return ENCODER.encodeToString(output.toBytes());
        }catch (Exception e){
            e.printStackTrace();
            throw new IOException("序列化失败");
        }
    }

    public static FirstRoundInput Bytes2FirstRoundInput(byte[] bytes) throws IOException {
        String base64Str = new String(bytes);
        bytes = DECODER.decode(base64Str);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            Input input = new Input(byteArrayInputStream)) {
            return kryo.readObject(input, FirstRoundInput.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("反序列化失败");
        }
    }

    public static String secondRoundInput2Bytes(SecondRoundInput secondRoundInput, int size) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream, 10240 * size)) {
            kryo.writeObject(output, secondRoundInput);
            return ENCODER.encodeToString(output.toBytes());
        }catch (Exception e){
            e.printStackTrace();
            throw new IOException("序列化失败");
        }
    }

    public static SecondRoundInput Bytes2SecondRoundInput(byte[] bytes) throws IOException {
        String base64Str = new String(bytes);
        bytes = DECODER.decode(base64Str);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            return kryo.readObject(input, SecondRoundInput.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("反序列化失败");
        }
    }

    public static String thirdRoundInput2Bytes(ThirdRoundInput thirdRoundInput, int size) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream, 15360 * size)) {
            kryo.writeObject(output, thirdRoundInput);
            return ENCODER.encodeToString(output.toBytes());
        }catch (Exception e){
            e.printStackTrace();
            throw new IOException("序列化失败");
        }
    }

    public static ThirdRoundInput Bytes2ThirdRoundInput(byte[] bytes) throws IOException {
        String base64Str = new String(bytes);
        bytes = DECODER.decode(base64Str);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            Input input = new Input(byteArrayInputStream)) {
            return kryo.readObject(input, ThirdRoundInput.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("反序列化失败");
        }
    }
}
