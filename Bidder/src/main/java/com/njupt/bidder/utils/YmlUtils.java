package com.njupt.bidder.utils;

import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import java.io.*;

public class YmlUtils {
    public static <T> T toObject(String ymlPath, Class<T> clazz) throws FileNotFoundException {
        File file = ResourceUtils.getFile(ymlPath);
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        Yaml props = new Yaml(new Constructor(clazz));
        return props.load(is);
    }
}
